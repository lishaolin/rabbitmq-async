package cn.likh.core.bean;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 针对注解@MQAsyncMethod的初步解析包装类
 *
 * @author shaolin.li
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeclInfo {

    /**
     * 业务类型
     */
    private String taskType;

    /**
     * 使用@MQAsyncMethod 所在的类的class对象
     */
    private Class<?> rawClazz;

    /**
     * 被@MQAsyncMethod标识的方法
     */
    private MethodWrapBean methodWrapBean;

}
