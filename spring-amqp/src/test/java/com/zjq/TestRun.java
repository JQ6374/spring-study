package com.zjq;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class TestRun {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testQueue() throws InterruptedException {
        // 队列名称
        String queueName = "test.queue";
        // 消息
        String message = "hello, message_";
        for (int i = 0; i < 50; i++) {
            // 发送消息，每20毫秒发送一次，相当于每秒发送50条消息
            rabbitTemplate.convertAndSend(queueName, "", message + i);
            Thread.sleep(20);
        }
    }

    @Test
    public void testSendMap() throws InterruptedException {
        // 准备消息
        Map<String,Object> msg = new HashMap<>();
        msg.put("name", "柳岩");
        msg.put("age", 21);
        // 发送消息
        rabbitTemplate.convertAndSend("object.queue", msg);
    }

    @Test
    public void testFanoutExchange() {
        // 交换机名称
        String exchangeName = "test.fanout";
        // 消息
        String message = "hello, everyone!";
        rabbitTemplate.convertAndSend(exchangeName, "", message);
    }

    @Test
    public void testDirectExchange() throws InterruptedException {
        // 交换机名称
        String exchangeName = "test.direct";
        // 消息
        String message = "hello, red!";
        rabbitTemplate.convertAndSend(exchangeName, "blue", message);
    }

    @Test
    public void testTopicExchange() throws InterruptedException {
        // 交换机名称
        String exchangeName = "test.topic";
        // 消息
        String message = "hello, China!";
        rabbitTemplate.convertAndSend(exchangeName, "china.good", message);
    }
}
