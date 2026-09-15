package com.pharmacy.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Optional coordination accelerator. Database constraints and row locks remain the correctness boundary. */
@Configuration
public class RedissonConfig {
    @Bean(destroyMethod="shutdown")
    @ConditionalOnProperty(name="app.redis.distributed-locks-enabled",havingValue="true")
    RedissonClient redissonClient(@Value("${spring.data.redis.host}") String host,
                                  @Value("${spring.data.redis.port}") int port,
                                  @Value("${spring.data.redis.password:}") String password){
        Config config=new Config();var server=config.useSingleServer().setAddress("redis://"+host+":"+port);
        if(password!=null&&!password.isBlank())server.setPassword(password);
        return Redisson.create(config);
    }
}
