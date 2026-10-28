package com.medical.modules.system.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 权限缓存：Redis 可用时命中缓存，不可用时回源数据库；两种路径都应返回 admin 全量权限。
 */
@SpringBootTest
@ActiveProfiles("test")
class PermissionCacheServiceTest {

    @Autowired
    private PermissionCacheService permissionCacheService;

    @Test
    @DisplayName("admin 权限加载两次结果一致，且包含 system:user:list")
    void adminPermissionsLoadedTwiceAreConsistent() {
        List<String> first = permissionCacheService.getAuthorities(true, 1L);
        List<String> second = permissionCacheService.getAuthorities(true, 1L);

        assertThat(first).isNotEmpty().contains("system:user:list");
        assertThat(second).containsExactlyInAnyOrderElementsOf(first);
    }
}
