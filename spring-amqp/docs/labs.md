# RabbitMQ 实验：从启动到看懂消息路由

先阅读[核心概念与架构图](../README.md)。以下命令均从项目根目录执行。

## 1. 启动环境

```bash
docker compose up -d --wait
mvn -pl spring-amqp spring-boot:run
```

在 IDE 中也可以直接运行 `com.zjq.rabbitmq.RabbitLearningApplication`。

打开 [RabbitMQ 管理界面](http://localhost:15673)，使用 `learner` / `learner-local` 登录。启动应用后，会自动声明 `study.*` 交换机、队列与绑定，无需手动创建。

| 配置 | 默认值 | 可覆盖的环境变量 |
| --- | --- | --- |
| RabbitMQ 地址 | `localhost` | `RABBITMQ_HOST` |
| 宿主机 AMQP 端口 | `5673`，映射至容器 `5672` | `RABBITMQ_PORT` |
| 管理界面端口 | `15673`，映射至容器 `15672` | `RABBITMQ_MANAGEMENT_PORT`，只作用于 Compose |
| 用户名 | `learner` | `RABBITMQ_USERNAME` |
| 密码 | `learner-local` | `RABBITMQ_PASSWORD` |
| vhost | `/` | `RABBITMQ_VHOST`，连接外部实例时需提前准备好 |
| 应用 HTTP 端口 | `8080` | `SERVER_PORT` |
| 消费者自动启动 | `true` | `RABBITMQ_LISTENERS_ENABLED` |

更换端口时，确保启动 Compose 和 Java 应用的终端使用相同环境变量。Compose 的 `.env` 不会自动变成 Java 进程的环境变量；可以在两个终端中分别 `export`。默认账号仅用于本机学习。已有数据卷中的账号不会因修改 Compose 环境变量就自动重建。

## 2. 实验一：一个队列，两个消费者分担任务

```bash
for i in $(seq 1 20); do
  curl -sS -X POST http://localhost:8080/api/messages/work \
    -H 'Content-Type: application/json' \
    -d "{\"text\":\"work-$i\"}"
  echo
done
```

观察应用日志里的 `work-fast`、`work-slow`：两者监听同一个 `study.work.queue`，分别模拟 20ms 和 200ms 的处理耗时。

正常情况下，每条消息只由其中一个消费者处理；快消费者通常能处理更多，但不要假定固定比例。`prefetch: 1` 限制每个消费者同时持有的未确认消息数量。

## 3. 实验二：Direct 精确匹配

```bash
curl -i -X POST 'http://localhost:8080/api/messages/direct?routingKey=blue' \
  -H 'Content-Type: application/json' \
  -d '{"text":"hello direct"}'
```

| Routing Key | 预期消费者 |
| --- | --- |
| `blue` | `direct-1` |
| `yellow` | `direct-2` |
| `red`，也是默认值 | `direct-1`、`direct-2` 各收到一份 |
| `unknown` | 无队列匹配，接口返回 `503`，提示检查 Routing Key |

生产者同时检查发布 Confirm 和无法路由的 Return，避免把“收到发布 ACK 但没有任何队列接收”当成实验成功。

## 4. 实验三：Fanout 广播到各个绑定队列

```bash
curl -i -X POST http://localhost:8080/api/messages/fanout \
  -H 'Content-Type: application/json' \
  -d '{"text":"hello fanout"}'
```

预期 `fanout-1`、`fanout-2` 各打印一条。这是两个队列各收到一份，再分别由各自的消费者处理。

## 5. 实验四：Topic 通配符匹配

```bash
curl -i -X POST 'http://localhost:8080/api/messages/topic?routingKey=china.news' \
  -H 'Content-Type: application/json' \
  -d '{"text":"hello topic"}'
```

| Routing Key | `china.#` → `topic-1` | `#.news` → `topic-2` |
| --- | --- | --- |
| `china.news`，也是默认值 | 收到 | 收到 |
| `china.good` | 收到 | 不收到 |
| `usa.news` | 不收到 | 收到 |
| `china` | 收到，`#` 可匹配零个词 | 不收到 |

本教学接口将 Topic 路由键限制为由字母、数字、下划线、连字符组成的词，以点分隔。通配符写在绑定规则中。

## 6. 看懂返回值和日志

请求体为 `{"text":"消息内容"}`，内容不能为空且最多 1000 个字符；非法输入返回 `400`。

成功返回示例：

```json
{
  "messageId": "每次发布生成的 UUID",
  "exchange": "study.direct",
  "routingKey": "red",
  "status": "BROKER_CONFIRMED"
}
```

HTTP `202` 只表示已经取得 RabbitMQ 的发布确认，且未发现无法路由；消费者处理结果看消费日志。日志会带上消费者名称、队列、消息 ID、Routing Key 和内容。

连接错误、发布 NACK、Return 或等待确认失败返回 `503`。确认等待最多 5 秒；连接建立受另行配置的超时约束。超时和连接中断时，消息状态可能不确定，不能直接认定消息丢失；重复发送需要考虑幂等性。本教学项目不会自动重发，也没有实现业务去重。

## 7. 观察积压与死信

要观察队列积压，可停止应用，然后关闭消费者重新启动：

```bash
RABBITMQ_LISTENERS_ENABLED=false mvn -pl spring-amqp spring-boot:run
```

发送消息后，在管理界面查看队列 `Ready` 增长。停止应用、按正常方式启动后，消费者会处理已有消息。已有消费者仍运行时，仅启动另一个禁用消费者的进程无法阻止原消费者消费。

学习队列配置了死信交换机 `study.dead-letter`，死信进入 `study.dead-letter.queue`，该队列没有自动消费者。监听容器使用 Spring `AUTO` 确认模式，正常返回后由容器确认；拒绝时不无限重新入队。

常规监听器只打印日志，不故意制造失败。死信实验由集成测试显式拒绝消息触发，避免初学时误以为“任意消息都会自动进入死信”。

## 8. 测试与打包

```bash
# 无需 RabbitMQ：测试 Confirm、Return 和失败语义。
mvn test

# 生成可执行 JAR。
mvn package
java -jar spring-amqp/target/spring-amqp-1.0-SNAPSHOT.jar
```

真实 RabbitMQ 集成测试使用 `rabbitmq-it` Maven profile，覆盖 HTTP 输入校验、默认交换机、Direct、Fanout、Topic、无法路由和死信处理：

```bash
# 连接专用测试实例；测试前会清空上述 study.* 队列。
RABBITMQ_HOST=localhost RABBITMQ_PORT=5673 \
  mvn -Prabbitmq-it verify
```

运行集成测试前，停止学习应用，避免它的消费者取走测试消息。测试内的消费者自动启动已禁用；连接失败会导致测试失败，不会默默跳过。

## 9. 推荐阅读代码的顺序

1. `config/RabbitTopology.java`：实验中的名字。
2. `config/RabbitTopologyConfig.java`：队列、交换机、绑定和死信声明。
3. `web/MessageController.java`：把 HTTP 请求映射到四种实验。
4. `messaging/MessagePublisher.java`：发送消息、设置 ID、等待发布确认。
5. `consumer/LearningConsumers.java`：监听队列及消费日志。
6. `src/main/resources/application.yml`：连接、确认模式和预取配置。

前五项路径相对于 `src/main/java/com/zjq/rabbitmq/`。先顺着“请求 → 发送 → 路由 → 消费”读，再回头研究确认和失败处理。

## 10. 常见问题

- **连接失败**：确认容器健康、账号正确。Java 默认连接宿主机 `5673`，不要填写管理界面的 `15673`。
- **端口被占用**：通过环境变量修改端口，并让 Compose 与 Java 配置保持一致。
- **没有看到消费日志**：确认消费者已开启、Routing Key 能匹配、HTTP 返回 `202`。学习队列被其他进程监听时，消息也可能由那个进程消费。
- **队列刚出现就为空**：消费者处理很快，查看应用日志；想观察积压可按第 7 节关闭消费者。
- **`PRECONDITION_FAILED`**：同名队列或交换机已存在但属性不一致。先核对已有资源；需要保留消息时，不能直接删除队列。
- **停止实验**：应用 `Ctrl+C`；`docker compose down` 停止本项目服务并保留数据卷。修改 Java 代码不会删除旧 Demo 在其他 RabbitMQ 实例中已创建的资源。

完成四组实验后，再学习有限重试、业务幂等、Outbox、Quorum Queue 和高可用。当前单节点实验环境用于理解行为，不具备生产级高可用能力。
