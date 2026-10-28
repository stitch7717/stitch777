package com.medical.modules.system;

import com.medical.framework.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 方法级鉴权：医生账号访问用户管理接口应 403，未登录应 401。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PermissionEnforcementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("无 system:user:list 权限的医生访问用户分页返回 403")
    void doctorCannotAccessUserPage() throws Exception {
        String token = tokenFor("doc01");

        mockMvc.perform(get("/api/system/user/page")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("同一医生访问 /me 正常，未登录访问返回 401")
    void authenticatedMeWorks_unauthenticatedReturns401() throws Exception {
        String token = tokenFor("doc01");

        mockMvc.perform(get("/api/system/user/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("doc01"));

        mockMvc.perform(get("/api/system/user/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    private String tokenFor(String username) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = ?", Long.class, username);
        Integer version = jdbcTemplate.queryForObject(
                "SELECT token_version FROM sys_user WHERE id = ?", Integer.class, id);
        return jwtUtil.generate(id, username, version);
    }
}
