package cn.likh.core.factory;

import cn.hutool.core.util.StrUtil;
import cn.likh.core.exception.MQAsyncDefinitionException;
import cn.likh.core.handler.MQAsyncHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQAsyncHandler的工厂类
 */
public class MQAsyncHandlerFactory {

    /**
     * handlerName和对应的处理类的关系
     */
    private static final Map<String, MQAsyncHandler> handlerMap = new ConcurrentHashMap<>();

    /**
     * 获取handler
     *
     * @param handlerName handlerName
     * @return 处理类
     */
    public static MQAsyncHandler getHandler(String handlerName) {
        return handlerMap.get(handlerName);
    }

    /**
     * registerHandler
     *
     * @param handlerName handlerName
     * @param handler     处理类
     */
    public static void registerHandler(String handlerName, MQAsyncHandler handler) {
        if (null == handler) {
            return;
        }
        if (handlerMap.containsKey(handlerName)) {
            throw new MQAsyncDefinitionException(StrUtil.format("MQ异步任务类型 {[]} 已被声明,请勿重复", handlerName));
        }
        handlerMap.put(handlerName, handler);
    }

}
