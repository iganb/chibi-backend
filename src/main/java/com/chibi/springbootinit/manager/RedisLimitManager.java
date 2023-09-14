package com.chibi.springbootinit.manager;


import com.chibi.springbootinit.common.ErrorCode;
import com.chibi.springbootinit.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 专门提供RedisLimitManager限流基础服务（提供了通用的能力）
 */
@Service
public class RedisLimitManager {

    @Resource
    private RedissonClient redissonClient;


    /**
     * 限流操作
     * @param key 区分不同的限流器，比如不同的
     */
    public void doRateLimit(String key){
        //创建一个名称为user-limiter的限流器，每秒最多访问2次
        RRateLimiter rateLimiter=redissonClient.getRateLimiter(key);
        //每俩个请求，连续的请求只有一个请求允许被通过
//        RateType.OVERALL限制所有请求速率
        rateLimiter.trySetRate(RateType.OVERALL,2,1, RateIntervalUnit.SECONDS);

        //每当一个操作来了之后，请求一个令牌
        boolean canOp=rateLimiter.tryAcquire(1);
        if (!canOp){
            throw  new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
