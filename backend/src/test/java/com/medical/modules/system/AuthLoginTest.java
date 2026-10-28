package com.medical.modules.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("正确账号密码登录成功，返回 token 与角色")
    void loginWithValidCredentials_returnsToken() throws Exception {
        mockMvc.perform(post("/api/system/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.userInfo.roleCode").value("admin"));
    }

    @Test
    @DisplayName("错误密码返回 400，不签发 token")
    void loginWithWrongPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/system/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"no_such_user_test\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
