package com.zjq.rabbitmq.messaging;

/** 仅代表 RabbitMQ 确认发布，不代表消费者业务已完成。 */
public record PublishReceipt(String messageId, String exchange, String routingKey, String status) {
}
