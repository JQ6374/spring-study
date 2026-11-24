# Spring Boot 2、3、4：主要区别

学习时最值得关注的是 Java 基线、底层 Spring Framework、Jakarta 包名，以及第三方依赖的兼容性。Controller、Service、依赖注入、自动配置这些日常开发概念一直延续。

## 一张表看清楚

| 对比项 | Spring Boot 2（以 2.7 为例） | Spring Boot 3 | Spring Boot 4 |
| --- | --- | --- | --- |
| 最低 Java 版本 | Java 8 | Java 17 | Java 17 |
| 底层 Spring Framework | 5.x | 6.x | 7.x |
| 企业规范相关包名 | 主要为 `javax.*` | 改为 `jakarta.*` | 延续 `jakarta.*`，更新规范基线 |
| 默认 JSON 技术栈 | Jackson 2 | Jackson 2 | Jackson 3 |
| Servlet 容器代表 | Tomcat 9 | Tomcat 10.1 | Tomcat 11 |
| 值得关注的变化 | 大量历史项目使用的开发方式 | Jakarta 迁移、Java 17、AOT / 原生镜像支持 | 更细的依赖模块与 Starter、Jackson 3、Jakarta EE 11 |

表格概括主要版本路线，具体依赖版本以所选补丁版本的依赖管理为准。Boot 2.7 的 Java 8 / Framework 5.3 基线见[官方文档](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/getting-started.html#getting-started.system-requirements)；Boot 3 与 4 的变化见[3.0 迁移指南](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)、[4.0 迁移指南](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)。

## 1. Boot 2 → 3：Java 和 Jakarta 是最大的迁移门槛

首先，运行和编译的最低要求从 Java 8 升到 Java 17。旧项目里的 Maven 编译配置、构建镜像及部署机器也要同步调整。

其次，企业规范 API 从 `javax` 迁到 `jakarta`。例如参数校验：

```java
// Boot 2 中常见的写法。
import javax.validation.constraints.NotBlank;

// Boot 3 / 4 中的写法。
import jakarta.validation.constraints.NotBlank;
```

Servlet、JPA 等相关 API 和第三方库也要配套升级。**不是所有 `javax` 都要替换**，Java SE 自带的 `javax.sql`、`javax.crypto` 等仍保留原包名。

另外，Boot 3 提供 AOT 和 GraalVM 原生镜像支持，并更新可观测性相关集成。初学者仍然可以用常规 JVM 运行应用，无需一开始就学习原生镜像。[官方 Boot 3 迁移指南](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)

## 2. Boot 3 → 4：关注依赖、模块和默认实现

Boot 4 基于 Spring Framework 7、Jakarta EE 11 / Servlet 6.1，最低 Java 要求仍为 17。**Boot 4 并不强制你使用 Java 21。**

依赖模块和 Starter 更细分，例如 MVC 项目优先使用 `spring-boot-starter-webmvc`，相关测试能力也有对应的技术 Starter；部分自动配置类的包路径发生变化。迁移时不能只改父 POM 版本。

默认 JSON 技术栈升级到 Jackson 3。如果你手写了 `ObjectMapper` 配置，需要留意相关类型从 `com.fasterxml.jackson.databind` 迁移到 `tools.jackson.databind`，而 Jackson 注解仍可处于 `com.fasterxml.jackson.annotation`。本项目的 HTTP JSON 交给 Boot 自动配置，MQ 消息先用字符串，减少第一轮学习负担。

第三方库必须与 Framework 7、Jakarta 新规范配套。例如 ORM、接口文档、安全框架及自己编写的自动配置扩展，都需要逐项确认兼容版本。[官方 Boot 4 迁移指南](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)

## 3. 哪些知识仍然通用？

你熟悉的这些代码仍然是学习基础：

```java
@SpringBootApplication
@RestController
@Service
@Configuration
@Bean
```

构造器注入、`application.yml`、Maven Starter、用 `main` 启动、打包后 `java -jar` 运行，整体开发方式延续。

RabbitMQ 的核心概念不取决于 Boot 大版本。学习生产者、交换机、绑定、队列、消费者，以及 `RabbitTemplate`、`@RabbitListener`，仍然能迁移到不同版本的项目中。具体 API 要参考该项目实际使用的 Spring AMQP 版本。

## 4. 这个学习项目为什么用 4.1.3？

原项目是 Boot 2.7.12，现已按新项目重建，基础版本选择 **Boot 4.1.3 + Java 17**：既采用新的主版本，也使用已有补丁维护的稳定分支。4.1.3 不是 Snapshot，也不表示它是所有分支中最新的版本。[所选版本系统要求](https://docs.spring.io/spring-boot/4.1/system-requirements.html)

Java 17 保持与你本机环境一致；后续也可以单独升级到兼容的较新 LTS JDK。Spring、Rabbit 客户端、Jackson 和日志实现由 Boot 的依赖管理统一配套，避免混用旧版本。

对于已有业务系统，升级应先评估依赖，再按官方建议从最新 2.7 迁到 3、从最新 3.5 迁到 4。这个仓库是学习 Demo，可以直接重建，不需要保留旧框架的兼容层。
