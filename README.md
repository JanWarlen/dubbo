# 服务发布
## 服务发布入口
`org.apache.dubbo.config.ServiceConfig.export`
### invoker调用链构建
`org.apache.dubbo.rpc.protocol.ProtocolFilterWrapper#buildInvokerChain`

### 服务发布实现入口
`org.apache.dubbo.registry.integration.RegistryProtocol#export`
### 服务注册
#### 服务注册入口
`org.apache.dubbo.registry.integration.RegistryProtocol#register`

## 服务接受请求入口
`org.apache.dubbo.remoting.transport.netty.NettyHandler`
或者
`org.apache.dubbo.remoting.transport.netty4.NettyServerHandler`
实际取决于启动的是哪一个版本，默认情况下，启动 netty4
> 内部关键的 handler 是经过装饰模式层层封装的
# 服务消费
## 服务消费启动入口
`org.apache.dubbo.config.ReferenceConfig#get`
启动入口会生成目标接口的代理，通过`org.apache.dubbo.rpc.proxy.InvokerInvocationHandler`进行装饰封装

## 服务消费调用入口
`org.apache.dubbo.rpc.proxy.InvokerInvocationHandler#invoke`
默认（JavassistProxyFactory）情况下的代理入口

## 服务消费最终调用入口
通过启动入口获取代理，正常调用时，会通过代理层层调用，最终转发到具体实现类的invoker中
正常默认情况下，会通过内部封装的netty客户端调用服务提供端
`org.apache.dubbo.rpc.protocol.dubbo.DubboInvoker#doInvoke`

## 服务降级
消费端发起远程调用时，先查看`force:return`是否设置，若设置直接返回mock值，不进行远程调用
否则进行远程调用，如果远程调用正常，则返回远程调用返回结果
如果远程调用失败，则查看`force:return`是否设置，如果设置，则返回mock值，否则本次调用失败
### 服务降级触发入口
`org.apache.dubbo.rpc.cluster.support.wrapper.MockClusterInvoker#invoke`
### mock合法性检查入口
`org.apache.dubbo.config.AbstractInterfaceConfig#checkMock`

## 集群容错
### 集群容错模式
+ FailOver Cluster : 失败重试
  + 调用失败后会自动切换到其他服务提供者重试，如果只有一个，将不会切换
+ FailFast Cluster : 快速失败
  + 调用失败后，立即报错，只会调用1次
+ FailSafe Cluster : 安全失败
  + 调用出现异常时，忽略异常
+ FailBack Cluster : 失败自动恢复
  + 调用服务出现异常后，记录失败的请求，根据一定的策略后期再进行重试
+ Forking  Cluster : 并行调用
  + 并行调用多个服务提供者的服务，只要一个成功则返回
+ BroadCast Cluster : 广播调用
  + 逐个调用所有服务提供者，任意一次调用异常则本次调用失败
+ 自定义容错策略
  + 实现接口 `org.apache.dubbo.rpc.cluster.Cluster`

## 负载均衡
### 负载均衡策略
+ Random LoadBalance : 随机策略
  + 可设置权重，较均匀
+ RoundRobin LoadBalance : 轮询策略
  + 按公约后的权重设置轮询比率
+ LeastActive LoadBalance : 最少活跃调用数
  + 当前正在处理的请求最少的优先选择
+ ConsistenHash LoadBalance : 一致性Hash策略
  + 确保相同参数的请求转发到同一个提供者
+ 自定义负载均衡策略
  + 实现接口`org.apache.dubbo.rpc.cluster.LoadBalance`
  + 继承类`org.apache.dubbo.rpc.cluster.loadbalance.AbstractLoadBalance`

