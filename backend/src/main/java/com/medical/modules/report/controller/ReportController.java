package com.medical.modules.report.controller;

import com.medical.common.BusinessException;
import com.medical.common.Result;
import com.medical.modules.report.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/outpatient")
    @PreAuthorize("hasAuthority('report:view')")
    public Result<Map<String, Object>> outpatient(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Long deptId) {
        return Result.ok(reportService.outpatient(startDate, endDate, deptId));
    }

    @GetMapping("/inpatient")
    @PreAuthorize("hasAuthority('report:view')")
    public Result<Map<String, Object>> inpatient(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(reportService.inpatient(startDate, endDate));
    }

    @GetMapping("/income")
    @PreAuthorize("hasAuthority('report:view')")
    public Result<Map<String, Object>> income(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Long deptId) {
        return Result.ok(reportService.income(startDate, endDate, deptId));
    }

    @GetMapping("/drug")
    @PreAuthorize("hasAuthority('report:view')")
    public Result<Map<String, Object>> drug(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(reportService.drug(startDate, endDate));
    }

    @GetMapping("/patient")
    @PreAuthorize("hasAuthority('report:view')")
    public Result<Map<String, Object>> patient() {
        return Result.ok(reportService.patient());
    }

    /** 数据总览（仅登录态，六角色全开） */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(reportService.overview());
    }

    @GetMapping("/depts")
    @PreAuthorize("hasAuthority('report:view')")
    public Result<List<Map<String, Object>>> depts() {
        return Result.ok(reportService.deptOptions());
    }

    /** 报表导出 CSV（BOM + Excel 中文兼容） */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('report:export')")
    public void export(@RequestParam String type,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                       HttpServletResponse response) throws IOException {
        StringBuilder csv = new StringBuilder();
        switch (type) {
            case "outpatient" -> {
                Map<String, Object> data = reportService.outpatient(startDate, endDate, null);
                csv.append("日期,门诊量\n");
                ((Map<String, Long>) data.get("trend")).forEach((k, v) -> csv.append(k).append(',').append(v).append('\n'));
                csv.append("合计,").append(data.get("total")).append('\n');
            }
            case "income" -> {
                Map<String, Object> data = reportService.income(startDate, endDate, null);
                csv.append("类别,金额\n");
                ((Map<String, BigDecimal>) data.get("byType")).forEach((k, v) -> csv.append(k).append(',').append(v).append('\n'));
                csv.append("总收入,").append(data.get("total")).append('\n');
                csv.append("退款,").append(data.get("refund")).append('\n');
            }
            case "drug" -> {
                Map<String, Object> data = reportService.drug(startDate, endDate);
                csv.append("药品,销售额\n");
                ((List<Map<String, Object>>) data.get("top10")).forEach(m ->
                        csv.append(m.get("name")).append(',').append(m.get("amount")).append('\n'));
            }
            case "patient" -> {
                Map<String, Object> data = reportService.patient();
                csv.append("年龄段,人数\n");
                ((Map<String, Long>) data.get("ageDist")).forEach((k, v) -> csv.append(k).append(',').append(v).append('\n'));
            }
            default -> throw new BusinessException("不支持的报表类型：" + type);
        }
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=report_" + type + ".csv");
        // BOM 兼容 Excel 中文
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        response.getOutputStream().write(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
}
