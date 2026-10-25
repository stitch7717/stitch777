package com.medical.modules.system.controller;

import com.medical.common.Result;
import com.medical.modules.system.entity.SysDept;
import com.medical.modules.system.service.SysDeptAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class SysDeptAdminController {

    private final SysDeptAdminService adminService;

    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:edit')")
    public Result<Void> create(@RequestBody SysDept dept) {
        adminService.save(dept);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysDept dept) {
        adminService.update(id, dept);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:edit')")
    public Result<Void> delete(@PathVariable Long id) {
        adminService.delete(id);
        return Result.ok();
    }
}
