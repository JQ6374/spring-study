package com.zjq.rabbitmq.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.zjq.rabbitmq.config.RabbitTopology.*;

@Configuration(proxyBeanMethods = false)
public class RabbitTopologyConfig {
    @Bean
    Declarables workTopology() {
        // 默认交换机自动以队列名称为 routing key 绑定此队列。
        return new Declarables(learningQueue(WORK_QUEUE));
    }

    @Bean
    Declarables directTopology() {
        DirectExchange exchange = new DirectExchange(DIRECT_EXCHANGE);
        Queue first = learningQueue(DIRECT_QUEUE_1);
        Queue second = learningQueue(DIRECT_QUEUE_2);
        return new Declarables(exchange, first, second,
                BindingBuilder.bind(first).to(exchange).with("red"),
                BindingBuilder.bind(first).to(exchange).with("blue"),
                BindingBuilder.bind(second).to(exchange).with("red"),
                BindingBuilder.bind(second).to(exchange).with("yellow"));
    }

    @Bean
    Declarables fanoutTopology() {
        FanoutExchange exchange = new FanoutExchange(FANOUT_EXCHANGE);
        Queue first = learningQueue(FANOUT_QUEUE_1);
        Queue second = learningQueue(FANOUT_QUEUE_2);
        return new Declarables(exchange, first, second,
                BindingBuilder.bind(first).to(exchange),
                BindingBuilder.bind(second).to(exchange));
    }

    @Bean
    Declarables topicTopology() {
        TopicExchange exchange = new TopicExchange(TOPIC_EXCHANGE);
        Queue first = learningQueue(TOPIC_QUEUE_1);
        Queue second = learningQueue(TOPIC_QUEUE_2);
        return new Declarables(exchange, first, second,
                BindingBuilder.bind(first).to(exchange).with("china.#"),
                BindingBuilder.bind(second).to(exchange).with("#.news"));
    }

    @Bean
    Declarables deadLetterTopology() {
        FanoutExchange exchange = new FanoutExchange(DEAD_LETTER_EXCHANGE);
        Queue queue = QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
        // 不自动消费死信，便于在管理界面观察和排查。
        return new Declarables(exchange, queue, BindingBuilder.bind(queue).to(exchange));
    }

    private Queue learningQueue(String name) {
        return QueueBuilder.durable(name).deadLetterExchange(DEAD_LETTER_EXCHANGE).build();
    }
}
