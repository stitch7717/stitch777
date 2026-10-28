package com.medical.modules.system.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 登录失败计数：5 次失败锁定，clear 解锁。
 * Redis 可用时走 Redis；本地没有 Redis 时走内存回退，两种路径都应通过。
 */
@SpringBootTest
@ActiveProfiles("test")
class LoginFailureServiceTest {

    @Autowired
    private LoginFailureService loginFailureService;

    @Test
    @DisplayName("5 次失败后锁定，clear 后解锁")
    void fiveFailuresLockAndClearUnlocks() {
        String username = "lock-test-" + System.nanoTime();
        try {
            assertThat(loginFailureService.isLocked(username, 5)).isFalse();

            for (int i = 0; i < 4; i++) {
                loginFailureService.recordFailure(username, 5);
            }
            assertThat(loginFailureService.isLocked(username, 5)).isFalse();

            loginFailureService.recordFailure(username, 5);
            assertThat(loginFailureService.isLocked(username, 5)).isTrue();
        } finally {
            loginFailureService.clear(username);
        }

        assertThat(loginFailureService.isLocked(username, 5)).isFalse();
    }
}
