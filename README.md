



# MQ 异步框架的使用与介绍

# 1、整体流程

<img src="static\流程图.png">

<img src="static\架构图.png">



# 2、使用示例

## 2.1: 快速入门

### 2.1.1: 服务内引用服务
请注意，本项目并未上传到中央仓库，使用时请克隆项目后自行安装到本地或上传到企业私服
```xml
<dependency>
    <groupId>cn.likh</groupId>
    <artifactId>rabbitmq-async</artifactId>
    <version>1.0.0</version>
</dependency>
```



### 2.1.2: 声明两个Bean,一个为出现异常时的处理，一个为执行锁

出现异常时的处理，此处例举一种示例
handlerException()会在一下下几种情况下触发

1、消息发送异常

2、发送消息至交换机异常

3、发送消息至队列异常

4、消息消费异常

5、未找到对应的业务处理类
```
import cn.hutool.json.JSONUtil;
import cn.likh.config.OperationsMQAsyncProperties;
import cn.likh.core.entity.MQAsyncTask;
import cn.likh.core.enums.MQAsyncTaskExecStatusEnum;
import cn.likh.core.exception.MQAsyncExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultMQAsyncExceptionHandler implements MQAsyncExceptionHandler {

    @Override
    public void handlerException(OperationsMQAsyncProperties properties, MQAsyncTask mqAsyncTask, MQAsyncTaskExecStatusEnum execStatus, String cause) {
      log.error("MQ异步任务发生异常，系统配置信息:{}, 方法执行参数:{}, 异常阶段:{}, 异常原因:{}", JSONUtil.toJsonStr(properties), JSONUtil.toJsonStr(mqAsyncTask), execStatus.getName(), cause);
    }
}

```



执行锁，此处例举一种示例，防止消息被重复消费（理论上不会）此处提供一种默认方式，但只能在单机情况下保证不会重复消费，集群环境请使用分布式锁

```

import cn.hutool.core.thread.ThreadUtil;
import cn.likh.core.lock.MQAsyncLockHandler;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class DefaultMQAsyncLockHandler implements MQAsyncLockHandler {

    private static final ConcurrentHashMap<String, String> lockMap = new ConcurrentHashMap<>();

    @Override
    public boolean getLock(String lockKey) {
        long waitTimeout = 5000L;
        long nanoTime = System.nanoTime(); // 当前时间
        do {
            Object o = lockMap.putIfAbsent(lockKey, lockKey);
            if (null == o) {
                return true;
            }
            ThreadUtil.sleep(500L);//休眠500毫秒
        } while ((System.nanoTime() - nanoTime) < TimeUnit.MILLISECONDS.toNanos(waitTimeout));
        return false;
    }

    @Override
    public void lockOnException(String lockKey) {

    }

    @Override
    public void releaseLock(String lockKey) {
        lockMap.remove(lockKey);
    }
}

```



### 2.1.3: 在需要异步的地方调用方法

```java
MQAsyncUtils.async(MQAsyncTask.builder()
        .bizName("test")
        .bizCode(bizCode)
        .param(param)
        .build());
```



### 2.1.4: 继承 AbstractMQAsyncHandler,编写自己的业务方法

```java
@Service
@Slf4j
public class TestAsyncHandler extends AbstractMQAsyncHandler {

    /**
     * 实际业务处理方法
     * @param task 发起异步时的参数
     */
    @Override
    public void handlerProcess(MQAsyncTask task) {
        // 进行自己的业务处理
    }

    /**
     * 将自己注册到异步工厂中，以保证消息找到对应的处理类
     */
    @Override
    public void registerHandler() {
        MQAsyncHandlerFactory.registerHandler("test", this);
    }

}
```

## 2.2: 详细介绍

### 2.2.1: MQAsyncUtils._async()方法详解_

该方法为异步触发点，该方法负责将异步数据推送至 MQ

该方法只会在两种情况下抛出异常，请注意

_1、参数为__null_

_2、参数对象中的 handlerName 为空_



### 2.2.2: 模板方法 AbstractMQAsyncHandler 详解

AbstractMQAsyncHandler 是一个抽象类，其实现了一个接口 MQAsyncHandler，异步消息的消费者监听到消息后，调用 MQAsyncHandler 的 process(MQAsyncTask task)方法进行逻辑处理

```java
// 从工厂类中拿到bizName对应的handler
MQAsyncHandler handler = MQAsyncHandlerFactory.getHandler(mqAsyncTask.getBizName());
// 各handler执行自己的逻辑
handler.process(mqAsyncTask);
```

<img src="static\抽象类.png">

其中，AbstractMQAsyncHandler 中定义了两个抽象方法必须子类重写

1、handlerProcess() 该抽象方法子类重写，在其中编写自己的业务逻辑

2、registerHandler() 该抽象方法子类重写，调用 MQAsyncHandlerFactory._registerHandler()_向 map 中注册当前 handler