# 线程
## 线程模型
+ all : AllDispatcher : 所有消息均由业务线程池处理
+ direct : DirectDispatcher : 所有消息由IO线程池处理
+ message : MessageOnlyDispatcher : 请求与响应由业务线程池处理
+ execution : ExecutionDispatcher : 请求由业务线程池处理
+ connection : ConnectionOrderedDispatcher : 连接/断开由IO线程池处理
+ 自定义
  + 拓展实现接口`org.apache.dubbo.remoting.Dispatcher`
  + 创建文件/META-INF/dubbo/org.apache.dubbo.remoting.Dispatcher
  + 文件内容 自定义线程模型名=自定义线程模型实现类全路径

## 线程池策略
+ FixedThreadPool : 固定线程个数线程池
+ LimitedThreadPool : 存在最大线程数线程池，空闲不回收
+ EagerThreadPool : 有核心线程和最大线程数，空闲回收
+ CachedThreadPool : 自适应线程池，空闲回收
+ 自定义策略
  + 实现拓展接口`org.apache.dubbo.common.threadpool.ThreadPool`
  + 创建文件/META-INF/dubbo/org.apache.dubbo.common.threadpool.ThreadPool
  + 文件内容 自定义策略名=自定义策略实现类全路径

# 泛化调用
## 泛化调用核心
### 消费端
`org.apache.dubbo.rpc.filter.GenericImplFilter#invoke`
### 服务提供端
`org.apache.dubbo.rpc.filter.GenericFilter#invoke`

# 编码解码
## 编码核心入口
`org.apache.dubbo.remoting.exchange.codec.ExchangeCodec#encode`



# filter 责任链构建
filter责任链的构造分两部分，一部分根据角色和开关配置决定是否加载，一部分根据url中参数`reference.filter`决定

filter生效group: 代表哪些角色可以加载该filter
filter 生效配置关键词: 消费端or服务端的url中包含`同名`或`.同名`配置，则filter将会生效加载
|filter代表词|filter生效group|filter 生效配置关键词|
|--|--|--|
exception|[provider]|
cache|[consumer, provider]|[cache]
genericimpl|[consumer]|[generic]
deprecated|[consumer]|[deprecated]
classloader|[provider]|
echo|[provider]|
monitor|[provider, consumer]|
generic|[provider]|
timeout|[provider]|
accesslog|[provider]|[accesslog]
token|[provider]|[token]
trace|[provider]|
executelimit|[provider]|[executes]
future|[consumer]|
context|[provider]|
activelimit|[consumer]|[actives]
validation|[consumer, provider]|[validation]
consumercontext|[consumer]|

```java
"exception" -> {$Proxy7@3007} "@org.apache.dubbo.common.extension.Activate(after=[], value=[], before=[], group=[provider], order=0)"
"cache" -> {$Proxy7@3058} "@org.apache.dubbo.common.extension.Activate(after=[], value=[cache], before=[], group=[consumer, provider], order=0)"
"genericimpl" -> {$Proxy7@3060} "@org.apache.dubbo.common.extension.Activate(after=[], value=[generic], before=[], group=[consumer], order=20000)"
"deprecated" -> {$Proxy7@3062} "@org.apache.dubbo.common.extension.Activate(after=[], value=[deprecated], before=[], group=[consumer], order=0)"
"classloader" -> {$Proxy7@3064} "@org.apache.dubbo.common.extension.Activate(after=[], value=[], before=[], group=[provider], order=-30000)"
"echo" -> {$Proxy7@3066} "@org.apache.dubbo.common.extension.Activate(after=[], value=[], before=[], group=[provider], order=-110000)"
"monitor" -> {$Proxy7@3068} "@org.apache.dubbo.common.extension.Activate(after=[], value=[], before=[], group=[provider, consumer], order=0)"
"generic" -> {$Proxy7@3070} "@org.apache.dubbo.common.extension.Activate(after=[], value=[], before=[], group=[provider], order=-20000)"
"timeout" -> {$Proxy7@3072} "@org.apache.dubbo.common.extension.Activate(after=[], value=[], before=[], group=[provider], order=0)"
"accesslog" -> {$Proxy7@3074} "@org.apache.dubbo.common.extension.Activate(after=[], value=[accesslog], before=[], group=[provider], order=0)"
"token" -> {$Proxy7@3076} "@org.apache.dubbo.common.extension.Activate(after=[], value=[token], before=[], group=[provider], order=0)"
"trace" -> {$Proxy7@3078} "@org.apache.dubbo.common.extension.Activate(after=[], value=[], before=[], group=[provider], order=0)"
"executelimit" -> {$Proxy7@3080} "@org.apache.dubbo.common.extension.Activate(after=[], value=[executes], before=[], group=[provider], order=0)"
"future" -> {$Proxy7@3082} "@org.apache.dubbo.common.extension.Activate(after=[], value=[], before=[], group=[consumer], order=0)"
"context" -> {$Proxy7@3084} "@org.apache.dubbo.common.extension.Activate(after=[], value=[], before=[], group=[provider], order=-10000)"
"activelimit" -> {$Proxy7@3086} "@org.apache.dubbo.common.extension.Activate(after=[], value=[actives], before=[], group=[consumer], order=0)"
"validation" -> {$Proxy7@3088} "@org.apache.dubbo.common.extension.Activate(after=[], value=[validation], before=[], group=[consumer, provider], order=10000)"
"consumercontext" -> {$Proxy7@3090} "@org.apache.dubbo.common.extension.Activate(after=[], value=[], before=[], group=[consumer], order=-10000)"
```

