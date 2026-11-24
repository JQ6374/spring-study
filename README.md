# Spring Study

基于 **Spring Boot 4.1.3 + Java 17 + Maven** 的中间件学习项目，从 RabbitMQ 消息流转开始，逐步理解路由、确认和失败处理。

Spring Boot 版本由父 POM 统一管理，模块不单独覆盖 Spring、Jackson 等基础依赖。4.1.3 是当前选择的稳定维护版本，不使用 Snapshot。[版本要求](https://docs.spring.io/spring-boot/4.1/system-requirements.html)

## 先学什么

1. [Spring Boot 2、3、4 的主要区别](docs/spring-boot-versions.md)
2. [RabbitMQ 入门教程与架构图](spring-amqp/README.md)
3. [运行项目并完成四组实验](spring-amqp/docs/labs.md)

## 快速启动

准备 Java 17 或更新的兼容 JDK、Maven 3.6.3+、Docker Compose。

在项目根目录执行：

```bash
docker compose up -d --wait
mvn -pl spring-amqp spring-boot:run
```

本项目默认使用独立端口，避开本机已有 RabbitMQ：

| 地址 | 用途 |
| --- | --- |
| `localhost:5673` | Java 应用连接 RabbitMQ；容器内部仍是标准 AMQP 端口 5672 |
| [localhost:15673](http://localhost:15673) | RabbitMQ 管理界面；容器内部为 15672 |
| `localhost:8080` | 学习项目 HTTP 接口 |

本地学习账号为 `learner`，密码为 `learner-local`。配置支持环境变量，详见实验文档。端口只绑定本机。

另开终端，发送第一条消息：

```bash
curl -i -X POST http://localhost:8080/api/messages/work \
  -H 'Content-Type: application/json' \
  -d '{"text":"我的第一条 RabbitMQ 消息"}'
```

收到 `202` 和 `BROKER_CONFIRMED` 表示 RabbitMQ 已确认发布；回到应用终端观察 `work-fast` 或 `work-slow` 的消费日志。

## 构建与验证

```bash
# 单元测试，无需启动 RabbitMQ。
mvn test

# 生成可执行 JAR。
mvn package

# 独立测试 RabbitMQ / vhost 上执行真实路由与 HTTP 集成测试。
# 会清空 study.* 学习队列，请使用专门的测试实例。
mvn -Prabbitmq-it verify
```

集成测试默认关闭监听器，便于直接检查队列里的消息；正常运行时监听器自动开启。[完整实验与测试说明](spring-amqp/docs/labs.md)

停止应用使用 `Ctrl+C`；停止本项目 RabbitMQ 使用 `docker compose down`，命名卷会保留学习数据。
