package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class MultiConsumer {

    private static final String TASK_QUEUE_NAME = "multi_queue";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        final Connection connection = factory.newConnection();


        for (int i = 0; i < 2; i++) {
            final Channel channel = connection.createChannel();
// 声明一个队列,并设置属性:队列名称,持久化,非排他,非自动删除,其他参数;如果队列不存在,则创建它
            channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
// 设置预取计数为 1，这样RabbitMQ就会在给消费者新消息之前等待先前的消息被确认
            channel.basicQos(1);
// 创建消息接收回调函数,以便接收消息
            final  int finalI=i;
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");


                try {
                    System.out.println(" [x] Received '" +"编号"+finalI+":"+ message + "'");
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(),false);
                    //停20秒，模拟机器处理能力有限
                    Thread.sleep(20000);

                } catch (InterruptedException e) {


                    e.printStackTrace();
                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(),false,false);
                } finally {
                    System.out.println(" [x] Done");
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }


            };
            channel.basicConsume(TASK_QUEUE_NAME, false, deliverCallback, consumerTag -> {
            });
        }

    }
// (不用doWork)用于模拟消息处理的函数，消息中的每一个'.'字符都会让线程暂停一秒钟
//    private static void doWork(String task) {
//        for (char ch : task.toCharArray()) {
//            if (ch == '.') {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException _ignored) {
//                    Thread.currentThread().interrupt();
//                }
//            }
//        }
//    }
}