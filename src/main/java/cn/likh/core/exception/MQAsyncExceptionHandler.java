package cn.likh.core.exception;

import cn.likh.config.OperationsMQAsyncProperties;
import cn.likh.core.entity.MQAsyncDTO;
import cn.likh.core.entity.MQAsyncTask;
import cn.likh.core.enums.MQAsyncTaskExecStatusEnum;

/**
 * 异常处理
 */
public interface MQAsyncExceptionHandler {

    /**
     * 异常处理
     * @param properties 异常发生时相关系统配置
     * @param mqAsyncTask 方法执行时的参数
     * @param execStatus 执行状态
     * @param cause 异常原因
     */
    void handlerException(OperationsMQAsyncProperties properties, MQAsyncTask mqAsyncTask, MQAsyncTaskExecStatusEnum execStatus, String cause);
}
