package cn.likh.core.entity;

import lombok.*;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ToString
public class MQAsyncTask {

    /**
     * 方法执行的参数 对象
     * 非必传
     */
    private Object param;

    /**
     * 处理的Bean名称, 也是业务标识
     * 该参数必传
     */
    private String bizCode;

    /**
     * bizNo，由调用方决定是否传递
     * 非必传
     */
    private String bizNo;

    /**
     * 是否需要重试，默认为不需要
     */
    private Integer retry = 0;

}
