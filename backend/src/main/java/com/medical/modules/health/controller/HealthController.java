package com.medical.modules.health.controller;

import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.common.Result;
import com.medical.modules.health.entity.BizHealthCheckup;
import com.medical.modules.health.entity.BizHealthRecord;
import com.medical.modules.health.entity.BizInfectiousReport;
import com.medical.modules.health.entity.BizVaccination;
import com.medical.modules.health.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    @GetMapping("/record/page")
    @PreAuthorize("hasAuthority('health:list')")
    public Result<PageResult<Map<String, Object>>> recordPage(PageQuery query, String chronicDisease) {
        return Result.ok(healthService.recordPage(query, chronicDisease));
    }

    @PostMapping("/record")
    @PreAuthorize("hasAuthority('health:record:add')")
    public Result<Map<String, Long>> createRecord(@RequestBody BizHealthRecord record) {
        return Result.ok(Map.of("id", healthService.createRecord(record)));
    }

    @PutMapping("/record/{id}")
    @PreAuthorize("hasAuthority('health:record:edit')")
    public Result<Void> updateRecord(@PathVariable Long id, @RequestBody BizHealthRecord patch) {
        healthService.updateRecord(id, patch);
        return Result.ok();
    }

    @GetMapping("/vaccination/page")
    @PreAuthorize("hasAuthority('health:list')")
    public Result<PageResult<BizVaccination>> vaccinationPage(PageQuery query, Long patientId) {
        return Result.ok(healthService.vaccinationPage(query, patientId));
    }

    @PostMapping("/vaccination")
    @PreAuthorize("hasAuthority('health:vaccination:add')")
    public Result<Map<String, Long>> addVaccination(@RequestBody BizVaccination vaccination) {
        return Result.ok(Map.of("id", healthService.addVaccination(vaccination)));
    }

    @GetMapping("/vaccination/remind")
    @PreAuthorize("hasAuthority('health:list')")
    public Result<List<Map<String, Object>>> remind() {
        return Result.ok(healthService.remindList());
    }

    @PostMapping("/infectious/report")
    @PreAuthorize("hasAuthority('health:infectious:report')")
    public Result<Map<String, Long>> reportInfectious(@RequestBody BizInfectiousReport report) {
        return Result.ok(Map.of("id", healthService.reportInfectious(report)));
    }

    @GetMapping("/infectious/page")
    @PreAuthorize("hasAuthority('health:list')")
    public Result<PageResult<Map<String, Object>>> infectiousPage(PageQuery query) {
        return Result.ok(healthService.infectiousPage(query));
    }

    @GetMapping("/checkup/page")
    @PreAuthorize("hasAuthority('health:list')")
    public Result<PageResult<Map<String, Object>>> checkupPage(PageQuery query) {
        return Result.ok(healthService.checkupPage(query));
    }

    @PostMapping("/checkup")
    @PreAuthorize("hasAuthority('health:checkup:add')")
    public Result<Map<String, Long>> addCheckup(@RequestBody BizHealthCheckup checkup) {
        return Result.ok(Map.of("id", healthService.addCheckup(checkup)));
    }
}
