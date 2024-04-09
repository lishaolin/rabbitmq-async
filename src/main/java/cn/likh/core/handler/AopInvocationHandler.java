package cn.likh.core.handler;

import cn.hutool.core.exceptions.InvocationTargetRuntimeException;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.likh.core.bean.DeclWarpBean;
import cn.likh.core.bean.MethodWrapBean;
import cn.likh.core.entity.MQAsyncTask;
import cn.likh.core.exception.MQAsyncDefinitionException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 方法动态代理
 * @author shaolin.li
 */
public class AopInvocationHandler implements InvocationHandler {
    private final DeclWarpBean declWarpBean;

    public AopInvocationHandler(DeclWarpBean declWarpBean) {
        this.declWarpBean = declWarpBean;
    }

    /**
     * 能进这个方法的前提为该方法被@MQAsyncMethod标记
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 获取当前调用方法，是否在被代理的对象方法里面(根据@MQAsyncMethod这个标注去判断)
        MethodWrapBean currentMethodWrapBean = declWarpBean.getMethodWrapBeanList().stream()
                .filter(methodWrapBean -> methodWrapBean.getMqAsyncMethod().method().getCode().equals(method.getName()))
                .findFirst()
                .orElse(null);

        // 如果被代理的对象里有此标注标的方法，则调用此被代理的对象里的方法，如果没有，则调用父类里的方法
        // 进行检查，检查被代理的bean里是否第一个参数为MQAsyncTask这个类型的
        boolean checkFlag = currentMethodWrapBean.getMethod().getParameterTypes().length > 0
                && currentMethodWrapBean.getMethod().getParameterTypes()[0].equals(MQAsyncTask.class);
        if (!checkFlag) {
            String errMsg = StrUtil.format(
                    "方法[{}.{}] 必须拥有 MQAsyncTask 这个参数(第一个参数得为 MQAsyncTask)",
                    declWarpBean.getRawClazz().getName(), currentMethodWrapBean.getMethod().getName());
            throw new MQAsyncDefinitionException(errMsg);
        }

        try {
            if (args != null && args.length > 0){
                return ReflectUtil.invoke(declWarpBean.getRawBean(), currentMethodWrapBean.getMethod(), args);
            }else{
                return ReflectUtil.invoke(declWarpBean.getRawBean(), currentMethodWrapBean.getMethod(), proxy);
            }
        }catch (InvocationTargetRuntimeException e) {
            InvocationTargetException targetEx = (InvocationTargetException) e.getCause();
            throw targetEx.getTargetException();
        }
    }
}
