package com.pharmacy.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

@Configuration @Slf4j
public class CacheConfig implements CachingConfigurer {
    @Override
    public CacheErrorHandler errorHandler(){return new CacheErrorHandler(){
        public void handleCacheGetError(RuntimeException e,Cache cache,Object key){warn(e,cache,key);}
        public void handleCachePutError(RuntimeException e,Cache cache,Object key,Object value){warn(e,cache,key);}
        public void handleCacheEvictError(RuntimeException e,Cache cache,Object key){warn(e,cache,key);}
        public void handleCacheClearError(RuntimeException e,Cache cache){warn(e,cache,null);}
        private void warn(RuntimeException e,Cache cache,Object key){log.warn("Redis cache unavailable; falling back to database cache={} key={}",cache.getName(),key);}
    };}
}
