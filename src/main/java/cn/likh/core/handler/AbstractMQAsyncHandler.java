package cn.likh.core.handler;

import cn.hutool.extra.spring.SpringUtil;
import cn.likh.core.entity.MQAsyncTask;
import cn.likh.core.enums.MQAsyncTaskExecStatusEnum;
import cn.likh.core.exception.MQAsyncExceptionHandler;
import cn.likh.core.util.MQAsyncUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * 模板方法，简化各子类的代码
 */
@Slf4j
public abstract class AbstractMQAsyncHandler implements MQAsyncHandler {

    @Resource
    public MQAsyncExceptionHandler exceptionHandler;

    public void setMqAsyncApi(MQAsyncExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * 任务初始化
     * @param task 任务相关参数
     */
    public void handlerInit(MQAsyncTask task){

    }

    /**
     * 任务执行
     * @param task 任务相关参数
     */
    public abstract void handlerProcess(MQAsyncTask task);


    @Override
    public void process(MQAsyncTask task) {
        try {
            log.info("handler:{}任务开始执行初始化,参数:{}", task.getBizCode(), task.getParam());
            handlerInit(task);
            log.info("handler:{}任务初始化执行结束,开始进行逻辑处理", task.getBizCode());
            handlerProcess(task);
            log.info("handler:{}任务逻辑处理执行结束,开始进行任务结束", task.getBizCode());
            handlerOver(task);
            log.info("handler:{}任务执行结束", task.getBizCode());
        } catch (Exception e) {
            log.error("handler:{}任务执行异常", task.getBizCode(), e);
            handlerException(task, e);
            log.error("handler:{}任务执行异常,异常处理结束", task.getBizCode());
        } finally {
            log.info("handler:{}任务开始finally", task.getBizCode());
            handlerFinally(task);
            log.info("handler:{}任务finally执行结束", task.getBizCode());
        }
    }

    /**
     * 任务异常处理
     * @param task 任务相关参数
     * @param e 异常信息
     */
    public void handlerException(MQAsyncTask task, Exception e){
        exceptionHandler.handlerException(MQAsyncUtils.getStaticProperties(), task, MQAsyncTaskExecStatusEnum.CUSTOMER_EXCEPTION, MQAsyncUtils.traceToString(e));
    }

    /**
     * 任务结束
     * @param task 任务相关参数
     */
    public void handlerOver(MQAsyncTask task){

    }

    /**
     * 最终执行
     * @param task 任务相关参数
     */
    public void handlerFinally(MQAsyncTask task){

    }

    /**
     * 注册自己到工厂中
     */
    public abstract void registerHandler();


    @Override
    public void afterPropertiesSet() throws Exception {
        registerHandler();
    }
}
