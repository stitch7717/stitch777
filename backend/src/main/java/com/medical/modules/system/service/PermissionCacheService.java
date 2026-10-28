package com.medical.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.common.cache.RedisGuard;
import com.medical.modules.system.entity.SysMenu;
import com.medical.modules.system.entity.SysRoleMenu;
import com.medical.modules.system.mapper.SysMenuMapper;
import com.medical.modules.system.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * 角色权限缓存：perms:admin / perms:role:{roleId}，TTL 5 分钟。
 * Redis 不可用时回源数据库（fail-open）；角色/菜单变更时删除缓存。
 */
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final String ADMIN_KEY = "perms:admin";
    private static final String ROLE_PREFIX = "perms:role:";

    private final StringRedisTemplate redis;
    private final RedisGuard guard;
    private final ObjectMapper objectMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public List<String> getAuthorities(boolean admin, Long roleId) {
        String key = admin ? ADMIN_KEY : ROLE_PREFIX + roleId;
        if (!guard.isDegraded()) {
            try {
                String json = redis.opsForValue().get(key);
                if (json != null) {
                    guard.markHealthy();
                    return objectMapper.readValue(json, STRING_LIST);
                }
            } catch (Exception e) {
                guard.markDegraded("perms:get");
            }
        }

        List<String> perms = loadFromDb(admin, roleId);
        if (!guard.isDegraded()) {
            try {
                redis.opsForValue().set(key, objectMapper.writeValueAsString(perms), TTL);
                guard.markHealthy();
            } catch (Exception e) {
                guard.markDegraded("perms:set");
            }
        }
        return perms;
    }

    private List<String> loadFromDb(boolean admin, Long roleId) {
        if (admin) {
            return menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                            .eq(SysMenu::getStatus, 1).isNotNull(SysMenu::getPerms))
                    .stream().map(SysMenu::getPerms).distinct().toList();
        }
        if (roleId == null) {
            return List.of();
        }
        List<Long> menuIds = roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).distinct().toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                        .in(SysMenu::getId, menuIds)
                        .eq(SysMenu::getStatus, 1).isNotNull(SysMenu::getPerms))
                .stream().map(SysMenu::getPerms).distinct().toList();
    }

    public void evictRole(Long roleId) {
        if (roleId != null) {
            delete(ROLE_PREFIX + roleId);
        }
    }

    public void evictAll() {
        if (guard.isDegraded()) {
            return;
        }
        try {
            // 演示项目 key 很少；生产环境应使用 SCAN 而不是 KEYS
            Set<String> keys = redis.keys("perms:*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
            }
            guard.markHealthy();
        } catch (Exception e) {
            guard.markDegraded("perms:evict");
        }
    }

    /**
     * 事务提交后再删除缓存，避免并发请求在提交前把旧权限重新写回。
     */
    public void evictAllAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictAll();
                }
            });
        } else {
            evictAll();
        }
    }

    private void delete(String key) {
        if (guard.isDegraded()) {
            return;
        }
        try {
            redis.delete(key);
            guard.markHealthy();
        } catch (Exception e) {
            guard.markDegraded("perms:delete");
        }
    }
}
