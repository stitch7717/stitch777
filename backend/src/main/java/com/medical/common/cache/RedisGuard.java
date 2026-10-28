package com.medical.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis 熔断开关：Redis 故障时 30 秒内直接走数据库/内存，避免每个请求都等连接超时。
 * 这是 fail-open 设计：缓存不可用不影响主流程，但会退化为查库。
 */
@Slf4j
@Component
public class RedisGuard {

    private static final long DEGRADE_MILLIS = 30_000L;
    private final AtomicLong degradeUntil = new AtomicLong(0L);

    public boolean isDegraded() {
        return System.currentTimeMillis() < degradeUntil.get();
    }

    public void markDegraded(String scene) {
        long now = System.currentTimeMillis();
        long until = now + DEGRADE_MILLIS;
        long previous = degradeUntil.getAndSet(until);
        if (previous <= now) {
            log.warn("Redis 访问失败（{}），{} 秒内直接走数据库/内存", scene, DEGRADE_MILLIS / 1000);
        }
    }

    public void markHealthy() {
        if (degradeUntil.getAndSet(0L) > 0L) {
            log.info("Redis 已恢复，缓存重新启用");
        }
    }
}
