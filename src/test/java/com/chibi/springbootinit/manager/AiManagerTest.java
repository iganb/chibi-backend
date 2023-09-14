package com.chibi.springbootinit.manager;

import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class AiManagerTest {

    @Resource
    private AiManager aiManager;

//    @Test
//    void doChat() {
//        String answer=aiManager.doChat(1675772021360898050L,"分析需求：\n" +
//                "分析网站用户的增长情况\n" +
//                "原始数据：\n" +
//                "日期，用户数\n" +
//                "1号，10\n" +
//                "2号，30\n" +
//                "3号，50");
//        System.out.println(answer);
//    }
}