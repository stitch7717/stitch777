package com.medical.modules.system.controller;

import com.medical.common.Result;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.service.SysUserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class SysUserAdminController {

    private final SysUserAdminService adminService;

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Map<String, Long>> create(@RequestBody SysUser user) {
        return Result.ok(Map.of("id", adminService.create(user)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        adminService.update(id, user);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> delete(@PathVariable Long id) {
        adminService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> status(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminService.changeStatus(id, body.get("status"));
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        adminService.resetPassword(id, body == null ? null : body.get("password"));
        return Result.ok();
    }
}
