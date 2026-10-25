package com.medical.framework;

import lombok.Data;

/**
 * 登录用户上下文（存入 SecurityContext）
 */
@Data
public class LoginUser {

    private Long id;
    private String username;
    private String realName;
    private Long roleId;
    private String roleCode;

    public LoginUser(Long id, String username, String realName, Long roleId, String roleCode) {
        this.id = id;
        this.username = username;
        this.realName = realName;
        this.roleId = roleId;
        this.roleCode = roleCode;
    }
}
