package cn.likh.core.util;


import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.likh.config.OperationsMQAsyncProperties;
import cn.likh.core.confirm.RabbitProducerConfirmCallback;
import cn.likh.core.constants.Constants;
import cn.likh.core.entity.MQAsyncDTO;
import cn.likh.core.entity.MQAsyncTask;
import cn.likh.core.entity.RabbitMQCollectionDTO;
import cn.likh.core.enums.MQAsyncTaskExecStatusEnum;
import cn.likh.core.exception.MQAsyncExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import javax.annotation.PostConstruct;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * 异步任务发起工具类
 */
@Slf4j
@RequiredArgsConstructor
public class MQAsyncUtils {

    /**
     * mq
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 异常数据处理
     */
    private final MQAsyncExceptionHandler asyncExceptionHandler;

    /**
     * 相关配置信息
     */
    private final OperationsMQAsyncProperties properties;

    /**
     * 异常回调
     */
    private final RabbitProducerConfirmCallback confirmCallback;

    /**
     * mq
     */
    private static RabbitTemplate staticRabbitTemplate;

    /**
     * 异常数据落库
     */
    private static MQAsyncExceptionHandler staticAsyncExceptionHandler;

    /**
     * 相关配置信息
     */
    private static OperationsMQAsyncProperties staticProperties;

    /**
     * 异常回调
     */
    private static RabbitProducerConfirmCallback staticConfirmCallback;

    @PostConstruct
    public void init() {
        staticRabbitTemplate = rabbitTemplate;
        staticProperties = properties;
        staticAsyncExceptionHandler = asyncExceptionHandler;
        staticConfirmCallback = confirmCallback;
    }


    /**
     * 创建异步任务
     *
     * @param mqAsyncTask 该对象有两个参数，handlerName不可为空
     */
    public static void async(MQAsyncTask mqAsyncTask) {
        send(mqAsyncTask, staticRabbitTemplate, staticProperties.getExchangeName(), staticProperties.getRoutingKey());
    }

    /**
     * 重新发送消息，可自定义队列
     *
     * @param collectionDTO 相关连接信息
     * @param mqAsyncTask   异步参数
     */
    public static void resendCustomer(RabbitMQCollectionDTO collectionDTO, MQAsyncTask mqAsyncTask) {
        send(mqAsyncTask,
                getRabbitTemplate(collectionDTO),
                StrUtil.isBlank(collectionDTO.getExchangeName()) ? staticProperties.getExchangeName() : collectionDTO.getExchangeName(),
                StrUtil.isBlank(collectionDTO.getRoutingKey()) ? staticProperties.getRoutingKey() : collectionDTO.getRoutingKey());

    }

    /**
     * 发送异步消息至MQ
     *
     * @param mqAsyncTask  异步参数
     * @param template     mq
     * @param exchangeName 交换机名称
     * @param routingKey   路由key
     */
    private static void send(MQAsyncTask mqAsyncTask, RabbitTemplate template, String exchangeName, String routingKey) {
        if (null == mqAsyncTask) {
            throw new IllegalArgumentException("异步任务参数不可为null");
        }
        if (StrUtil.isBlank(mqAsyncTask.getBizCode())) {
            throw new IllegalArgumentException("handlerName不可为空");
        }
        try {
            CorrelationData correlationData = new CorrelationData(IdUtil.fastUUID());
            correlationData.setReturned(new ReturnedMessage(new Message(JSONUtil.toJsonStr(mqAsyncTask).getBytes(StandardCharsets.UTF_8)), Constants.FAIL, Constants.EMPTY_STRING, exchangeName, routingKey));
            template.convertAndSend(exchangeName, routingKey, JSONUtil.toJsonStr(mqAsyncTask), correlationData);
        } catch (Exception e) {
            log.error("发送异步消息至MQ时出现异常", e);
            try {
                staticAsyncExceptionHandler.handlerException(MQAsyncUtils.getStaticProperties(), mqAsyncTask, MQAsyncTaskExecStatusEnum.SEND_ERROR, traceToString(e));
            } catch (Exception exception) {
                log.error("发送异步消息至MQ时出现异常, 异常处理失败", e);
            }

        }
    }

    /**
     * 防止异常信息超长，截取3000长度
     *
     * @param t 异常信息
     * @return 异常信息
     */
    public static String traceToString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw, true));
        String result = sw.getBuffer().toString();
        if (result.length() > 3000) {
            result = result.substring(0, 3000) + ".....";
        }
        return result;
    }

    /**
     * 获取配置信息
     */
    public static OperationsMQAsyncProperties getStaticProperties() {
        return staticProperties;
    }

    /**
     * 获取MQ连接
     *
     * @param collectionDTO 链接相关参数
     * @return 链接
     */
    private static RabbitTemplate getRabbitTemplate(RabbitMQCollectionDTO collectionDTO) {
        if (null == collectionDTO) {
            collectionDTO = new RabbitMQCollectionDTO();
        }
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setAddresses(StrUtil.isBlank(collectionDTO.getAddresses()) ? staticProperties.getAddresses() : collectionDTO.getAddresses());
        connectionFactory.setUsername(StrUtil.isBlank(collectionDTO.getUserName()) ? staticProperties.getUserName() : collectionDTO.getUserName());
        connectionFactory.setPassword(StrUtil.isBlank(collectionDTO.getPassword()) ? staticProperties.getPassword() : collectionDTO.getPassword());
        connectionFactory.setVirtualHost(StrUtil.isBlank(collectionDTO.getVirtualHost()) ? staticProperties.getVirtualHost() : collectionDTO.getVirtualHost());
        // 生产者消费确认机制，确认交付成功之消息确认发送至交换机，回调为异步回调
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        // 生产者消费确认机制，确认交付成功之交换机到队列，回调为异步回调
        connectionFactory.setPublisherReturns(true);

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnsCallback(staticConfirmCallback);
        rabbitTemplate.setConfirmCallback(staticConfirmCallback);
        return rabbitTemplate;
    }

}
