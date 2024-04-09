package cn.likh.core.annotation;


import cn.likh.core.enums.MQAsyncMethodEnum;

import java.lang.annotation.*;

/**
 * 声明式MQ异步方法
 * 该注解使用在方法上，使用前提为方法所在的类需为被spring管理的类
 * 通过{@link MQAsyncMethodEnum} 声明自己的方法类型，不同的方法类型在不同时机执行，详见枚举类中的注释
 * eg：
 *    @MQAsyncMethod(method = MQAsyncMethodEnum.HANDLER_PROCESS, taskType = MQAsyncTaskEnum.WORK_FLOW_START_MSG_SEND)
 *     public void sendStartMsg(MQAsyncTask task) {
 *          // do something
 *     }
 * 最终，会根据taskType创建对象，一个taskType对应多个method
 * 本注解支持在同一个类中声明多个taskType，创建时会以taskType分组创建对象
 * 但是不支持同一个taskType在多个类中声明
 * 本注解不恰当使用会导致服务启动失败！！请注意以下三点
 * 1、在已有继承关系的spring组件中声明，项目启动会报错。eg:AService extends BHandler,此时在AService中声明会报错。 若AService implements BHandler,此时在AService中声明不会报错
 * 2、在会被其余业务引用的spring组件中声明,需在声明的类中加上@Primary注解。eg: 在AService中的某个方法上使用了@MQAsyncMethod注解，且AService被AController引用，则需在AService类上添加@Primary注解
 * 3、同一个taskType在多个spring组件中声明
 * @author shaolin.li
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MQAsyncMethod {

    /**
     * 方法类型
     * @return value
     */
    MQAsyncMethodEnum method();

	/**
	 * 业务类型
	 * @return bizCode
	 */
    String taskType();

}
