package com.medical.modules.exam.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.framework.LoginUser;
import com.medical.framework.SecurityUtils;
import com.medical.modules.exam.dto.ApplyRequest;
import com.medical.modules.exam.dto.ApplyVO;
import com.medical.modules.exam.dto.ReportRequest;
import com.medical.modules.exam.entity.BizExamApply;
import com.medical.modules.exam.entity.BizExamReport;
import com.medical.modules.exam.mapper.BizExamApplyMapper;
import com.medical.modules.exam.mapper.BizExamReportMapper;
import com.medical.modules.patient.entity.BizPatient;
import com.medical.modules.patient.mapper.PatientMapper;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BizExamApplyMapper applyMapper;
    private final BizExamReportMapper reportMapper;
    private final PatientMapper patientMapper;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;

    /**
     * 开检查申请（门诊开单同用），先插后回补 EXAM+日期+序号
     */
    @Transactional
    public Map<String, Object> apply(ApplyRequest request) {
        BizPatient patient = patientMapper.selectById(request.getPatientId());
        if (patient == null) {
            throw new BusinessException("患者不存在");
        }
        LoginUser doctor = SecurityUtils.getCurrentUser();
        BizExamApply apply = new BizExamApply();
        apply.setRegistrationId(request.getRegistrationId());
        apply.setPatientId(request.getPatientId());
        apply.setDoctorId(doctor.getId());
        apply.setExamType(request.getExamType() == null ? 1 : request.getExamType());
        apply.setExamItem(request.getExamItem());
        apply.setBodyPart(request.getBodyPart());
        apply.setClinicalDiagnosis(request.getClinicalDiagnosis());
        apply.setFee(request.getFee());
        apply.setStatus(BizExamApply.ST_UNPAID);
        apply.setApplyTime(LocalDateTime.now());
        applyMapper.insert(apply);

        String no = "EXAM" + LocalDate.now().format(NO_FMT) + String.format("%04d", apply.getId());
        BizExamApply patch = new BizExamApply();
        patch.setId(apply.getId());
        patch.setApplyNo(no);
        applyMapper.updateById(patch);
        return Map.of("id", apply.getId(), "applyNo", no);
    }

    /**
     * 申请分页（registrationId/status/examItem/keyword 可选；数据范围：doctor SELF，admin/dean ALL）
     */
    public PageResult<ApplyVO> page(PageQuery query, Long registrationId, Integer status,
                                    String examItem, Long doctorId) {
        LoginUser current = SecurityUtils.getCurrentUser();
        Long scope = "admin".equals(current.getRoleCode()) || "dean".equals(current.getRoleCode())
                ? doctorId : current.getId();

        Page<BizExamApply> page = applyMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<BizExamApply>lambdaQuery()
                        .eq(registrationId != null, BizExamApply::getRegistrationId, registrationId)
                        .eq(status != null, BizExamApply::getStatus, status)
                        .like(StringUtils.hasText(examItem), BizExamApply::getExamItem, examItem)
                        .eq(scope != null, BizExamApply::getDoctorId, scope)
                        .orderByDesc(BizExamApply::getId));
        if (page.getRecords().isEmpty()) {
            return PageResult.of(0, page.getCurrent(), page.getSize(), List.of());
        }

        Map<Long, BizPatient> patients = patientMapper.selectList(Wrappers.<BizPatient>lambdaQuery()
                        .in(BizPatient::getId, page.getRecords().stream()
                                .map(BizExamApply::getPatientId).distinct().toList()))
                .stream().collect(Collectors.toMap(BizPatient::getId, p -> p));
        Map<Long, BizExamReport> reports = reportMapper.selectList(Wrappers.<BizExamReport>lambdaQuery()
                        .in(BizExamReport::getApplyId, page.getRecords().stream()
                                .map(BizExamApply::getId).distinct().toList()))
                .stream().collect(Collectors.toMap(BizExamReport::getApplyId, r -> r, (a, b) -> a));

        List<ApplyVO> rows = page.getRecords().stream().map(a -> {
            ApplyVO vo = new ApplyVO();
            vo.setId(a.getId());
            vo.setApplyNo(a.getApplyNo());
            vo.setRegistrationId(a.getRegistrationId());
            vo.setPatientId(a.getPatientId());
            BizPatient p = patients.get(a.getPatientId());
            vo.setPatientName(p == null ? null : p.getName());
            vo.setDoctorName(userName(a.getDoctorId()));
            vo.setExamType(a.getExamType());
            vo.setExamItem(a.getExamItem());
            vo.setBodyPart(a.getBodyPart());
            vo.setClinicalDiagnosis(a.getClinicalDiagnosis());
            vo.setFee(a.getFee());
            vo.setStatus(a.getStatus());
            vo.setApplyTime(a.getApplyTime() == null ? null : a.getApplyTime().format(FMT));
            BizExamReport r = reports.get(a.getId());
            if (r != null) {
                vo.setReportId(r.getId());
                vo.setIsCritical(r.getIsCritical());
                vo.setCriticalHandled(r.getCriticalHandled());
                vo.setReportStatus(r.getStatus());
                vo.setAuditorId(r.getAuditorId());
            }
            return vo;
        }).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }

    /**
     * 样本采集：1待采样→2检验中（原子迁移）
     */
    @Transactional
    public void sample(Long applyId) {
        int updated = applyMapper.update(null, Wrappers.<BizExamApply>lambdaUpdate()
                .set(BizExamApply::getStatus, BizExamApply.ST_TESTING)
                .eq(BizExamApply::getId, applyId)
                .eq(BizExamApply::getStatus, BizExamApply.ST_SAMPLE_PENDING));
        if (updated == 0) {
            throw new BusinessException("仅待采样状态可采集");
        }
    }

    /**
     * 录入结果：草稿 upsert（一申请一报告 uk_apply）；已发布不可改
     */
    @Transactional
    public Long saveReport(ReportRequest request) {
        BizExamApply apply = applyMapper.selectById(request.getApplyId());
        if (apply == null) {
            throw new BusinessException("检查申请不存在");
        }
        if (apply.getStatus() != BizExamApply.ST_TESTING) {
            throw new BusinessException("仅检验中状态可录入结果");
        }
        LoginUser reporter = SecurityUtils.getCurrentUser();

        BizExamReport exist = reportMapper.selectOne(Wrappers.<BizExamReport>lambdaQuery()
                .eq(BizExamReport::getApplyId, request.getApplyId()));
        BizExamReport report = exist == null ? new BizExamReport() : exist;
        if (exist != null && exist.getStatus() == BizExamReport.ST_PUBLISHED) {
            throw new BusinessException("报告已发布，不可修改");
        }
        report.setApplyId(request.getApplyId());
        report.setResult(toJson(request.getItems()));
        report.setConclusion(request.getConclusion());
        report.setIsCritical(request.getIsCritical() != null && request.getIsCritical() == 1 ? 1 : 0);
        report.setAttachmentUrl(request.getAttachmentUrl());
        report.setReporterId(reporter.getId());
        if (report.getId() == null) {
            reportMapper.insert(report);
        } else {
            reportMapper.updateById(report);
        }

        if (report.getReportNo() == null) {
            String no = "RP" + LocalDate.now().format(NO_FMT) + String.format("%04d", report.getId());
            BizExamReport patch = new BizExamReport();
            patch.setId(report.getId());
            patch.setReportNo(no);
            reportMapper.updateById(patch);
        }
        return report.getId();
    }

    /**
     * 审核：写 auditor_id（状态仍 0 草稿）
     */
    @Transactional
    public void audit(Long reportId) {
        BizExamReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        if (report.getStatus() == BizExamReport.ST_PUBLISHED) {
            throw new BusinessException("报告已发布，不可重复审核");
        }
        if (report.getAuditorId() != null) {
            throw new BusinessException("报告已审核过");
        }
        LoginUser auditor = SecurityUtils.getCurrentUser();
        if (auditor.getId().equals(report.getReporterId())) {
            // 自审允许与否：课程级允许（双人复核为生产口径），不阻断
        }
        BizExamReport patch = new BizExamReport();
        patch.setId(reportId);
        patch.setAuditorId(auditor.getId());
        reportMapper.updateById(patch);
    }

    /**
     * 发布：报告 0→1 并联动检查单 2→3（原子迁移，锁序一致防死锁）
     */
    @Transactional
    public void publish(Long reportId) {
        BizExamReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        if (report.getStatus() == BizExamReport.ST_PUBLISHED) {
            throw new BusinessException("报告已发布");
        }
        if (report.getAuditorId() == null) {
            throw new BusinessException("报告未审核，不可发布");
        }
        // 先迁移申请单状态（与录入保存的锁序一致：先锁申请行）
        int moved = applyMapper.update(null, Wrappers.<BizExamApply>lambdaUpdate()
                .set(BizExamApply::getStatus, BizExamApply.ST_REPORTED)
                .eq(BizExamApply::getId, report.getApplyId())
                .eq(BizExamApply::getStatus, BizExamApply.ST_TESTING));
        if (moved == 0) {
            throw new BusinessException("检查单状态已变更，发布失败");
        }
        int published = reportMapper.update(null, Wrappers.<BizExamReport>lambdaUpdate()
                .set(BizExamReport::getStatus, BizExamReport.ST_PUBLISHED)
                .set(BizExamReport::getReportTime, LocalDateTime.now())
                .eq(BizExamReport::getId, reportId)
                .eq(BizExamReport::getStatus, BizExamReport.ST_DRAFT));
        if (published == 0) {
            throw new BusinessException("发布失败，请刷新重试");
        }
    }

    /**
     * 危急值处理：发布后的报告，写处理人/时间/备注（重复处理 400）
     */
    @Transactional
    public void handleCritical(Long reportId, String remark) {
        BizExamReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        if (report.getIsCritical() == null || report.getIsCritical() != 1) {
            throw new BusinessException("该报告非危急值");
        }
        if (report.getCriticalHandled() != null && report.getCriticalHandled() == 1) {
            throw new BusinessException("危急值已处理");
        }
        LoginUser handler = SecurityUtils.getCurrentUser();
        BizExamReport patch = new BizExamReport();
        patch.setId(reportId);
        patch.setCriticalHandled(1);
        patch.setCriticalHandleRemark(remark);
        patch.setCriticalHandleTime(LocalDateTime.now());
        patch.setCriticalHandlerId(handler.getId());
        reportMapper.updateById(patch);
    }

    /**
     * 报告详情（明细反序列化 + 患者聚合）
     */
    public Map<String, Object> reportDetail(Long reportId) {
        BizExamReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        BizExamApply apply = applyMapper.selectById(report.getApplyId());
        BizPatient patient = apply == null ? null : patientMapper.selectById(apply.getPatientId());
        return Map.of(
                "report", report,
                "items", fromJson(report.getResult()),
                "applyNo", apply == null ? "" : apply.getApplyNo(),
                "examItem", apply == null ? "" : apply.getExamItem(),
                "patientName", patient == null ? "" : patient.getName(),
                "reporterName", userName(report.getReporterId()),
                "auditorName", userName(report.getAuditorId()));
    }

    private String toJson(List<ReportRequest.ResultItem> items) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            throw new BusinessException("明细序列化失败");
        }
    }

    private List<Object> fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Object.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private String userName(Long id) {
        if (id == null) return null;
        SysUser u = userMapper.selectById(id);
        if (u == null) return null;
        return StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername();
    }
}
