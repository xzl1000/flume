package org.apache.flume.sink.rocketmq;

import org.apache.flume.*;
import org.apache.flume.conf.Configurable;
import org.apache.flume.instrumentation.SinkCounter;
import org.apache.flume.sink.AbstractSink;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author xuzl
 * @version 1.0.0
 * @ClassName RocketMQSink.java
 * @Description TODO
 * @createTime 2023-05-07 16:49
 */
public class RocketMQSink extends AbstractSink implements Configurable {

    private static final Logger LOG = LoggerFactory.getLogger(RocketMQSink.class);

    private String topic;

    private String tag;

    private MQProducer producer;

    private String allow;

    private String deny;

    private String extra;

    private boolean asyn = true;//是否异步发送

    private SinkCounter counter;

    @Override public void configure(Context context) {
        // 获取配置项
        topic = context.getString(RocketMQSinkConstants.TOPIC, RocketMQSinkConstants.DEFAULT_TOPIC);
        tag = context.getString(RocketMQSinkConstants.TAG, RocketMQSinkConstants.DEFAULT_TAG);
        // 初始化Producer
        producer = RocketMQSinkUtil.getProducerInstance(context);

        allow = context.getString(RocketMQSinkConstants.ALLOW, null);
        deny = context.getString(RocketMQSinkConstants.DENY, null);
        extra = context.getString(RocketMQSinkConstants.EXTRA,null);

        asyn = context.getBoolean(RocketMQSinkConstants.ASYN, true);

        if ( null == counter ) {
            counter = new SinkCounter(getName());
        }

        if ( LOG.isInfoEnabled() ) {
            LOG.info("RocketMQSource configure success, topic={},tag={},allow={},deny={},extra={}, asyn={}", topic, tag, allow, deny, extra, asyn);
        }

    }


    @Override public Status process() throws EventDeliveryException {
        Channel channel = getChannel();
        Transaction tx = channel.getTransaction();
        try {
            tx.begin();
            Event event = channel.take();
            if ( event == null || event.getBody() == null || event.getBody().length == 0 ) {
                tx.commit();
                return Status.READY;
            }

            if ( null != deny && deny.trim().length() > 0 ) {
                String msg = new String(event.getBody(), "UTF-8");
                if ( msg.matches(deny) ) {
                    tx.commit();
                    msg = null;
                    return Status.READY;
                }
            }

            if ( null != allow && allow.trim().length() > 0 ) {
                String msg = new String(event.getBody(), "UTF-8");
                if ( !msg.matches(allow) ) {
                    tx.commit();
                    msg = null;
                    return Status.READY;
                }
            }

            // 发送消息
            final Message msg = new Message(topic, tag, event.getBody());
            counter.incrementEventDrainAttemptCount();

            if (null != event.getHeaders() && event.getHeaders().size() > 0 ){
                for ( Map.Entry<String,String> entry : event.getHeaders().entrySet() ){
                    msg.putUserProperty(entry.getKey(),entry.getValue());
                }
            }
            if ( null != extra && extra.length() > 0 ){
                msg.putUserProperty("extra",extra);
            }

            if ( asyn ) {
                producer.send(msg, new SendCallback() {

                    @Override public void onSuccess(SendResult sendResult) {
                        LOG.debug("send success msg:{},result:{}",msg,sendResult);
                        counter.incrementEventDrainSuccessCount();
                    }

                    @Override public void onException(Throwable e) {
                        System.out.println("send exception->" + e);
                        LOG.error("send exception->", e);
                        SendResult result = null;
                        try {
                            result = producer.send(msg);//异步发送失败，会再同步发送一次
                            if ( null == result || result.getSendStatus() != SendStatus.SEND_OK ) {
                                LOG.warn("sync send msg fail:sendResult={}", result);
                            }
                        } catch ( Exception e1 ) {
                            LOG.error("asyn send msg retry fail: sendResult=" + result, e);
                        }
                    }
                });
            } else {
                SendResult sendResult = producer.send(msg); //默认失败会重试
                LOG.debug("sendResult->{}", sendResult);
                if ( null == sendResult || sendResult.getSendStatus() != SendStatus.SEND_OK ) {
                    LOG.warn("sync send msg fail:sendResult={}", sendResult);
                }else{
                    counter.incrementEventDrainSuccessCount();
                }
            }
            tx.commit();
            return Status.READY;
        } catch ( Exception e ) {
            LOG.error("RocketMQSink send message exception", e);
            try {
                tx.rollback();
                return Status.BACKOFF;
            } catch ( Exception e2 ) {
                LOG.error("Rollback exception", e2);
            }
            return Status.BACKOFF;
        } finally {
            tx.close();
        }
    }

    @Override
    public synchronized void start() {
        try {
            LOG.warn("RocketMQSink start producer... ");
            producer.start();
            counter.start();
            counter.incrementConnectionCreatedCount();
        } catch ( MQClientException e ) {
            LOG.error("RocketMQSink start producer failed", e);
        }
        super.start();
    }

    @Override
    public synchronized void stop() {
        // 停止Producer
        producer.shutdown();
        counter.incrementConnectionClosedCount();
        counter.stop();
        super.stop();
        LOG.warn("RocketMQSink stop producer... ");
    }
}
