package com.zjq.rabbitmq.web;

import com.zjq.rabbitmq.messaging.MessagePublisher;
import com.zjq.rabbitmq.messaging.PublishReceipt;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.zjq.rabbitmq.config.RabbitTopology.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessagePublisher publisher;

    public MessageController(MessagePublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/work")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PublishReceipt work(@Valid @RequestBody PublishRequest request) {
        return publisher.publish("", WORK_QUEUE, request.text());
    }

    @PostMapping("/direct")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PublishReceipt direct(@RequestParam(defaultValue = "red")
                                 @NotBlank @Size(max = 255)
                                 @Pattern(regexp = "[A-Za-z0-9_.-]+") String routingKey,
                                 @Valid @RequestBody PublishRequest request) {
        return publisher.publish(DIRECT_EXCHANGE, routingKey, request.text());
    }

    @PostMapping("/fanout")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PublishReceipt fanout(@Valid @RequestBody PublishRequest request) {
        return publisher.publish(FANOUT_EXCHANGE, "", request.text());
    }

    @PostMapping("/topic")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PublishReceipt topic(@RequestParam(defaultValue = "china.news") @Size(max = 255)
                                @Pattern(regexp = "[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*") String routingKey,
                                @Valid @RequestBody PublishRequest request) {
        return publisher.publish(TOPIC_EXCHANGE, routingKey, request.text());
    }

    public record PublishRequest(@NotBlank @Size(max = 1000) String text) {
    }
}
