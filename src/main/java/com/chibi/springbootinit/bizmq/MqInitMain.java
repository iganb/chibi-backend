package com.chibi.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * 用于创建测试用到的交换机和队列（只执行一次）
 */
public class MqInitMain {

    public static void main(String[] args) {

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();


            String EXCHANGE_NAME="code-exchange";
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");


            String queueName = "code_queue";
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, EXCHANGE_NAME, "my_routingKey");
        }catch (Exception e){

        }


    }
}
