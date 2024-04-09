package cn.likh.config;

import cn.hutool.core.util.StrUtil;
import cn.likh.core.confirm.RabbitProducerConfirmCallback;
import cn.likh.core.constants.Constants;
import cn.likh.core.customer.MQAsyncCustomer;
import cn.likh.core.exception.MQAsyncExceptionHandler;
import cn.likh.core.lock.MQAsyncLockHandler;
import cn.likh.core.util.MQAsyncUtils;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 消息队列配置类
 *
 */
@AutoConfiguration
@EnableConfigurationProperties(OperationsMQAsyncProperties.class)
public class OperationsMQAsyncAutoConfiguration {

    /**
     * 交换机后缀
     */
    private static final String EXCHANGE_SUFFIX = "_async_exchange";

    /**
     * 队列后缀
     */
    private static final String QUERY_SUFFIX = "_async_query";

    /**
     * 监听者ID SUFFIX
     */
    private static final String ID_SUFFIX = "_ENDPOINT";

    @Bean(name = "operationMqAsyncConnectionFactory")
    public ConnectionFactory operationMqConnectionFactory(OperationsMQAsyncProperties mqProperties) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setAddresses(mqProperties.getAddresses());
        connectionFactory.setUsername(mqProperties.getUserName());
        connectionFactory.setPassword(mqProperties.getPassword());
        connectionFactory.setVirtualHost(mqProperties.getVirtualHost());
        // 生产者消费确认机制，确认交付成功之消息确认发送至交换机，回调为异步回调
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        // 生产者消费确认机制，确认交付成功之交换机到队列，回调为异步回调
        connectionFactory.setPublisherReturns(true);
        return connectionFactory;
    }

    @Bean(name = "operationMqAsyncContainerFactory")
    public SimpleRabbitListenerContainerFactory operationMqContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            @Qualifier("operationMqAsyncConnectionFactory") ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        return factory;
    }

    @Bean(name = "operationAsyncRabbitTemplate")
    public RabbitTemplate operationRabbitTemplate(
            @Qualifier("operationMqAsyncConnectionFactory") ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }

    @Bean("asyncQuery")
    public Queue asyncQuery(OperationsMQAsyncProperties mqProperties) {
        if (StrUtil.isBlank(mqProperties.getQueryName())) {
            mqProperties.setQueryName(mqProperties.getApplicationName());
        }
        // 应用加环境区分，各环境独立
        mqProperties.setQueryName(mqProperties.getQueryName() + Constants.UNDER_LINE + mqProperties.getActive() + QUERY_SUFFIX);
        return new Queue(mqProperties.getQueryName(), // Queue 名字
                mqProperties.isQueryDurable(), // durable: 是否持久化
                mqProperties.isQueryExclusive(), // exclusive: 是否排它
                mqProperties.isQueryAutoDelete()); // autoDelete: 是否自动删除
    }

    @Bean("asyncExchange")
    public DirectExchange asyncExchange(OperationsMQAsyncProperties mqProperties) {
        if (StrUtil.isBlank(mqProperties.getExchangeName())) {
            mqProperties.setExchangeName(mqProperties.getApplicationName());
        }
        // 应用加环境区分，各环境独立
        mqProperties.setExchangeName(mqProperties.getExchangeName() + Constants.UNDER_LINE + mqProperties.getActive() + EXCHANGE_SUFFIX);
        return new DirectExchange(mqProperties.getExchangeName(),
                mqProperties.isExchangeDurable(),  // durable: 是否持久化
                mqProperties.isExchangeExclusive());  // exclusive: 是否排它
    }

    // 创建 Binding
    @Bean("asyncBinding")
    public Binding asyncBinding(OperationsMQAsyncProperties mqProperties,
                                @Qualifier("asyncQuery")Queue queue,
                                @Qualifier("asyncExchange")DirectExchange exchange) {
        mqProperties.setRoutingKey(mqProperties.getRoutingKey() + Constants.UNDER_LINE + mqProperties.getActive());
        return BindingBuilder.bind(queue).to(exchange).with(mqProperties.getRoutingKey());
    }

    @Bean("asyncRabbitProducerConfirmCallback")
    RabbitProducerConfirmCallback rabbitProducerConfirmCallback(@Qualifier("operationAsyncRabbitTemplate") RabbitTemplate operationAsyncRabbitTemplate, MQAsyncExceptionHandler exceptionHandler) {
        return new RabbitProducerConfirmCallback(operationAsyncRabbitTemplate, exceptionHandler);
    }

    @Bean
    public RabbitListenerConfigurer mqAsyncCustomer(OperationsMQAsyncProperties operationsMQAsyncProperties,
                                                    MQAsyncExceptionHandler exceptionHandler,
                                                    MQAsyncLockHandler lockHandler) {
        return rabbitListenerEndpointRegistrar -> {
            SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
            endpoint.setQueueNames(operationsMQAsyncProperties.getQueryName());
            endpoint.setConcurrency(operationsMQAsyncProperties.getConcurrency());
            endpoint.setId(operationsMQAsyncProperties.getQueryName() + ID_SUFFIX);
            endpoint.setMessageListener(new MQAsyncCustomer(exceptionHandler, lockHandler));
            endpoint.setAckMode(AcknowledgeMode.MANUAL);
            rabbitListenerEndpointRegistrar.registerEndpoint(endpoint);
        };
    }

    @Bean
    public MQAsyncUtils asyncUtils(@Qualifier("operationAsyncRabbitTemplate") RabbitTemplate operationAsyncRabbitTemplate,
                                   MQAsyncExceptionHandler exceptionHandler,
                                   OperationsMQAsyncProperties operationsMQAsyncProperties,
                                   @Qualifier("asyncRabbitProducerConfirmCallback") RabbitProducerConfirmCallback confirmCallback) {
        return new MQAsyncUtils(operationAsyncRabbitTemplate, exceptionHandler, operationsMQAsyncProperties, confirmCallback);
    }

    @Bean
    public DeclBeanDefinition declBeanDefinition() {
        return new DeclBeanDefinition();
    }

    @Bean
    public DeclBeanProxy declBeanRe() {
        return new DeclBeanProxy();
    }



}
