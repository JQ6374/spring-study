package com.zjq.rabbitmq.messaging;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessagePublisher {
    private final RabbitTemplate rabbitTemplate;

    public MessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public PublishReceipt publish(String exchange, String routingKey, String text) {
        String messageId = UUID.randomUUID().toString();
        CorrelationData correlation = new CorrelationData(messageId);
        rabbitTemplate.convertAndSend(exchange, routingKey, text, message -> {
            message.getMessageProperties().setMessageId(messageId);
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        }, correlation);

        try {
            // 显式等待发布确认，便于区分“已确认发布”和“业务处理成功”。
            CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.ack()) {
                throw new MessagePublishException("RabbitMQ 未确认消息发布，messageId=" + messageId);
            }
            // 无路由消息也可能收到 ACK，因此还必须检查 Return。
            if (correlation.getReturned() != null) {
                throw new MessagePublishException("消息未匹配任何队列，请检查 routingKey，messageId=" + messageId);
            }
            return new PublishReceipt(messageId, exchange, routingKey, "BROKER_CONFIRMED");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MessagePublishException("等待发布确认被中断，消息状态待核实，messageId=" + messageId, ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new MessagePublishException("未能取得发布确认，消息状态待核实，messageId=" + messageId, ex);
        }
    }
}
