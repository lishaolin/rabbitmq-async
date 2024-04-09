package cn.likh.core.enums;


import lombok.Getter;

/**
 * 声明式MQ异步方法，不同方法有不同的执行时机
 * handlerInit在业务方法执行前执行
 * handlerProcess为业务具体的执行逻辑
 * handlerOver在业务方法执行后执行
 * handlerException在业务出现异常时执行，前提为【handlerInit、handlerProcess、handlerOver】这三个方法将异常抛出，若这三个方法自己处理了异常，则handlerException不会执行
 * handlerFinally为最终执行
 * 以上所有方法参数都为： {@link cn.likh.core.entity.MQAsyncTask}  注：handlerException多一个参数 {@link Exception}
 * 现通过以下代码简易表示方法执行顺序
 * MQAsyncTask task;
 * try {
 * handlerInit(task);
 * handlerProcess(task);
 * handlerOver(task);
 * } catch (Exception e) {
 * handlerException(task, e);
 * } finally {
 * handlerFinally(task);
 * }
 * 以上，handlerProcess为必须方法，若未声明，则项目启动时会报异常，handlerException存在默认方法，会将异常信息存入SYSTEM_MQ_ASYNC表中，其余方法若未声明，则不会执行
 * 方法的修饰符需为public
 *
 * @author shaolin.li
 */
@Getter
public enum MQAsyncMethodEnum {

    HANDLER_INIT("handlerInit", "任务初始化执行方法", false),

    HANDLER_PROCESS("handlerProcess", "任务执行", true),

    HANDLER_EXCEPTION("handlerException", "任务异常处理", false),

    HANDLER_OVER("handlerOver", "任务结束", false),

    HANDLER_FINALLY("handlerFinally", "最终执行", false),

    ;


    /**
     * code
     */
    private final String code;

    /**
     * name
     */
    private final String name;

    /**
     * 是否为必须方法
     */
    private final boolean isMainMethod;

    MQAsyncMethodEnum(String code, String name, boolean isMainMethod) {
        this.code = code;
        this.name = name;
        this.isMainMethod = isMainMethod;
    }

    public boolean isMainMethod() {
        return isMainMethod;
    }
}
