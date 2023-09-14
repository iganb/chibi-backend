package com.chibi.springbootinit;

import com.chibi.springbootinit.config.WxOpenConfig;
import javax.annotation.Resource;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * 主类测试
 *
 
 */
@SpringBootTest
class MainApplicationTests {

    @Resource
    private WxOpenConfig wxOpenConfig;

//    @Test
//    void contextLoads() {
//        System.out.println(wxOpenConfig);
//    }

}
