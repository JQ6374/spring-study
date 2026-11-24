package com.zjq.rabbitmq;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static com.zjq.rabbitmq.config.RabbitTopology.*;
import static org.assertj.core.api.Assertions.assertThat;

/** 仅通过 rabbitmq-it profile 运行。应连接独立的测试 RabbitMQ 或 vhost。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class RabbitRoutingIT {
    @Autowired
    private RabbitTemplate template;
    @Autowired
    private ConnectionFactory connectionFactory;
    @Autowired
    private Environment environment;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    @BeforeEach
    void clearLearningQueues() {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.initialize();
        for (String queue : List.of(WORK_QUEUE, DIRECT_QUEUE_1, DIRECT_QUEUE_2,
                FANOUT_QUEUE_1, FANOUT_QUEUE_2, TOPIC_QUEUE_1, TOPIC_QUEUE_2, DEAD_LETTER_QUEUE)) {
            admin.purgeQueue(queue, false);
        }
    }

    @Test
    void workUsesTheDefaultExchange() throws Exception {
        assertAccepted(post("work", "{\"text\":\"hello\"}"));
        assertMessage(WORK_QUEUE, true);
    }

    @ParameterizedTest
    @CsvSource({"blue,true,false", "red,true,true", "yellow,false,true"})
    void directRoutesToExactlyTheMatchingQueues(String key, boolean first, boolean second) throws Exception {
        assertAccepted(post("direct?routingKey=" + key, "{\"text\":\"hello\"}"));
        assertMessage(DIRECT_QUEUE_1, first);
        assertMessage(DIRECT_QUEUE_2, second);
    }

    @Test
    void fanoutDeliversACopyToEachQueue() throws Exception {
        assertAccepted(post("fanout", "{\"text\":\"hello\"}"));
        assertMessage(FANOUT_QUEUE_1, true);
        assertMessage(FANOUT_QUEUE_2, true);
    }

    @ParameterizedTest
    @CsvSource({"china.news,true,true", "china.good,true,false", "usa.news,false,true", "china,true,false"})
    void topicMatchesBothWildcardPatterns(String key, boolean first, boolean second) throws Exception {
        assertAccepted(post("topic?routingKey=" + key, "{\"text\":\"hello\"}"));
        assertMessage(TOPIC_QUEUE_1, first);
        assertMessage(TOPIC_QUEUE_2, second);
    }

    @Test
    void unmatchedRoutingKeyIsNotReportedAsAccepted() throws Exception {
        HttpResponse<String> response = post("direct?routingKey=unknown", "{\"text\":\"hello\"}");
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).contains("未匹配任何队列");
        assertMessage(DIRECT_QUEUE_1, false);
        assertMessage(DIRECT_QUEUE_2, false);
    }

    @Test
    void rejectsInvalidInputBeforePublishing() throws Exception {
        assertThat(post("work", "{\"text\":\" \"}").statusCode()).isEqualTo(400);
        assertThat(post("work", "{\"text\":\"" + "x".repeat(1001) + "\"}").statusCode()).isEqualTo(400);
        assertThat(post("work", "not-json").statusCode()).isEqualTo(400);
        assertThat(post("topic?routingKey=china..news", "{\"text\":\"hello\"}").statusCode()).isEqualTo(400);
        assertThat(post("direct?routingKey=bad!key", "{\"text\":\"hello\"}").statusCode()).isEqualTo(400);
        assertMessage(WORK_QUEUE, false);
    }

    @Test
    void rejectedDeliveryMovesToDeadLetterQueue() throws Exception {
        assertAccepted(post("work", "{\"text\":\"hello\"}"));
        try (Channel channel = connectionFactory.createConnection().createChannel(false)) {
            GetResponse delivery = channel.basicGet(WORK_QUEUE, false);
            assertThat(delivery).isNotNull();
            channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
        }
        assertMessage(DEAD_LETTER_QUEUE, true);
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        String port = environment.getRequiredProperty("local.server.port");
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/messages/" + path))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void assertAccepted(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.body()).contains("BROKER_CONFIRMED");
    }

    private void assertMessage(String queue, boolean expected) {
        Message message = template.receive(queue, expected ? 3000 : 100);
        if (expected) {
            assertThat(message).isNotNull();
            assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).isEqualTo("hello");
            assertThat(message.getMessageProperties().getMessageId()).isNotBlank();
        } else {
            assertThat(message).isNull();
        }
    }
}
