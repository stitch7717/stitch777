package com.medical.modules.system.controller;

import com.medical.common.Result;
import com.medical.framework.LoginUser;
import com.medical.framework.SecurityUtils;
import com.medical.modules.system.dto.LoginRequest;
import com.medical.modules.system.dto.LoginResponse;
import com.medical.modules.system.dto.UserPageVO;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.modules.system.service.AuthService;
import com.medical.modules.system.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final AuthService authService;
    private final SysUserService userService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        // 无状态 JWT：登出由前端清除 token 实现，预留服务端黑名单扩展
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping("/page")
    public Result<PageResult<UserPageVO>> page(PageQuery query,
                                               Long deptId, Long roleId, Integer status) {
        return Result.ok(userService.page(query, deptId, roleId, status));
    }

    @GetMapping("/me")
    public Result<LoginUser> me() {
        return Result.ok(SecurityUtils.getCurrentUser());
    }
}
