package com.medical.modules.clinic.controller;

import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.common.Result;
import com.medical.modules.clinic.dto.PrescriptionRequest;
import com.medical.modules.clinic.dto.PrescriptionVO;
import com.medical.modules.clinic.entity.BizDrug;
import com.medical.modules.clinic.service.PrescriptionService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.modules.clinic.mapper.DrugMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clinic")
@RequiredArgsConstructor
public class ClinicPrescriptionController {

    private final PrescriptionService prescriptionService;
    private final DrugMapper drugMapper;

    @PostMapping("/prescription")
    @PreAuthorize("hasAuthority('clinic:prescribe')")
    public Result<Map<String, Object>> create(@Valid @RequestBody PrescriptionRequest request) {
        return Result.ok(prescriptionService.create(request));
    }

    @GetMapping("/prescription/page")
    @PreAuthorize("hasAuthority('clinic:list')")
    public Result<PageResult<PrescriptionVO>> page(PageQuery query, Long visitId, Integer status) {
        return Result.ok(prescriptionService.page(query, visitId, status));
    }

    @PutMapping("/prescription/{id}/audit")
    @PreAuthorize("hasAuthority('clinic:audit')")
    public Result<Void> audit(@PathVariable Long id) {
        prescriptionService.audit(id);
        return Result.ok();
    }

    @DeleteMapping("/prescription/{id}")
    @PreAuthorize("hasAuthority('clinic:prescribe')")
    public Result<Void> voidPrescription(@PathVariable Long id) {
        prescriptionService.voidPrescription(id);
        return Result.ok();
    }

    /** 开方选品下拉（最小字段：id/名称/规格/售价/单位） */
    @GetMapping("/drugs")
    @PreAuthorize("hasAuthority('clinic:prescribe')")
    public Result<List<Map<String, Object>>> drugs() {
        List<Map<String, Object>> list = drugMapper.selectList(Wrappers.<BizDrug>lambdaQuery()
                        .eq(BizDrug::getStatus, 1).orderByAsc(BizDrug::getId))
                .stream().map(d -> Map.<String, Object>of(
                        "id", d.getId(),
                        "name", d.getDrugName(),
                        "spec", d.getSpec() == null ? "" : d.getSpec(),
                        "salePrice", d.getSalePrice(),
                        "unit", d.getUnit() == null ? "" : d.getUnit()))
                .toList();
        return Result.ok(list);
    }
}
