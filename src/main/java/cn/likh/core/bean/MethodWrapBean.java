package cn.likh.core.bean;


import cn.likh.core.annotation.MQAsyncMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;

/**
 * 对注解@MQAsyncMethod标注的方法的包装类
 *
 * @author shaolin.li
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MethodWrapBean {

    /**
     * 方法本身
     */
    private Method method;

    /**
     * 注解
     */
    private MQAsyncMethod mqAsyncMethod;

}