在方法执行的生命周期中，AbstractMQAsyncHandler 只重写了 handlerException()方法，在发生异常时将数据保存到数据库，这要求各子实现类在重写 handlerProcess()时需将异常抛出，否则 handlerException()方法无法执行

注：

1、方法执行的生命周期子类皆可重写

2、子类自行考量是否继承 AbstractMQAsyncHandler，也可直接实现 MQAsyncHandler



### 2.2.3: 可选配置项

配置路径前缀：spring.rabbitmq.async

| 配置路径          | 备注             | 默认值                           | 是否必须 | 示例用法                                                                                                         |
| ----------------- | ---------------- | -------------------------------- | -------- | ---------------------------------------------------------------------------------------------------------------- |
| addresses         | MQ 地址          | 无                               | 是       | 无                                                                                                               |
| userName          | MQ 用户名        | 无                               | 是       | 无                                                                                                               |
| password          | MQ 密码          | 无                               | 是       | 无                                                                                                               |
| virtualHost       | virtualHost      | 无                               | 是       | 无                                                                                                               |
| queryName         | 队列名称         | 应用名称 + 环境 +_async_query    | 否       | 本地调试时，消息容易被其他本地起了服务的同事监听到，此时可配置队列名称区分（记得删除队列），其余情况不建议自定义 |
| exchangeName      | 交换机名称       | 应用名称 + 环境 +_async_exchange | 否       | 本地调试时，消息容易被其他本地起了服务的同事监听到，此时可配置交换机名称区分，其余情况不建议自定义               |
| queryDurable      | 队列是否持久化   | true                             | 否       | 无                                                                                                               |
| queryExclusive    | 队列是否排他     | false                            | 否       | 无                                                                                                               |
| queryAutoDelete   | 队列是否自动删除 | false                            | 否       | 无                                                                                                               |
| exchangeDurable   | 交换机是否持久化 | true                             | 否       | 无                                                                                                               |
| exchangeExclusive | 交换机是否排他   | false                            | 否       | 无                                                                                                               |
| routingKey        | 路由 key         | ROUTING_KEY_MQ_ASYNC+ 环境       | 否       | 无                                                                                                               |
| concurrency       | 并发线程数       | 10                               | 否       | 无                                                                                                               |



## 2.3: 使用声明式使用异步框架

在快速入门中，使用需编写一个类继承 AbstractMQAsyncHandler 并重写相关方法,现在介绍一种更简洁的方式使用本异步框架

### 2.3.1: 在需要异步的地方调用方法（与 2.1.3 一致）

```java
MQAsyncUtils._async_(MQAsyncTask._builder_()
        .bizName("test")
        .bizCode(bizCode)
        .param(param)
        .build());
```



### 2.3.2: 在某个组件中使用注解方式定义自己的业务处理逻辑

```java
@MQAsyncMethod(method = MQAsyncMethodEnum.HANDLER_PROCESS, taskType = "test")
public void bizMethod(MQAsyncTask task) {
     // 编写自己的业务逻辑
}
```



### 2.3.3：相关测试用例

| 测试用例                                                     | 预期结果                     | 结果 |
| ------------------------------------------------------------ | ---------------------------- | ---- |
| 同一个taskType在多个类中声明                                 | 项目启动报错                 | √    |
| 同一个taskType在一个类中多次声明，方法不同                   | 不报错，且顺利执行每一个方法 | √    |
| 同一个taskType在一个类中多次声明，方法相同                   | 项目启动报错                 | √    |
| 同一个taskType在一个类中单次声明，无主方法                   | 项目启动报错                 | √    |
| 同一个taskType在一个类中单次声明，有主方法                   | 不报错，且顺利执行主方法     | √    |
| 多个taskType在一个类中多次声明                               | 不报错，且顺利执行每一个方法 | √    |
| 在有接口的类中声明，且接口中无方法，只在实现类中声明         | 不报错，且顺利执行           | √    |
| 在有接口的类中声明，接口中有方法，实现类中重写               | 不报错，且顺利执行           | √    |
| 在无接口的类中声明                                           | 不报错，且顺利执行           | √    |
| 方法的修饰符为provider                                       | 执行时报空指针异常           | √    |
| 方法的修饰符为public                                         | 不报错，且顺利执行           | √    |
| 方法中调用api                                                | 不报错，且顺利执行           | √    |
| 方法中调用入库方法                                           | 不报错，且顺利执行           | √    |
| 方法中调用MQ中间件                                           | 不报错，且顺利执行           | √    |
| 在service中声明，不影响service中其他方法的执行               | 不影响                       | √    |
| 在有继承关系的类中声明                                       | 项目启动报错                 | √    |
| 在业务service中声明，且有@RefreshScope之类的注解、且不加@Primary | 项目启动报错                 | √    |
| 在业务service中声明，加@Primary                              | 不报错，且顺利执行           | √    |


