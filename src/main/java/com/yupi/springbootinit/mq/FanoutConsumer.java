package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class FanoutConsumer {
  private static final String EXCHANGE_NAME = "fanout-exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    Channel channel2 = connection.createChannel();

    channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
    channel2.exchangeDeclare(EXCHANGE_NAME, "fanout");
    //创建队列，随机分配一个队列
    String queueName = "小王的工作队列";
    channel.queueDeclare(queueName, true, false, false, null);
    channel.queueBind(queueName, EXCHANGE_NAME, "");


    String queueName2 = "小李的工作队列";
    channel2.queueDeclare(queueName2, true, false, false, null);
    channel2.queueBind(queueName2, EXCHANGE_NAME, "");

    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        System.out.println(" [小王] Received '" + message + "'");
    };

    DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
      String message = new String(delivery.getBody(), "UTF-8");
      System.out.println(" [小李] Received '" + message + "'");
    };

    channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });

    channel2.basicConsume(queueName2, true, deliverCallback2, consumerTag -> { });

  }
}