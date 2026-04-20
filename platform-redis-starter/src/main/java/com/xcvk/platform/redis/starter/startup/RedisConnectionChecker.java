package com.xcvk.platform.redis.starter.startup;

import com.xcvk.platform.redis.starter.properties.PlatformRedisHealthCheckProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;

/**
 * Redis 连接检查器
 *
 * <p>启动后执行一次 ping，用于确认 Redis 基础连接是否可用。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-19
 */
@Slf4j
@RequiredArgsConstructor
public class RedisConnectionChecker implements ApplicationListener<ApplicationReadyEvent> {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisProperties redisProperties;
    private final PlatformRedisHealthCheckProperties properties;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String host = redisProperties.getHost();
        Integer port = redisProperties.getPort();
        int db = redisProperties.getDatabase();

        RedisConnectionFactory factory = stringRedisTemplate.getConnectionFactory();
        if (factory == null) {
            log.warn("""
                
                ======== Middleware Check (redis-starter) ========
                Redis:  FAIL
                Reason: ConnectionFactory is null
                Addr :  {}:{}
                DB   :  {}
                ================================================
                
                """, host, port, db);

            if (properties.isFailFast()) {
                throw new IllegalStateException("Redis connection factory is null");
            }
            return;
        }

        try (RedisConnection conn = factory.getConnection()) {
            String pong = conn.ping();
            log.info("""
                
                ======== Middleware Check (redis-starter) ========
                Redis:  OK
                Addr :  {}:{}
                DB   :  {}
                PING :  {}
                ================================================
                
                """, host, port, db, pong);
        } catch (Exception e) {
            log.warn("""
                
                ======== Middleware Check (redis-starter) ========
                Redis:  FAIL
                Addr :  {}:{}
                DB   :  {}
                ERR  :  {}
                ================================================
                
                """, host, port, db, rootMessage(e));

            if (properties.isFailFast()) {
                throw new IllegalStateException("Redis connection check failed", e);
            }
        }
    }

    private String rootMessage(Throwable t) {
        Throwable cur = Objects.requireNonNull(t);
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur.getClass().getSimpleName() + ": " + cur.getMessage();
    }
}