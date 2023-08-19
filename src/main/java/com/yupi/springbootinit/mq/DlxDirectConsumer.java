package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * 死信队列消费者
 */
public class DlxDirectConsumer {
    private static final String DEAD_EXCHANGE_NAME = "dlx-direct_exchange";
    private static final String WORK_EXCHANGE_NAME = "direct2_exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(WORK_EXCHANGE_NAME, "direct");

        //指定死信队列参数
        Map<String, Object> args = new HashMap<String, Object>();
        //要绑定到哪个交换机
        args.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
        //指定死信要转发到哪个死信队列
        args.put("x-dead-letter-routing-key", "waibao");


        String queueName = "xiaoyu_queue";
        channel.queueDeclare(queueName, true, false, false, args);
        channel.queueBind(queueName, WORK_EXCHANGE_NAME, "xiaoyu");


        Map<String, Object> args2 = new HashMap<String, Object>();
        args2.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
        args2.put("x-dead-letter-routing-key", "laoban");

        String queueName2 = "xiaopi_queue";
        channel.queueDeclare(queueName2, true, false, false, args2);
        channel.queueBind(queueName2, WORK_EXCHANGE_NAME, "xiaopi");


        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback xiaoyudeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            //拒绝消息
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(),false,false);
            System.out.println(" [xiaoyu] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        DeliverCallback xiaopideliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            //拒绝消息
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(),false,false);
            System.out.println(" [xiaopi] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        channel.basicConsume(queueName, false, xiaoyudeliverCallback, consumerTag -> {
        });

        channel.basicConsume(queueName2, false, xiaopideliverCallback, consumerTag -> {
        });
    }
}