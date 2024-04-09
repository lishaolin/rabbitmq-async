package cn.likh.core.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum MQAsyncTaskExecStatusEnum {
    SEND_ERROR(0, "消息发送异常"),
    SEND_2_EXCHANGE_ERROR(1, "发送消息至交换机异常"),
    SEND_2_QUERY_ERROR(2, "发送消息至队列异常"),
    CUSTOMER_EXCEPTION(3, "消息消费异常"),
    HANDLER_NOT_FOND(4, "未找到对应的业务处理类");

    private final Integer code;

    private final String name;

    MQAsyncTaskExecStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private final static Map<Integer, String> code2name = new HashMap<>();
    private final static Map<String, Integer> name2code = new HashMap<>();

    static {
        for (MQAsyncTaskExecStatusEnum value : MQAsyncTaskExecStatusEnum.values()) {
            code2name.put(value.getCode(), value.getName());
            name2code.put(value.getName(), value.getCode());
        }
    }

    public static String getNameByCode(Integer code) {
        return code2name.get(code);
    }

    public static Integer getCodeByName(String name) {
        return name2code.get(name);
    }


}
