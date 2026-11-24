package com.zjq.rabbitmq.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.zjq.rabbitmq.config.RabbitTopology.*;

@Component
public class LearningConsumers {
    private static final Logger log = LoggerFactory.getLogger(LearningConsumers.class);

    // 同一队列的两个消费者分担任务，prefetch=1 避免慢消费者囤积消息。
    @RabbitListener(queues = WORK_QUEUE)
    public void workFast(String text, Message message) throws InterruptedException {
        Thread.sleep(20);
        received("work-fast", WORK_QUEUE, text, message);
    }

    @RabbitListener(queues = WORK_QUEUE)
    public void workSlow(String text, Message message) throws InterruptedException {
        Thread.sleep(200);
        received("work-slow", WORK_QUEUE, text, message);
    }

    @RabbitListener(queues = DIRECT_QUEUE_1)
    public void directFirst(String text, Message message) {
        received("direct-1", DIRECT_QUEUE_1, text, message);
    }

    @RabbitListener(queues = DIRECT_QUEUE_2)
    public void directSecond(String text, Message message) {
        received("direct-2", DIRECT_QUEUE_2, text, message);
    }

    @RabbitListener(queues = FANOUT_QUEUE_1)
    public void fanoutFirst(String text, Message message) {
        received("fanout-1", FANOUT_QUEUE_1, text, message);
    }

    @RabbitListener(queues = FANOUT_QUEUE_2)
    public void fanoutSecond(String text, Message message) {
        received("fanout-2", FANOUT_QUEUE_2, text, message);
    }

    @RabbitListener(queues = TOPIC_QUEUE_1)
    public void topicFirst(String text, Message message) {
        received("topic-1", TOPIC_QUEUE_1, text, message);
    }

    @RabbitListener(queues = TOPIC_QUEUE_2)
    public void topicSecond(String text, Message message) {
        received("topic-2", TOPIC_QUEUE_2, text, message);
    }

    private void received(String consumer, String queue, String text, Message message) {
        log.info("consumer={} queue={} messageId={} routingKey={} text={}",
                consumer, queue, message.getMessageProperties().getMessageId(),
                message.getMessageProperties().getReceivedRoutingKey(), text);
    }
}
