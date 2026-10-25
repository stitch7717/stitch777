package com.medical.modules.clinic.controller;

import com.medical.common.Result;
import com.medical.modules.clinic.dto.RecordRequest;
import com.medical.modules.clinic.dto.VisitVO;
import com.medical.modules.clinic.entity.BizMedicalRecord;
import com.medical.modules.clinic.service.ClinicVisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clinic/visit")
@RequiredArgsConstructor
public class ClinicVisitController {

    private final ClinicVisitService visitService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('clinic:list')")
    public Result<List<VisitVO>> page(Integer status, Long doctorId) {
        return Result.ok(visitService.todayVisits(status, doctorId));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('clinic:record')")
    public Result<Void> receive(@PathVariable Long id) {
        visitService.receive(id);
        return Result.ok();
    }

    @PostMapping("/record")
    @PreAuthorize("hasAuthority('clinic:record')")
    public Result<Void> saveRecord(@Valid @RequestBody RecordRequest request) {
        visitService.saveRecord(request);
        return Result.ok();
    }

    @GetMapping("/record")
    @PreAuthorize("hasAuthority('clinic:list')")
    public Result<BizMedicalRecord> record(@RequestParam String visitNo) {
        return Result.ok(visitService.recordByVisitNo(visitNo));
    }
}
