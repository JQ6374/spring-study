package com.zjq.rabbitmq.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class MessagePublisherTest {
    private final RabbitTemplate template = mock(RabbitTemplate.class);
    private final MessagePublisher publisher = new MessagePublisher(template);

    @Test
    void receiptRequiresBrokerConfirmationAndIncludesMessageId() {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            MessagePostProcessor processor = invocation.getArgument(3);
            Message message = processor.postProcessMessage(new Message(new byte[0], new MessageProperties()));
            assertThat(message.getMessageProperties().getMessageId()).isEqualTo(correlation.getId());
            assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(template).convertAndSend(eq("study.direct"), eq("red"), eq("hello"),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        PublishReceipt receipt = publisher.publish("study.direct", "red", "hello");
        assertThat(receipt.messageId()).isNotBlank();
        assertThat(receipt.status()).isEqualTo("BROKER_CONFIRMED");
    }

    @Test
    void ackDoesNotHideAnUnroutableMessage() {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.setReturned(new ReturnedMessage(
                    new Message(new byte[0], new MessageProperties()), 312, "NO_ROUTE", "study.direct", "unknown"));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(template).convertAndSend(eq("study.direct"), eq("unknown"), eq("hello"),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThatThrownBy(() -> publisher.publish("study.direct", "unknown", "hello"))
                .isInstanceOf(MessagePublishException.class).hasMessageContaining("未匹配任何队列");
    }

    @Test
    void brokerNackIsNotReportedAsSuccess() {
        answerWithConfirmation(new CorrelationData.Confirm(false, "broker rejected"));
        assertThatThrownBy(() -> publisher.publish("study.direct", "red", "hello"))
                .isInstanceOf(MessagePublishException.class).hasMessageContaining("未确认消息发布");
    }

    @Test
    void failedConfirmationDoesNotClaimThatMessageWasLost() {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.getFuture().completeExceptionally(new IllegalStateException("connection closed"));
            return null;
        }).when(template).convertAndSend(eq("study.direct"), eq("red"), eq("hello"),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThatThrownBy(() -> publisher.publish("study.direct", "red", "hello"))
                .isInstanceOf(MessagePublishException.class).hasMessageContaining("消息状态待核实");
    }

    private void answerWithConfirmation(CorrelationData.Confirm confirm) {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.getFuture().complete(confirm);
            return null;
        }).when(template).convertAndSend(eq("study.direct"), eq("red"), eq("hello"),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }
}
