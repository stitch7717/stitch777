package com.medical.modules.exam.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.common.Result;
import com.medical.modules.exam.dto.ApplyRequest;
import com.medical.modules.exam.dto.ApplyVO;
import com.medical.modules.exam.entity.BizExamApply;
import com.medical.modules.exam.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;
    private final com.medical.modules.exam.mapper.BizExamApplyMapper applyMapper;

    /** 开检查申请（门诊开单同用） */
    @PostMapping("/apply")
    @PreAuthorize("hasAuthority('exam:apply:add')")
    public Result<Map<String, Object>> apply(@Valid @RequestBody ApplyRequest request) {
        return Result.ok(examService.apply(request));
    }

    /** 申请分页（registrationId/status/examItem/doctorId 可选） */
    @GetMapping("/apply/page")
    @PreAuthorize("hasAuthority('exam:list')")
    public Result<PageResult<ApplyVO>> page(PageQuery query, Long registrationId,
                                            Integer status, String examItem, Long doctorId) {
        return Result.ok(examService.page(query, registrationId, status, examItem, doctorId));
    }

    /** 样本采集（1待采样→2检验中） */
    @PostMapping("/sample")
    @PreAuthorize("hasAuthority('exam:report:add')")
    public Result<Void> sample(@RequestBody Map<String, Long> body) {
        examService.sample(body.get("applyId"));
        return Result.ok();
    }

    /** 录入结果（草稿 upsert） */
    @PostMapping("/report")
    @PreAuthorize("hasAuthority('exam:report:add')")
    public Result<Map<String, Long>> saveReport(@RequestBody com.medical.modules.exam.dto.ReportRequest request) {
        return Result.ok(Map.of("id", examService.saveReport(request)));
    }

    /** 报告审核（写 auditor_id，状态仍 0） */
    @PutMapping("/report/{id}/audit")
    @PreAuthorize("hasAuthority('exam:report:audit')")
    public Result<Void> audit(@PathVariable Long id) {
        examService.audit(id);
        return Result.ok();
    }

    /** 报告发布（0→1 并联动检查单 2→3） */
    @PutMapping("/report/{id}/publish")
    @PreAuthorize("hasAuthority('exam:report:audit')")
    public Result<Void> publish(@PathVariable Long id) {
        examService.publish(id);
        return Result.ok();
    }

    /** 报告详情 */
    @GetMapping("/report/{id}")
    @PreAuthorize("hasAuthority('exam:list')")
    public Result<Map<String, Object>> reportDetail(@PathVariable Long id) {
        return Result.ok(examService.reportDetail(id));
    }

    /** 危急值处理 */
    @PostMapping("/critical/handle")
    @PreAuthorize("hasAuthority('exam:report:audit')")
    public Result<Void> handleCritical(@RequestBody Map<String, Object> body) {
        Long reportId = Long.valueOf(String.valueOf(body.get("reportId")));
        examService.handleCritical(reportId, (String) body.get("remark"));
        return Result.ok();
    }

    /** 项目下拉（distinct 项目名） */
    @GetMapping("/items")
    @PreAuthorize("hasAuthority('exam:list')")
    public Result<List<String>> items() {
        return Result.ok(applyMapper.selectList(Wrappers.<BizExamApply>lambdaQuery()
                        .select(BizExamApply::getExamItem))
                .stream().map(BizExamApply::getExamItem).distinct().collect(Collectors.toList()));
    }
}
