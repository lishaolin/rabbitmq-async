package cn.likh.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("spring.rabbitmq.async")
@Validated
public class OperationsMQAsyncProperties {

    /**
     * 应用名称
     */
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * 环境
     */
    @Value("${spring.profiles.active}")
    private String active;

    /**
     * 地址
     */
    private String addresses;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    /**
     * virtualHost
     */
    private String virtualHost;

    /**
     * 队列名称
     */
    private String queryName;

    /**
     * 交换机名称
     */
    private String exchangeName;

    /**
     * 队列是否持久化
     */
    private boolean queryDurable = true;

    /**
     * 队列是否排他
     */
    private boolean queryExclusive = false;

    /**
     * 队列是否自动删除
     */
    private boolean queryAutoDelete = false;

    /**
     * 交换机是否持久化
     */
    private boolean exchangeDurable = true;

    /**
     * 交换机是否排他
     */
    private boolean exchangeExclusive = false;

    /**
     * 路由key
     */
    private String routingKey = "ROUTING_KEY_MQ_ASYNC";

    /**
     * 并发线程数
     */
    private String concurrency = "10";

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public String getAddresses() {
        return addresses;
    }

    public void setAddresses(String addresses) {
        this.addresses = addresses;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public String getQueryName() {
        return queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public boolean isQueryDurable() {
        return queryDurable;
    }

    public void setQueryDurable(boolean queryDurable) {
        this.queryDurable = queryDurable;
    }

    public boolean isQueryExclusive() {
        return queryExclusive;
    }

    public void setQueryExclusive(boolean queryExclusive) {
        this.queryExclusive = queryExclusive;
    }

    public boolean isQueryAutoDelete() {
        return queryAutoDelete;
    }

    public void setQueryAutoDelete(boolean queryAutoDelete) {
        this.queryAutoDelete = queryAutoDelete;
    }

    public boolean isExchangeDurable() {
        return exchangeDurable;
    }

    public void setExchangeDurable(boolean exchangeDurable) {
        this.exchangeDurable = exchangeDurable;
    }

    public boolean isExchangeExclusive() {
        return exchangeExclusive;
    }

    public void setExchangeExclusive(boolean exchangeExclusive) {
        this.exchangeExclusive = exchangeExclusive;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(String concurrency) {
        this.concurrency = concurrency;
    }
}
