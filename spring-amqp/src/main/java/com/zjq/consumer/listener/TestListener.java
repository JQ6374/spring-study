package com.zjq.consumer.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Map;

@Component
public class TestListener {
    // 利用RabbitListener来声明要监听的队列信息
    // 将来一旦监听的队列中有了消息，就会推送给当前服务，调用当前方法，处理消息。
    // 可以看到方法体中接收的就是消息体的内容
    @RabbitListener(queues = "test.queue")
    public void listenWorkQueue1(String msg) throws InterruptedException {
        System.out.println("【test.queue】消费者1接收到消息：【" + msg + "】" + LocalTime.now());
        Thread.sleep(20);
    }

    @RabbitListener(queues = "test.queue")
    public void listenWorkQueue2(String msg) throws InterruptedException {
        System.err.println("【test.queue】消费者2........接收到消息：【" + msg + "】" + LocalTime.now());
        Thread.sleep(200);
    }

    @RabbitListener(queues = "object.queue")
    public void listenSimpleQueueMessage(Map<String, Object> msg) throws InterruptedException {
        System.out.println("消费者接收到object.queue消息：【" + msg + "】");
    }
}
