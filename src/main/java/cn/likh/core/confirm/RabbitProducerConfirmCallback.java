package cn.likh.core.confirm;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.likh.core.constants.Constants;
import cn.likh.core.entity.MQAsyncDTO;
import cn.likh.core.entity.MQAsyncTask;
import cn.likh.core.enums.MQAsyncTaskExecStatusEnum;
import cn.likh.core.exception.MQAsyncExceptionHandler;
import cn.likh.core.util.MQAsyncUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * 异步消息确认，确认发送到交换机,队列
 */
@Slf4j
@RequiredArgsConstructor
public class RabbitProducerConfirmCallback implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {

    private MQAsyncExceptionHandler exceptionHandler;

    public RabbitProducerConfirmCallback(RabbitTemplate rabbitTemplate, MQAsyncExceptionHandler handler) {
        exceptionHandler = handler;
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnsCallback(this);
    }

    /**
     * 消息发送至交换机时回调
     *
     * @param correlationData 异常时消息
     * @param ack             是否发送成功
     * @param cause           异常原因
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String exchange = Constants.EMPTY_STRING;

        MQAsyncTask mqAsyncTask = new MQAsyncTask();

        if (null != correlationData && null != correlationData.getReturned()) {
            exchange = correlationData.getReturned().getExchange();
            if (null != correlationData.getReturned().getMessage()) {
                MQAsyncTask mqAsyncTaskJSON = JSONUtil.toBean(new String(correlationData.getReturned().getMessage().getBody()), MQAsyncTask.class);
                if (null != mqAsyncTaskJSON) {
                    mqAsyncTask = mqAsyncTaskJSON;
                }
            }
        }
        if (ack) {
            log.info("异步消息发送至RabbitMQ交换机成功,交换机：{}, 业务编号:{}, 业务参数:{}", exchange, mqAsyncTask.getBizCode(), mqAsyncTask.getParam());
            return;
        }

        log.error("发送异步消息至交换机时出现异常, cause:{}", cause);
        try {
            exceptionHandler.handlerException(MQAsyncUtils.getStaticProperties(), mqAsyncTask, MQAsyncTaskExecStatusEnum.SEND_2_EXCHANGE_ERROR, cause);
        } catch (Exception e) {
            log.error("发送异步消息至交换机时出现异常, 异常处理失败, cause:{}", cause, e);
        }

    }


    /**
     * 消息从交换机至队列时异常
     *
     * @param returnedMessage 消息
     */
    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        log.error("消息发送至队列失败, message:{}, replyCode:{}, replyTest:{}, exchange:{}, routingKey:{}", returnedMessage.getMessage(), returnedMessage.getReplyCode(), returnedMessage.getReplyText(), returnedMessage.getExchange(), returnedMessage.getRoutingKey());
        MQAsyncTask mqAsyncTask = JSONUtil.toBean(new String(returnedMessage.getMessage().getBody()), MQAsyncTask.class);
        if (null == mqAsyncTask) {
            mqAsyncTask = new MQAsyncTask();
        }
        String cause = StrUtil.format("异常码:{},异常信息:{},交换机:{},routingKey:{}", returnedMessage.getReplyCode(), returnedMessage.getReplyText(), returnedMessage.getExchange(), returnedMessage.getRoutingKey());
        log.error("异步消息从交换机到队列时出现异常, 异常信息:{}", cause);
        try {
            exceptionHandler.handlerException(MQAsyncUtils.getStaticProperties(), mqAsyncTask, MQAsyncTaskExecStatusEnum.SEND_2_QUERY_ERROR, cause);
        } catch (Exception e) {
            log.error("异步消息从交换机到队列时出现异常, 异常处理失败, 异常信息:{}", cause, e);
        }
    }
}
