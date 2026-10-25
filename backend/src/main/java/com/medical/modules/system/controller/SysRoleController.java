package com.medical.modules.system.controller;

import com.medical.common.Result;
import com.medical.modules.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<List<SysRoleService.RoleItem>> list() {
        return Result.ok(roleService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(roleService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<Map<String, Long>> create(@RequestBody SysRoleService.RoleRequest request) {
        return Result.ok(Map.of("id", roleService.create(request)));
    }

    /** 修改角色（roleName/remark/status；roleCode 不可改） */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysRoleService.RoleRequest request) {
        roleService.update(id, request);
        return Result.ok();
    }

    /** 分配权限 */
    @PutMapping("/{id}/permission")
    @PreAuthorize("hasAuthority('system:role:perm')")
    public Result<Void> permission(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        roleService.assignPermissions(id, body.get("menuIds"));
        return Result.ok();
    }

    /** 删除角色（硬删除，内置角色保护，有用户引用拒绝） */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }
}
