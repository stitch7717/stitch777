package com.medical.modules.patient.controller;

import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.common.Result;
import com.medical.modules.patient.dto.PatientPageVO;
import com.medical.modules.patient.dto.PatientRequest;
import com.medical.modules.patient.entity.BizPatient;
import com.medical.modules.patient.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('patient:list')")
    public Result<PageResult<PatientPageVO>> page(PageQuery query,
                                                  Integer gender, Integer cardStatus, String tags) {
        return Result.ok(patientService.page(query, gender, cardStatus, tags));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('patient:list')")
    public Result<BizPatient> detail(@PathVariable Long id) {
        return Result.ok(patientService.detail(id));
    }

    /** 按身份证号查询（走 HMAC 检索列，不依赖加密列等值匹配） */
    @GetMapping("/by-id-card")
    @PreAuthorize("hasAuthority('patient:list')")
    public Result<BizPatient> byIdCard(@RequestParam String idCard) {
        return Result.ok(patientService.findByIdCard(idCard));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('patient:add')")
    public Result<Map<String, Long>> create(@Valid @RequestBody PatientRequest request) {
        return Result.ok(Map.of("id", patientService.create(request)));
    }

    /** 现场登记：按手机号查重，已有档案复用，没有才新建 */
    @PostMapping("/register-or-get")
    @PreAuthorize("hasAuthority('patient:add')")
    public Result<Map<String, Object>> registerOrGet(@RequestBody PatientRequest request) {
        return Result.ok(patientService.registerOrGet(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('patient:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        patientService.update(id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('patient:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return Result.ok();
    }

    @PutMapping("/card/{id}/loss")
    @PreAuthorize("hasAuthority('patient:card')")
    public Result<Void> loss(@PathVariable Long id) {
        patientService.loss(id);
        return Result.ok();
    }

    @PostMapping("/card/reissue")
    @PreAuthorize("hasAuthority('patient:card')")
    public Result<Map<String, String>> reissue(@RequestBody Map<String, Long> body) {
        Long patientId = body.get("patientId");
        String cardNo = patientService.reissue(patientId);
        return Result.ok(Map.of("cardNo", cardNo));
    }
}
