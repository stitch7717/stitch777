package com.medical.modules.patient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.common.cache.RedisGuard;
import com.medical.modules.patient.dto.PatientBasic;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 患者基础信息缓存：key = patient:phone:{phone}，TTL 2 分钟。
 *
 * <p>只缓存 id/patientNo/name，用于挂号时按手机号复用档案；
 * 过敏史、病史、身份证等临床/敏感数据仍然每次查 MySQL，不进入缓存。</p>
 */
@Service
@RequiredArgsConstructor
public class PatientCacheService {

    private static final Duration TTL = Duration.ofMinutes(2);
    private static final String PREFIX = "patient:phone:";

    private final StringRedisTemplate redis;
    private final RedisGuard guard;
    private final ObjectMapper objectMapper;

    public PatientBasic getByPhone(String phone) {
        if (!StringUtils.hasText(phone) || guard.isDegraded()) {
            return null;
        }
        try {
            String json = redis.opsForValue().get(key(phone));
            guard.markHealthy();
            return json == null ? null : objectMapper.readValue(json, PatientBasic.class);
        } catch (Exception e) {
            guard.markDegraded("patient:get");
            return null;
        }
    }

    public void putByPhone(String phone, PatientBasic basic) {
        if (!StringUtils.hasText(phone) || basic == null || guard.isDegraded()) {
            return;
        }
        try {
            redis.opsForValue().set(key(phone), objectMapper.writeValueAsString(basic), TTL);
            guard.markHealthy();
        } catch (Exception e) {
            guard.markDegraded("patient:set");
        }
    }

    public void evictPhone(String phone) {
        if (!StringUtils.hasText(phone) || guard.isDegraded()) {
            return;
        }
        try {
            redis.delete(key(phone));
            guard.markHealthy();
        } catch (Exception e) {
            guard.markDegraded("patient:delete");
        }
    }

    private String key(String phone) {
        return PREFIX + phone.trim();
    }
}
