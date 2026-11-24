package com.zjq.rabbitmq.config;

/** 使用独立名称，避免与旧 Demo 的队列属性发生冲突。 */
public final class RabbitTopology {
    public static final String WORK_QUEUE = "study.work.queue";
    public static final String DIRECT_EXCHANGE = "study.direct";
    public static final String DIRECT_QUEUE_1 = "study.direct.queue1";
    public static final String DIRECT_QUEUE_2 = "study.direct.queue2";
    public static final String FANOUT_EXCHANGE = "study.fanout";
    public static final String FANOUT_QUEUE_1 = "study.fanout.queue1";
    public static final String FANOUT_QUEUE_2 = "study.fanout.queue2";
    public static final String TOPIC_EXCHANGE = "study.topic";
    public static final String TOPIC_QUEUE_1 = "study.topic.queue1";
    public static final String TOPIC_QUEUE_2 = "study.topic.queue2";
    public static final String DEAD_LETTER_EXCHANGE = "study.dead-letter";
    public static final String DEAD_LETTER_QUEUE = "study.dead-letter.queue";

    private RabbitTopology() {
    }
}
