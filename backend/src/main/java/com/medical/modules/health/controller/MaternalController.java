package com.medical.modules.health.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.common.Result;
import com.medical.modules.health.entity.BizChildCare;
import com.medical.modules.health.entity.BizMaternalRecord;
import com.medical.modules.health.mapper.BizChildCareMapper;
import com.medical.modules.health.mapper.BizMaternalRecordMapper;
import com.medical.modules.patient.entity.BizPatient;
import com.medical.modules.patient.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class MaternalController {

    private final BizMaternalRecordMapper maternalMapper;
    private final BizChildCareMapper childMapper;
    private final PatientMapper patientMapper;

    @GetMapping("/maternal/page")
    @PreAuthorize("hasAuthority('health:list')")
    public Result<PageResult<Map<String, Object>>> maternalPage(PageQuery query) {
        Page<BizMaternalRecord> page = maternalMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<BizMaternalRecord>lambdaQuery().orderByDesc(BizMaternalRecord::getId));
        List<Map<String, Object>> rows = page.getRecords().stream().map(r -> {
            BizPatient p = patientMapper.selectById(r.getPatientId());
            return Map.<String, Object>of(
                    "id", r.getId(),
                    "patientName", p == null ? "" : p.getName(),
                    "lastPeriodDate", r.getLastPeriodDate() == null ? "" : r.getLastPeriodDate().toString(),
                    "expectedDate", r.getExpectedDate() == null ? "" : r.getExpectedDate().toString(),
                    "checkupCount", r.getCheckupCount(),
                    "status", r.getStatus(),
                    "remark", r.getRemark() == null ? "" : r.getRemark());
        }).toList();
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows));
    }

    @PostMapping("/maternal")
    @PreAuthorize("hasAuthority('health:maternal')")
    public Result<Map<String, Long>> addMaternal(@RequestBody BizMaternalRecord record) {
        if (record.getPatientId() == null) throw new com.medical.common.BusinessException("患者不能为空");
        maternalMapper.insert(record);
        return Result.ok(Map.of("id", record.getId()));
    }

    @GetMapping("/childcare/page")
    @PreAuthorize("hasAuthority('health:list')")
    public Result<PageResult<Map<String, Object>>> childPage(PageQuery query) {
        Page<BizChildCare> page = childMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<BizChildCare>lambdaQuery().orderByDesc(BizChildCare::getId));
        List<Map<String, Object>> rows = page.getRecords().stream().map(c -> {
            BizPatient p = patientMapper.selectById(c.getPatientId());
            return Map.<String, Object>of(
                    "id", c.getId(),
                    "patientName", p == null ? "" : p.getName(),
                    "birthWeight", c.getBirthWeight(),
                    "height", c.getHeight(), "weight", c.getWeight(),
                    "checkDate", c.getCheckDate() == null ? "" : c.getCheckDate().toString(),
                    "remark", c.getRemark() == null ? "" : c.getRemark());
        }).toList();
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows));
    }

    @PostMapping("/childcare")
    @PreAuthorize("hasAuthority('health:maternal')")
    public Result<Map<String, Long>> addChildCare(@RequestBody BizChildCare care) {
        if (care.getPatientId() == null) throw new com.medical.common.BusinessException("患者不能为空");
        childMapper.insert(care);
        return Result.ok(Map.of("id", care.getId()));
    }
}
