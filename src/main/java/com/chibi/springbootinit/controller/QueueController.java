package com.chibi.springbootinit.controller;


import cn.hutool.json.JSONUtil;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;


import javax.annotation.Resource;

import java.util.HashMap;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 队列测试
 *

 */
@RestController
@RequestMapping("/queue")
@Slf4j
@Profile({"dev","local"})
public class QueueController {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @GetMapping("/add")
    public void add(String name){
        CompletableFuture.runAsync(()->{
            System.out.println("任务执行中："+name+",执行人："+Thread.currentThread().getName());
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        },threadPoolExecutor);

    }

    @GetMapping("/get")
    public String get(){
        Map<String,Object> map=new HashMap<>();
        //获取线程池的队列长度
        int size=threadPoolExecutor.getQueue().size();
        map.put("队列长度：",size);
        //获取线程池已接收到的任务总数
        long taskCount=threadPoolExecutor.getTaskCount();
        map.put("任务总数：",taskCount);
        //获取线程池已完成的任务数
        long completedTaskCount=threadPoolExecutor.getCompletedTaskCount();
        map.put("已完成的任务总数：",completedTaskCount);
        //获取线程池中正在执行任务的线程数
        int activeCount = threadPoolExecutor.getActiveCount();
        map.put("执行中的线程数：",activeCount);
        //将map转换为字符串并返回
        return JSONUtil.toJsonStr(map);
    }
}
