package com.medical.modules.system.service;

import com.medical.common.cache.RedisGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败计数：优先 Redis（多实例共享、自动过期），Redis 不可用时回退到进程内 Map。
 *
 * <p>锁定时长 10 分钟，阈值由 AuthService 从 security.fail-count 读取后传入。</p>
 */
@Service
@RequiredArgsConstructor
public class LoginFailureService {

    private static final Duration LOCK_TTL = Duration.ofMinutes(10);
    private static final String PREFIX = "login:fail:";

    /**
     * INCR + EXPIRE 原子执行：第一次失败设置 TTL，达到阈值时重置 TTL（从锁定时刻重新计时）。
     */
    private static final DefaultRedisScript<Long> RECORD_SCRIPT = new DefaultRedisScript<>("""
            local c = redis.call('INCR', KEYS[1])
            if c == 1 or c >= tonumber(ARGV[2]) then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return c
            """, Long.class);

    private final StringRedisTemplate redis;
    private final RedisGuard guard;

    private final ConcurrentHashMap<String, Integer> memoryCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> memoryLockUntil = new ConcurrentHashMap<>();

    public boolean isLocked(String username, int maxFails) {
        if (!guard.isDegraded()) {
            try {
                String value = redis.opsForValue().get(key(username));
                guard.markHealthy();
                return value != null && Integer.parseInt(value) >= maxFails;
            } catch (Exception e) {
                guard.markDegraded("login-fail:get");
            }
        }
        Long until = memoryLockUntil.get(username);
        return until != null && until > System.currentTimeMillis();
    }

    public void recordFailure(String username, int maxFails) {
        if (!guard.isDegraded()) {
            try {
                redis.execute(RECORD_SCRIPT, List.of(key(username)),
                        String.valueOf(LOCK_TTL.toSeconds()), String.valueOf(maxFails));
                guard.markHealthy();
                return;
            } catch (Exception e) {
                guard.markDegraded("login-fail:incr");
            }
        }
        int count = memoryCount.merge(username, 1, Integer::sum);
        if (count >= maxFails) {
            memoryLockUntil.put(username, System.currentTimeMillis() + LOCK_TTL.toMillis());
        }
    }

    public void clear(String username) {
        if (!guard.isDegraded()) {
            try {
                redis.delete(key(username));
                guard.markHealthy();
            } catch (Exception e) {
                guard.markDegraded("login-fail:del");
            }
        }
        memoryCount.remove(username);
        memoryLockUntil.remove(username);
    }

    private String key(String username) {
        return PREFIX + username;
    }
}
