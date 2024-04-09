package cn.likh.config;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.likh.core.bean.DeclWarpBean;
import cn.likh.core.factory.MQAsyncHandlerFactory;
import cn.likh.core.handler.AbstractMQAsyncHandler;
import cn.likh.core.handler.AopInvocationHandler;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.annotation.Annotation;

/**
 * 创建代理类，在方法执行时执行代理方法
 */
public class DeclBeanProxy implements BeanPostProcessor {

    /**
     *  注解 @RefreshScope bean 的前缀
     */
    private static final String REFRESH_SCOPE_ANN_BEAN_PREFIX = "scopedTarget.";

    /**
     * 注解 @RefreshScope 完整类路径
     */
    private static final String REFRESH_SCOPE_ANN_CLASS_PATH = "org.springframework.cloud.context.config.annotation.RefreshScope";

    @SuppressWarnings("rawtypes")
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class clazz = getUserClass(bean.getClass());

        //声明式组件
        if (bean instanceof DeclWarpBean){
            // 创建代理类
            AbstractMQAsyncHandler nodeComponent = proxy2handler((DeclWarpBean) bean);
            // 注册到工厂
            MQAsyncHandlerFactory.registerHandler(getRealBeanName(clazz, beanName), nodeComponent);
            return nodeComponent;
        }

        return bean;

    }

    private String getRealBeanName(Class<?> clazz, String beanName) {
        if (beanName.startsWith(REFRESH_SCOPE_ANN_BEAN_PREFIX)) {
            Annotation[] annotations = AnnotationUtil.getAnnotations(clazz, true);
            for (Annotation annotation : annotations) {
                String name = annotation.annotationType().getName();
                if (REFRESH_SCOPE_ANN_CLASS_PATH.equals(name)) {
                    return beanName.replace(REFRESH_SCOPE_ANN_BEAN_PREFIX, "");
                }
            }
        }
        return beanName;
    }

    private AbstractMQAsyncHandler proxy2handler(DeclWarpBean declWarpBean) {
        // 获取当前节点的原有注解，如：LiteFlowRetry 之类的规则注解
        Annotation[] beanClassAnnotation = declWarpBean.getRawClazz().getAnnotations();

        try {
            // 创建对象
            // 这里package进行了重设，放到了被代理对象的所在目录
            // 生成的对象也加了上被代理对象拥有的注解
            // 被拦截的对象也根据被代理对象根据@LiteFlowMethod所标注的进行了动态判断
            Object instance = new ByteBuddy().subclass(AbstractMQAsyncHandler.class)
                    .name(StrUtil.format("{}$ByteBuddy${}${}", declWarpBean.getRawClazz().getName(), declWarpBean.getTaskType(), IdUtil.fastSimpleUUID()))
                    .implement(declWarpBean.getRawClazz().getInterfaces())
                    .method(ElementMatchers.namedOneOf(declWarpBean.getMethodWrapBeanList().stream().map(wrap -> wrap.getMqAsyncMethod().method().getCode()).toArray(String[]::new)))
                    .intercept(InvocationHandlerAdapter.of(new AopInvocationHandler(declWarpBean)))
                    .annotateType(beanClassAnnotation)
                    .make()
                    .load(DeclBeanProxy.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded()
                    .newInstance();
            return (AbstractMQAsyncHandler) instance;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> getUserClass(Class<?> clazz) {
        if (clazz.getName().contains("$$")) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return superclass;
            }
        }
        return clazz;
    }

}
