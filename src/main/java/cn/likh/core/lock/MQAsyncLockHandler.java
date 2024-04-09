package cn.likh.core.lock;

public interface MQAsyncLockHandler {

    boolean getLock(String lockKey);

    void lockOnException(String lockKey);

    void releaseLock(String lockKey);
}
