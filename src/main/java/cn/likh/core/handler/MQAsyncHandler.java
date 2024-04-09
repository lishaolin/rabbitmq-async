package cn.likh.core.handler;

import cn.likh.core.entity.MQAsyncTask;
import org.springframework.beans.factory.InitializingBean;

/**
 * 异步任务处理的基类
 */
public interface MQAsyncHandler extends InitializingBean {

    /**
     * 任务执行
     *
     * @param task 任务相关参数
     */
    void process(MQAsyncTask task);


}
