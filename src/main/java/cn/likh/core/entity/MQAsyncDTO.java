package cn.likh.core.entity;

import cn.likh.config.OperationsMQAsyncProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MQAsyncDTO {

    /**
     * 系统配置
     */
    private OperationsMQAsyncProperties properties;

    /**
     * 执行参数
     */
    private MQAsyncTask mqAsyncTask;

}
