package com.medical.framework;

import com.medical.modules.system.service.SysUserAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JWT 版本号：改密（token_version +1）后，旧 token 立即 401。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtVersionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserAdminService userAdminService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("token 版本号落后于数据库时，请求返回 401")
    void oldTokenRejectedAfterVersionBump() throws Exception {
        Long adminId = adminId();
        Integer original = currentVersion(adminId);
        try {
            String token = jwtUtil.generate(adminId, "admin", original);

            mockMvc.perform(get("/api/system/user/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            jdbcTemplate.update("UPDATE sys_user SET token_version = ? WHERE id = ?", original + 1, adminId);

            mockMvc.perform(get("/api/system/user/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        } finally {
            jdbcTemplate.update("UPDATE sys_user SET token_version = ? WHERE id = ?", original, adminId);
        }
    }

    @Test
    @DisplayName("管理员重置密码时 token_version +1")
    void resetPasswordIncrementsTokenVersion() {
        Long adminId = adminId();
        Integer original = currentVersion(adminId);
        try {
            userAdminService.resetPassword(adminId, "123456");
            assertThat(currentVersion(adminId)).isEqualTo(original + 1);
        } finally {
            jdbcTemplate.update("UPDATE sys_user SET token_version = ? WHERE id = ?", original, adminId);
        }
    }

    private Long adminId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = 'admin'", Long.class);
    }

    private Integer currentVersion(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT token_version FROM sys_user WHERE id = ?", Integer.class, userId);
    }
}
