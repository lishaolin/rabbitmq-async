package cn.likh.core.bean;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 声明式组件BeanDefinition的包装类
 *
 * @author shaolin.li
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeclWarpBean {

    /**
     * 业务类型
     */
    private String taskType;

    /**
     * 使用@MQAsyncMethod 所在的类的class对象创建出来的RootBeanDefinition
     */
    private Object rawBean;

    /**
     * 使用@MQAsyncMethod 所在的类的class对象
     */
    private Class<?> rawClazz;

    /**
     * 业务类型对应的且被@MQAsyncMethod标识的方法列表
     */
    private List<MethodWrapBean> methodWrapBeanList;

}
