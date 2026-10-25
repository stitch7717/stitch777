package com.medical.modules.system.dto;

import lombok.Data;

/**
 * 登录响应（接口文档 §2.2 示例：token + userInfo，阶段3 追加 roleName）
 */
@Data
public class LoginResponse {

    private String token;

    private UserInfo userInfo;

    @Data
    public static class UserInfo {
        private Long id;
        private String realName;
        private String roleCode;
        private String roleName;
    }
}
