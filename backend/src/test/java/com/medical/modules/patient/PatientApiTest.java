package com.medical.modules.patient;

import com.medical.framework.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 患者建档接口：加密字段写入后，重复身份证建档返回 HTTP 409。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PatientApiTest {

    private static final String PHONE = "13600009999";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM biz_patient WHERE phone = ?", PHONE);
    }

    @Test
    @DisplayName("同一身份证第二次建档返回 409")
    void duplicateIdCard_returnsConflict() throws Exception {
        String token = adminToken();
        String body = """
                {"name":"接口测试患者","gender":1,"idCard":"440300199202029999","phone":"13600009999"}
                """;

        mockMvc.perform(post("/api/patient")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber());

        mockMvc.perform(post("/api/patient")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    private String adminToken() {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = 'admin'", Long.class);
        Integer version = jdbcTemplate.queryForObject(
                "SELECT token_version FROM sys_user WHERE id = ?", Integer.class, id);
        return jwtUtil.generate(id, "admin", version);
    }
}
