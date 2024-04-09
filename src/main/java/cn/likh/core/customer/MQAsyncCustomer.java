package cn.likh.core.customer;

import cn.hutool.json.JSONUtil;
import cn.likh.core.lock.MQAsyncLockHandler;
import cn.likh.core.entity.MQAsyncTask;
import cn.likh.core.enums.MQAsyncTaskExecStatusEnum;
import cn.likh.core.exception.MQAsyncDefinitionException;
import cn.likh.core.exception.MQAsyncExceptionHandler;
import cn.likh.core.factory.MQAsyncHandlerFactory;
import cn.likh.core.handler.MQAsyncHandler;
import cn.likh.core.util.MQAsyncUtils;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

import java.io.IOException;

/**
 * 异步任务监听
 */
@Slf4j
@RequiredArgsConstructor
public class MQAsyncCustomer implements ChannelAwareMessageListener {

    /**
     * 异常数据保存
     */
    private final MQAsyncExceptionHandler exceptionHandler;

    /**
     * redis
     */
    private final MQAsyncLockHandler lockHandler;

    /**
     * 请求时的唯一ID
     */
    private static final String ID_HEAD = "spring_returned_message_correlation";

    @Override
    public void onMessage(Message message, Channel channel) throws IOException {
        if (null == message) {
            log.error("异步任务从mq中获取信息为空");
            return;
        }

        MQAsyncTask mqAsyncTask = null;
        MQAsyncHandler handler;
        try {
            mqAsyncTask = JSONUtil.toBean(new String(message.getBody()), MQAsyncTask.class);
            // 转换成MQAsyncTask失败，无效信息，直接丢弃
            if (null == mqAsyncTask) {
                log.error("异步任务从mq中获取信息转化为Task参数异常, message:{}, body:{}", message, message.getBody() == null ? "" : new String(message.getBody()));
                throw new MQAsyncDefinitionException("异步任务从mq中获取信息转化为Task参数异常");
            }
            handler = MQAsyncHandlerFactory.getHandler(mqAsyncTask.getBizCode());
            // 未找到对应handler（可能性极低，只要按照对应模式开发，bizName肯定能找到对应的handler，此处兜底处理）
            if (null == handler) {
                exceptionHandler.handlerException(MQAsyncUtils.getStaticProperties(), mqAsyncTask, MQAsyncTaskExecStatusEnum.HANDLER_NOT_FOND, MQAsyncTaskExecStatusEnum.HANDLER_NOT_FOND.getName());
                log.error("异步任务未找到对应的处理类, task：{}", JSONUtil.toJsonStr(mqAsyncTask));
                throw new MQAsyncDefinitionException("异步任务未找到对应的处理类异常");
            }
        } catch (Exception e) {
            // 回复ack，不退回队列重试，若出现异常，由业务方决定是否补偿
            log.error("消息消费异常，未进行业务处理, task:{}", JSONUtil.toJsonStr(mqAsyncTask), e);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }

        String uniqId = message.getMessageProperties().getHeader(ID_HEAD);

        // 分布式锁，防止消息重复消费
        try {
            // 获取到锁，执行业务代码，未获取到，直接ack，该消息已被消费
            if (lockHandler.getLock(uniqId)){
                // 执行业务代码
                handler.process(mqAsyncTask);
            } else {
                log.error("业务重复消费, 未获取到锁, task:{}, uniqId:{}", JSONUtil.toJsonStr(mqAsyncTask), uniqId);
            }
        } catch (Exception e) {
            // 此处异常理应只有redis出现问题时
            log.error("消息消费异常, task:{}", JSONUtil.toJsonStr(mqAsyncTask), e);
            lockHandler.lockOnException(uniqId);
        } finally {
            lockHandler.releaseLock(uniqId);
            // 执行业务代码后，消息确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }

    }
}
