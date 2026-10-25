package com.medical.modules.system.dto;

import lombok.Data;

/**
 * 用户分页行（脱敏：不含 password）
 */
@Data
public class UserPageVO {

    private Long id;
    private String username;
    private String realName;
    private Integer gender;
    private String phone;
    private String email;
    private Long deptId;
    private String deptName;
    private Long roleId;
    private String roleName;
    private Integer status;
    private String createTime;
}
