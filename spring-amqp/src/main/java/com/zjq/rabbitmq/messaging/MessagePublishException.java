package com.zjq.rabbitmq.messaging;

public class MessagePublishException extends RuntimeException {
    public MessagePublishException(String message) {
        super(message);
    }

    public MessagePublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
