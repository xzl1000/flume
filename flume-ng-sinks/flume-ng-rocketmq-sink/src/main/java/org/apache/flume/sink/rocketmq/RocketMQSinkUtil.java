package org.apache.flume.sink.rocketmq;

import org.apache.flume.Context;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;

/**
 * @author xuzl
 * @version 1.0.0
 * @ClassName RocketMQSinkUtil.java
 * @Description TODO
 * @createTime 2023-05-07 17:01
 */
public class RocketMQSinkUtil {
    public static MQProducer getProducerInstance(Context context) {
        final String producerGroup = context.getString(RocketMQSinkConstants.PRODUCER_GROUP, RocketMQSinkConstants.DEFAULT_PRODUCER_GROUP);
        System.out.println("----------producerGroup is "+producerGroup+" -----------");

        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);

        String nameSrvAddr = context.getString(RocketMQSinkConstants.NAMESRVADDR);
        if ( null != nameSrvAddr && nameSrvAddr.trim().length() > 0 ){
            checkNotNullNorEmpty("nameSrvAddr",nameSrvAddr);
            producer.setNamesrvAddr(nameSrvAddr);
        }else{
            nameSrvAddr= System.getProperty("rocketmq.namesrv.domain", null);
            if(nameSrvAddr.contains(":")){//包含port的话，就设置producer的nameSrvAddr
                producer.setNamesrvAddr(nameSrvAddr);//from jvm
            }else{
                System.out.println("------------nameSrvAddr is "+nameSrvAddr+" and not set producer.namesrvAddr---------------");
            }
        }

        System.out.println("----------nameSrvAddr is "+nameSrvAddr+" -----------");
        return producer;
    }

    public static void checkNotNullNorEmpty(String name, String s) {
        if (null == s || s.trim().length() == 0) {
            throw new IllegalArgumentException(name + " should not null nor empty.");
        }
    }
}
