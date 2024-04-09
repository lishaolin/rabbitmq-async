package cn.likh.core.entity;

import lombok.*;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ToString
public class RabbitMQCollectionDTO {

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
     * 路由键
     */
    private String routingKey;

    /**
     * 交换机名称
     */
    private String exchangeName;
}
