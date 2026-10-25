package com.medical.modules.patient.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.common.Result;
import com.medical.framework.LoginUser;
import com.medical.framework.SecurityUtils;
import com.medical.modules.clinic.entity.BizMedicalRecord;
import com.medical.modules.clinic.mapper.MedicalRecordMapper;
import com.medical.modules.patient.entity.BizFollowUp;
import com.medical.modules.patient.mapper.BizFollowUpMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FollowUpController {

    private final BizFollowUpMapper followUpMapper;
    private final MedicalRecordMapper recordMapper;

    /** 新增随访计划 */
    @PostMapping("/api/patient/followup")
    @PreAuthorize("hasAuthority('patient:followup')")
    public Result<Map<String, Long>> create(@RequestBody BizFollowUp followUp) {
        if (followUp.getPatientId() == null) {
            throw new com.medical.common.BusinessException("患者不能为空");
        }
        LoginUser user = SecurityUtils.getCurrentUser();
        followUp.setOperatorId(user.getId());
        if (followUp.getStatus() == null) followUp.setStatus(0);
        followUpMapper.insert(followUp);
        return Result.ok(Map.of("id", followUp.getId()));
    }

    /** 随访分页（patientId 可选） */
    @GetMapping("/api/patient/followup/page")
    @PreAuthorize("hasAuthority('patient:list')")
    public Result<PageResult<BizFollowUp>> page(PageQuery query, Long patientId, Integer status) {
        Page<BizFollowUp> page = followUpMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<BizFollowUp>lambdaQuery()
                        .eq(patientId != null, BizFollowUp::getPatientId, patientId)
                        .eq(status != null, BizFollowUp::getStatus, status)
                        .orderByAsc(BizFollowUp::getPlanDate).orderByDesc(BizFollowUp::getId));
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    /** 完成随访：写结果 0→1 */
    @PutMapping("/api/patient/followup/{id}/complete")
    @PreAuthorize("hasAuthority('patient:followup')")
    public Result<Void> complete(@PathVariable Long id, @RequestBody Map<String, String> body) {
        BizFollowUp follow = followUpMapper.selectById(id);
        if (follow == null) throw new com.medical.common.BusinessException("随访记录不存在");
        if (follow.getStatus() == 1) throw new com.medical.common.BusinessException("随访已完成");
        BizFollowUp patch = new BizFollowUp();
        patch.setId(id);
        patch.setResult(body.get("result"));
        patch.setStatus(1);
        followUpMapper.updateById(patch);
        return Result.ok();
    }

    /** 患者病历列表（文档 §3.2 /patient/{id}/records） */
    @GetMapping("/api/patient/{id}/records")
    @PreAuthorize("hasAuthority('patient:list')")
    public Result<List<BizMedicalRecord>> records(@PathVariable Long id) {
        return Result.ok(recordMapper.selectList(Wrappers.<BizMedicalRecord>lambdaQuery()
                .eq(BizMedicalRecord::getPatientId, id)
                .orderByDesc(BizMedicalRecord::getId)));
    }
}
