package com.medical.modules.audit.controller;

import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.common.Result;
import com.medical.modules.audit.entity.SysBackup;
import com.medical.modules.audit.entity.SysConfig;
import com.medical.modules.audit.entity.SysLoginLog;
import com.medical.modules.audit.entity.SysOperationLog;
import com.medical.modules.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    // ---------------- 操作日志 ----------------

    @GetMapping("/api/audit/operation/page")
    @PreAuthorize("hasAuthority('audit:log:view')")
    public Result<PageResult<SysOperationLog>> operationPage(PageQuery query, String module,
                                                             Integer status, String username) {
        return Result.ok(auditService.operationPage(query, module, status, username));
    }

    @GetMapping("/api/audit/operation/modules")
    @PreAuthorize("hasAuthority('audit:log:view')")
    public Result<List<String>> modules() {
        return Result.ok(auditService.modules());
    }

    // ---------------- 登录日志 ----------------

    @GetMapping("/api/audit/login/page")
    @PreAuthorize("hasAuthority('audit:log:view')")
    public Result<PageResult<SysLoginLog>> loginPage(PageQuery query, String username, Integer status) {
        return Result.ok(auditService.loginPage(query, username, status));
    }

    // ---------------- 备份 ----------------

    @PostMapping("/api/backup")
    @PreAuthorize("hasAuthority('backup:add')")
    public Result<Map<String, Object>> backup() throws Exception {
        return Result.ok(auditService.backup());
    }

    @GetMapping("/api/backup/page")
    @PreAuthorize("hasAuthority('audit:log:view')")
    public Result<PageResult<SysBackup>> backupPage(PageQuery query) {
        return Result.ok(auditService.backupPage(query));
    }

    @PostMapping("/api/backup/{id}/restore")
    @PreAuthorize("hasAuthority('backup:add')")
    public Result<Void> restore(@PathVariable Long id, @RequestBody Map<String, Boolean> body) throws Exception {
        auditService.restore(id, Boolean.TRUE.equals(body.get("confirm")));
        return Result.ok();
    }

    @DeleteMapping("/api/backup/{id}")
    @PreAuthorize("hasAuthority('backup:add')")
    public Result<Void> deleteBackup(@PathVariable Long id) {
        auditService.deleteBackup(id);
        return Result.ok();
    }

    // ---------------- 安全配置 ----------------

    @GetMapping("/api/security/config")
    @PreAuthorize("hasAuthority('security:config')")
    public Result<List<SysConfig>> config() {
        return Result.ok(auditService.configList());
    }

    @PutMapping("/api/security/config")
    @PreAuthorize("hasAuthority('security:config')")
    public Result<Void> updateConfig(@RequestBody Map<String, String> values) {
        auditService.updateConfig(values);
        return Result.ok();
    }
}
