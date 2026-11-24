package com.zjq.rabbitmq.web;

import com.zjq.rabbitmq.messaging.MessagePublishException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PublishExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(PublishExceptionHandler.class);

    @ExceptionHandler(MessagePublishException.class)
    ProblemDetail publishFailed(MessagePublishException ex) {
        log.warn("消息发布未成功确认：{}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(AmqpException.class)
    ProblemDetail brokerUnavailable(AmqpException ex) {
        log.warn("RabbitMQ 连接或发送异常", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "无法确认消息已发布，请检查 RabbitMQ 连接；重试前需核实消息是否已送达。");
    }
}