# Apache Dubbo (incubating) Project

[![Build Status](https://travis-ci.org/apache/incubator-dubbo.svg?branch=master)](https://travis-ci.org/apache/incubator-dubbo)
[![codecov](https://codecov.io/gh/apache/incubator-dubbo/branch/master/graph/badge.svg)](https://codecov.io/gh/apache/incubator-dubbo)
![maven](https://img.shields.io/maven-central/v/org.apache.dubbo/dubbo.svg)
![license](https://img.shields.io/github/license/alibaba/dubbo.svg)
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/apache/incubator-dubbo.svg)](http://isitmaintained.com/project/apache/incubator-dubbo "Average time to resolve an issue")
[![Percentage of issues still open](http://isitmaintained.com/badge/open/apache/incubator-dubbo.svg)](http://isitmaintained.com/project/apache/incubator-dubbo "Percentage of issues still open")
[![Tweet](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?text=Apache%20Dubbo%20(incubating)%20is%20a%20high-performance%2C%20java%20based%2C%20open%20source%20RPC%20framework.&url=http://dubbo.incubator.apache.org/&via=ApacheDubbo&hashtags=rpc,java,dubbo,micro-service)
[![](https://img.shields.io/twitter/follow/ApacheDubbo.svg?label=Follow&style=social&logoWidth=0)](https://twitter.com/intent/follow?screen_name=ApacheDubbo)
[![Gitter](https://badges.gitter.im/alibaba/dubbo.svg)](https://gitter.im/alibaba/dubbo?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

Apache Dubbo (incubating) is a high-performance, Java based open source RPC framework. Please visit [official site](http://dubbo.incubator.apache.org) for quick start and documentations, as well as [Wiki](https://github.com/apache/incubator-dubbo/wiki) for news, FAQ, and release notes.

We are now collecting dubbo user info in order to help us to improve Dubbo better, pls. kindly help us by providing yours on [issue#1012: Wanted: who's using dubbo](https://github.com/apache/incubator-dubbo/issues/1012), thanks :)

## Architecture

![Architecture](http://dubbo.apache.org/img/architecture.png)

## Features

* Transparent interface based RPC
* Intelligent load balancing
* Automatic service registration and discovery
* High extensibility
* Runtime traffic routing
* Visualized service governance

## Getting started

The following code snippet comes from [Dubbo Samples](https://github.com/apache/incubator-dubbo-samples/tree/master/dubbo-samples-api). You may clone the sample project and step into `dubbo-samples-api` sub directory before read on.

```bash
# git clone https://github.com/apache/incubator-dubbo-samples.git
# cd incubator-dubbo-samples/dubbo-samples-api
```

There's a [README](https://github.com/apache/incubator-dubbo-samples/tree/master/dubbo-samples-api/README.md) file under `dubbo-samples-api` directory. Read it and try this sample out by following the instructions.

### Maven dependency

```xml
<properties>
    <dubbo.version>2.7.1</dubbo.version>
</properties>
    
<dependencies>
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo</artifactId>
        <version>${dubbo.version}</version>
    </dependency>
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-dependencies-zookeeper</artifactId>
        <version>${dubbo.version}</version>
    </dependency>
</dependencies>
```

### Define service interfaces

```java
package org.apache.dubbo.samples.api;

public interface GreetingService {
    String sayHello(String name);
}
```

*See [api/GreetingService.java](https://github.com/apache/incubator-dubbo-samples/blob/master/dubbo-samples-api/src/main/java/org/apache/dubbo/samples/api/GreetingsService.java) on GitHub.*

### Implement service interface for the provider

```java
package org.apache.dubbo.samples.provider;
 
import org.apache.dubbo.samples.api.GreetingService;
 
public class GreetingServiceImpl implements GreetingService {
    public String sayHello(String name) {
        return "Hello " + name;
    }
}
```

*See [provider/GreetingServiceImpl.java](https://github.com/apache/incubator-dubbo-samples/blob/master/dubbo-samples-api/src/main/java/org/apache/dubbo/samples/provider/GreetingsServiceImpl.java) on GitHub.*

### Start service provider

```java
package org.apache.dubbo.demo.provider;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.samples.api.GreetingService;

import java.io.IOException;
 
public class Application {

    public static void main(String[] args) throws IOException {
        ServiceConfig<GreetingService> serviceConfig = new ServiceConfig<GreetingService>();
        serviceConfig.setApplication(new ApplicationConfig("first-dubbo-provider"));
        serviceConfig.setRegistry(new RegistryConfig("multicast://224.5.6.7:1234"));
        serviceConfig.setInterface(GreetingService.class);
        serviceConfig.setRef(new GreetingServiceImpl());
        serviceConfig.export();
        System.in.read();
    }
}
```

*See [provider/Application.java](https://github.com/apache/incubator-dubbo-samples/blob/master/dubbo-samples-api/src/main/java/org/apache/dubbo/samples/provider/Application.java) on GitHub.*

### Build and run the provider

```bash
# mvn clean package
# mvn -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.apache.dubbo.demo.provider.Application exec:java
```

### Call remote service in consumer

```java
package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.samples.api.GreetingService;

public class Application {
    public static void main(String[] args) {
        ReferenceConfig<GreetingService> referenceConfig = new ReferenceConfig<GreetingService>();
        referenceConfig.setApplication(new ApplicationConfig("first-dubbo-consumer"));
        referenceConfig.setRegistry(new RegistryConfig("multicast://224.5.6.7:1234"));
        referenceConfig.setInterface(GreetingService.class);
        GreetingService greetingService = referenceConfig.get();
        System.out.println(greetingService.sayHello("world"));
    }
}
```

### Build and run the consumer

```bash
# mvn clean package
# mvn -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.apache.dubbo.demo.consumer.Application exec:java
```

The consumer will print out `Hello world` on the screen.

*See [consumer/Application.java](https://github.com/apache/incubator-dubbo-samples/blob/master/dubbo-samples-api/src/main/java/org/apache/dubbo/samples/consumer/Application.java) on GitHub.*

### Next steps

* [Your first Dubbo application](http://dubbo.apache.org/en-us/blog/dubbo-101.html) - A 101 tutorial to reveal more details, with the same code above.
* [Dubbo user manual](http://dubbo.apache.org/en-us/docs/user/preface/background.html) - How to use Dubbo and all its features.
* [Dubbo developer guide](http://dubbo.apache.org/en-us/docs/dev/build.html) - How to involve in Dubbo development.
* [Dubbo admin manual](http://dubbo.apache.org/en-us/docs/admin/install/provider-demo.html) - How to admin and manage Dubbo services.

## Contact

* Mailing list: 
  * dev list: for dev/user discussion. [subscribe](mailto:dev-subscribe@dubbo.incubator.apache.org), [unsubscribe](mailto:dev-unsubscribe@dubbo.incubator.apache.org), [archive](https://lists.apache.org/list.html?dev@dubbo.apache.org),  [guide](https://github.com/apache/incubator-dubbo/wiki/Mailing-list-subscription-guide)
  
* Bugs: [Issues](https://github.com/apache/incubator-dubbo/issues/new?template=dubbo-issue-report-template.md)
* Gitter: [Gitter channel](https://gitter.im/alibaba/dubbo) 
* Twitter: [@ApacheDubbo](https://twitter.com/ApacheDubbo)

## Contributing

See [CONTRIBUTING](https://github.com/apache/incubator-dubbo/blob/master/CONTRIBUTING.md) for details on submitting patches and the contribution workflow.

### How can I contribute?

* Take a look at issues with tag called [`Good first issue`](https://github.com/apache/incubator-dubbo/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) or [`Help wanted`](https://github.com/apache/incubator-dubbo/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22).
* Join the discussion on mailing list, subscription [guide](https://github.com/apache/incubator-dubbo/wiki/Mailing-list-subscription-guide).
* Answer questions on [issues](https://github.com/apache/incubator-dubbo/issues).
* Fix bugs reported on [issues](https://github.com/apache/incubator-dubbo/issues), and send us pull request.
* Review the existing [pull request](https://github.com/apache/incubator-dubbo/pulls).
* Improve the [website](https://github.com/apache/incubator-dubbo-website), typically we need
  * blog post
  * translation on documentation
  * use cases about how Dubbo is being used in enterprise system.
* Improve the [dubbo-admin/dubbo-monitor](https://github.com/apache/incubator-dubbo-admin).
* Contribute to the projects listed in [ecosystem](https://github.com/dubbo).
* Any form of contribution that is not mentioned above.
* If you would like to contribute, please send an email to dev@dubbo.incubator.apache.org to let us know!

## Reporting bugs

Please follow the [template](https://github.com/apache/incubator-dubbo/issues/new?template=dubbo-issue-report-template.md) for reporting any issues.

## Reporting a security vulnerability

Please report security vulnerability to [us](mailto:security@dubbo.incubator.apache.org) privately.

## Dubbo ecosystem

* [Dubbo Ecosystem Entry](https://github.com/dubbo) - A GitHub group `dubbo` to gather all Dubbo relevant projects not appropriate in [apache](https://github.com/apache) group yet
* [Dubbo Website](https://github.com/apache/incubator-dubbo-website) - Apache Dubbo (incubating) official website
* [Dubbo Samples](https://github.com/apache/incubator-dubbo-samples) - samples for Apache Dubbo (incubating)
* [Dubbo Spring Boot](https://github.com/apache/incubator-dubbo-spring-boot-project) - Spring Boot Project for Dubbo
* [Dubbo Admin](https://github.com/apache/incubator-dubbo-admin) - The reference implementation for Dubbo admin

#### Language

* [Node.js](https://github.com/dubbo/dubbo2.js)
* [Python](https://github.com/dubbo/dubbo-client-py)
* [PHP](https://github.com/dubbo/dubbo-php-framework)
* [Go](https://github.com/dubbo/dubbo-go)

## License

Apache Dubbo is under the Apache 2.0 license. See the [LICENSE](https://github.com/apache/incubator-dubbo/blob/master/LICENSE) file for details.
