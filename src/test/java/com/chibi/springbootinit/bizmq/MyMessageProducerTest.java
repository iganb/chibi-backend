package com.chibi.springbootinit.bizmq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class MyMessageProducerTest {

    @Resource
    private  MyMessageProducer myMessageProducer;
    @Test
    void sendMessage() {
        myMessageProducer.sendMessage("code-exchange","my_routingKey","Good!");
    }
}