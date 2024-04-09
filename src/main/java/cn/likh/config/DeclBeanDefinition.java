package cn.likh.config;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.likh.core.annotation.MQAsyncMethod;
import cn.likh.core.bean.DeclInfo;
import cn.likh.core.bean.DeclWarpBean;
import cn.likh.core.bean.MethodWrapBean;
import cn.likh.core.exception.MQAsyncDefinitionException;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 声明式类的元信息注册器
 * 目的是把声明式的类(尤其是方法级声明的类)拆分出来成为一个或多个定义
 * @author shaolin.li
 */
public class DeclBeanDefinition implements BeanDefinitionRegistryPostProcessor {

    /**
     * 执行时机是在所有bean定义信息将要被加载，但是bean实例还未创建的时候
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) registry;

        String[] beanDefinitionNames = defaultListableBeanFactory.getBeanDefinitionNames();

        // filter出类中方法上有@MQAsyncMethod注解的beanName
        Arrays.stream(beanDefinitionNames).filter(beanName -> {
            BeanDefinition beanDefinition = defaultListableBeanFactory.getMergedBeanDefinition(beanName);
            // 获取到当前类的class对象，通过当前类的class对象，获取该类中的所有的方法
            Class<?> rawClass = getRawClassFromBeanDefinition(beanDefinition);
            // 若当前类的class对象为null，不处理
            if (rawClass == null){
                return false;
            }else{
                // 获取该类中的所有的方法，判断出该类中是否有@MQAsyncMethod注解的方法，若有，则进行下一步处理
                return Arrays.stream(rawClass.getMethods()).anyMatch(method -> AnnotationUtil.getAnnotation(method, MQAsyncMethod.class) != null);
            }
        }).forEach(beanName -> { // 类中方法上有@MQAsyncMethod注解的beanName
            BeanDefinition beanDefinition = defaultListableBeanFactory.getMergedBeanDefinition(beanName);
            Class<?> rawClass = getRawClassFromBeanDefinition(beanDefinition);

            // 解析以下class对象,解析结果为一个taskType对应多个method,此处list为多个taskType
            List<DeclWarpBean> declWarpBeanList = parseDeclBean(rawClass); // 上面筛选过，故这里不可能为null

            // 注册到spring的beanDefinitionMap中去
            declWarpBeanList.forEach(declWarpBean -> {
                GenericBeanDefinition newBeanDefinition = new GenericBeanDefinition();
                newBeanDefinition.setBeanClass(DeclWarpBean.class);
                newBeanDefinition.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
                MutablePropertyValues mutablePropertyValues = new MutablePropertyValues();
                mutablePropertyValues.add("taskType", declWarpBean.getTaskType());
                mutablePropertyValues.add("rawClazz", declWarpBean.getRawClazz());
                mutablePropertyValues.add("methodWrapBeanList", declWarpBean.getMethodWrapBeanList());
                mutablePropertyValues.add("rawBean", beanDefinition);
                newBeanDefinition.setPropertyValues(mutablePropertyValues);
                defaultListableBeanFactory.setAllowBeanDefinitionOverriding(true);
                defaultListableBeanFactory.registerBeanDefinition(declWarpBean.getTaskType(), newBeanDefinition);
            });
        });
    }


    private List<DeclWarpBean> parseDeclBean(Class<?> clazz){
        // 筛选出类中带有@MQAsyncMethod注解的方法，并根据taskType分组，组成对应关系，一个类中,一个taskType对应多个method
        Map<String, List<DeclInfo>> definitionMap = Arrays.stream(clazz.getMethods()).filter(
                method -> AnnotationUtil.getAnnotation(method, MQAsyncMethod.class) != null
        ).map(method -> {
            MQAsyncMethod mqAsyncMethod = AnnotationUtil.getAnnotation(method, MQAsyncMethod.class);

            return new DeclInfo(mqAsyncMethod.taskType(), method.getDeclaringClass(), new MethodWrapBean(method, mqAsyncMethod));
        }).collect(Collectors.groupingBy(DeclInfo::getTaskType));

        // 校验并组装数据
        return definitionMap.entrySet().stream().map(entry -> {
            String taskType = entry.getKey();
            List<DeclInfo> declInfos = entry.getValue();

            Map<String, List<DeclInfo>> collect = declInfos.stream().collect(Collectors.groupingBy(s -> s.getMethodWrapBean().getMqAsyncMethod().method().getCode()));

            if (collect.values().stream().anyMatch(s -> s.size() > 1)){
                throw new MQAsyncDefinitionException(StrUtil.format("MQ异步任务类型 [{}] 中有重复声明的方法，请检查", taskType));
            }

            DeclWarpBean declWarpBean = new DeclWarpBean();
            declWarpBean.setTaskType(taskType);

            DeclInfo processMethodDeclInfo = declInfos.stream().filter(declInfo -> declInfo.getMethodWrapBean().getMqAsyncMethod().method().isMainMethod()).findFirst().orElse(null);
            if (processMethodDeclInfo == null){
                throw new MQAsyncDefinitionException(StrUtil.format("MQ异步任务类型 [{}] 中没有主方法，请检查", taskType));
            }

            declWarpBean.setRawClazz(processMethodDeclInfo.getRawClazz());

            RootBeanDefinition rawClassDefinition = new RootBeanDefinition(clazz);
            rawClassDefinition.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);

            declWarpBean.setRawBean(rawClassDefinition);
            declWarpBean.setMethodWrapBeanList(declInfos.stream().map(DeclInfo::getMethodWrapBean).collect(Collectors.toList()));
            return declWarpBean;
        }).collect(Collectors.toList());
    }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    private Class<?> getRawClassFromBeanDefinition(BeanDefinition beanDefinition){
        try{
            Method method = ReflectUtil.getMethodByName(DeclBeanDefinition.class, "getResolvableType");
            if (method != null){
                Object resolvableType = ReflectUtil.invoke(beanDefinition, method);
                return ReflectUtil.invoke(resolvableType, "getRawClass");
            }else{
                return ReflectUtil.invoke(beanDefinition, "getTargetType");
            }
        }catch (Exception e){
            return null;
        }
    }
}
