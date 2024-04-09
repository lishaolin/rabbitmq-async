package cn.likh.core.lock;

public interface MQAsyncLockHandler {

    /**
     * 获取锁
     * @param lockKey 唯一标识
     * @return 是否获取到锁
     */
    boolean getLock(String lockKey);

    /**
     * 当获取锁出现异常时处理
     * @param lockKey 唯一标识
     */
    void lockOnException(String lockKey);

    /**
     * 释放锁
     * @param lockKey 唯一标识
     */
    void releaseLock(String lockKey);
}
