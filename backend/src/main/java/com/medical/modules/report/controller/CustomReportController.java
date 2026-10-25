package com.medical.modules.report.controller;

import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.common.Result;
import com.medical.modules.report.entity.SysReportConfig;
import com.medical.modules.report.service.CustomReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/report/custom")
@RequiredArgsConstructor
public class CustomReportController {

    private final CustomReportService customReportService;

    /** 数据集元数据（不暴露表名与列名） */
    @GetMapping("/datasets")
    @PreAuthorize("hasAuthority('report:custom')")
    public Result<Map<String, Object>> datasets() {
        return Result.ok(customReportService.datasets());
    }

    @PostMapping("/execute")
    @PreAuthorize("hasAuthority('report:custom')")
    public Result<Map<String, Object>> execute(@RequestBody Map<String, Object> config) {
        return Result.ok(customReportService.execute(config));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('report:custom')")
    public Result<Map<String, Long>> save(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of("id", customReportService.save(body)));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('report:custom')")
    public Result<PageResult<SysReportConfig>> page(PageQuery query) {
        return Result.ok(customReportService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('report:custom')")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(customReportService.detail(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('report:custom')")
    public Result<Void> delete(@PathVariable Long id) {
        customReportService.delete(id);
        return Result.ok();
    }
}
