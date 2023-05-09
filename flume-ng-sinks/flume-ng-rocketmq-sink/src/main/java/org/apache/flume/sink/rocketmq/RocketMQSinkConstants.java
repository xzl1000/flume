package org.apache.flume.sink.rocketmq;

/**
 * @author xuzl
 * @version 1.0.0
 * @ClassName RocketMQSinkConstants.java
 * @Description TODO
 * @createTime 2023-05-07 17:00
 */
public class RocketMQSinkConstants {
    public static final String PROPERTY_PREFIX = "rocketmq.";


    /* Properties */
    public static final String TOPIC = "topic";
    public static final String PRODUCER_GROUP = "producerGroup";
    public static final String TAG = "tag";
    public static final String ALLOW = "allow";
    public static final String DENY = "deny";
    public static final String ASYN = "asyn";
    public static final String NAMESRVADDR = "namesrvAddr";
    public static final String EXTRA = "extra";

    /* defalut */
    public static final String DEFAULT_TOPIC = "TOPIC_FLUME_NGINX";
    public static final String DEFAULT_PRODUCER_GROUP = "default_producer_group";
    public static final String DEFAULT_TAG = "TAG_FLUME_NGINX";
}
