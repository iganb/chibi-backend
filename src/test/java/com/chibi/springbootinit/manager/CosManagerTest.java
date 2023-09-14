package com.chibi.springbootinit.manager;

import javax.annotation.Resource;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Cos 操作测试
 *
 
 */
@SpringBootTest
class CosManagerTest {

    @Resource
    private CosManager cosManager;

//    @Test
//    void putObject() {
//        cosManager.putObject("test", "test.json");
//    }
}