package com.medical.modules.health.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.framework.LoginUser;
import com.medical.framework.SecurityUtils;
import com.medical.modules.health.entity.BizHealthCheckup;
import com.medical.modules.health.entity.BizHealthRecord;
import com.medical.modules.health.entity.BizInfectiousReport;
import com.medical.modules.health.entity.BizVaccination;
import com.medical.modules.health.mapper.BizHealthCheckupMapper;
import com.medical.modules.health.mapper.BizHealthRecordMapper;
import com.medical.modules.health.mapper.BizInfectiousReportMapper;
import com.medical.modules.health.mapper.BizVaccinationMapper;
import com.medical.modules.patient.entity.BizPatient;
import com.medical.modules.patient.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthService {

    /** 接种提醒窗口（文档 §10.2④） */
    public static final int REMIND_DAYS = 7;

    private final BizHealthRecordMapper recordMapper;
    private final BizVaccinationMapper vaccinationMapper;
    private final BizInfectiousReportMapper infectiousMapper;
    private final BizHealthCheckupMapper checkupMapper;
    private final PatientMapper patientMapper;

    // ---------------- 健康档案 ----------------

    public PageResult<Map<String, Object>> recordPage(PageQuery query, String chronicDisease) {
        List<Long> patientIds = null;
        if (StringUtils.hasText(query.getKeyword())) {
            // 姓名经患者表下推 id 集（无匹配直接空页）
            patientIds = patientMapper.selectList(Wrappers.<BizPatient>lambdaQuery()
                            .like(BizPatient::getName, query.getKeyword()))
                    .stream().map(BizPatient::getId).toList();
            if (patientIds.isEmpty()) {
                return PageResult.of(0, query.getPageNum(), query.getPageSize(), List.of());
            }
        }
        Page<BizHealthRecord> page = recordMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<BizHealthRecord>lambdaQuery()
                        .in(patientIds != null, BizHealthRecord::getPatientId, patientIds)
                        .apply(StringUtils.hasText(chronicDisease), "FIND_IN_SET({0}, chronic_disease)", chronicDisease)
                        .orderByDesc(BizHealthRecord::getId));
        if (page.getRecords().isEmpty()) {
            return PageResult.of(0, page.getCurrent(), page.getSize(), List.of());
        }
        Map<Long, BizPatient> patients = patientMapper.selectList(Wrappers.<BizPatient>lambdaQuery()
                        .in(BizPatient::getId, page.getRecords().stream()
                                .map(BizHealthRecord::getPatientId).distinct().toList()))
                .stream().collect(Collectors.toMap(BizPatient::getId, p -> p));
        List<Map<String, Object>> rows = page.getRecords().stream().map(r -> {
            BizPatient p = patients.get(r.getPatientId());
            return Map.<String, Object>of(
                    "id", r.getId(),
                    "recordNo", r.getRecordNo(),
                    "patientId", r.getPatientId(),
                    "patientName", p == null ? "" : p.getName(),
                    "gender", p == null ? 0 : p.getGender(),
                    "chronicDisease", r.getChronicDisease() == null ? "" : r.getChronicDisease(),
                    "smoking", r.getSmoking(),
                    "drinking", r.getDrinking(),
                    "exercise", r.getExercise() == null ? "" : r.getExercise(),
                    "createTime", r.getCreateTime() == null ? "" : r.getCreateTime().toString());
        }).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }

    /**
     * 新建档案：一人一档（Service 校验，不加唯一键）
     */
    @Transactional
    public Long createRecord(BizHealthRecord record) {
        if (record.getPatientId() == null) {
            throw new BusinessException("患者不能为空");
        }
        Long dup = recordMapper.selectCount(Wrappers.<BizHealthRecord>lambdaQuery()
                .eq(BizHealthRecord::getPatientId, record.getPatientId()));
        if (dup > 0) {
            throw new BusinessException("该患者已建立健康档案");
        }
        recordMapper.insert(record);
        BizHealthRecord patch = new BizHealthRecord();
        patch.setId(record.getId());
        patch.setRecordNo(String.format("H%05d", record.getId()));
        recordMapper.updateById(patch);
        return record.getId();
    }

    @Transactional
    public void updateRecord(Long id, BizHealthRecord patch) {
        if (recordMapper.selectById(id) == null) {
            throw new BusinessException("档案不存在");
        }
        patch.setId(id);
        patch.setPatientId(null);
        recordMapper.updateById(patch);
    }

    // ---------------- 疫苗接种 ----------------

    public PageResult<BizVaccination> vaccinationPage(PageQuery query, Long patientId) {
        if (patientId == null) {
            throw new BusinessException("患者ID必传");
        }
        Page<BizVaccination> page = vaccinationMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<BizVaccination>lambdaQuery()
                        .eq(BizVaccination::getPatientId, patientId)
                        .orderByDesc(BizVaccination::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    @Transactional
    public Long addVaccination(BizVaccination vaccination) {
        if (vaccination.getPatientId() == null || !StringUtils.hasText(vaccination.getVaccineName())) {
            throw new BusinessException("患者与疫苗名称必填");
        }
        LoginUser operator = SecurityUtils.getCurrentUser();
        vaccination.setOperatorId(operator.getId());
        vaccinationMapper.insert(vaccination);
        return vaccination.getId();
    }

    /** 接种提醒：next_date ∈ [今日, 今日+7] 升序 */
    public List<Map<String, Object>> remindList() {
        LocalDate today = LocalDate.now();
        List<BizVaccination> list = vaccinationMapper.selectList(Wrappers.<BizVaccination>lambdaQuery()
                .ge(BizVaccination::getNextDate, today)
                .le(BizVaccination::getNextDate, today.plusDays(REMIND_DAYS))
                .orderByAsc(BizVaccination::getNextDate));
        Map<Long, BizPatient> patients = list.isEmpty() ? Map.of()
                : patientMapper.selectList(Wrappers.<BizPatient>lambdaQuery()
                        .in(BizPatient::getId, list.stream().map(BizVaccination::getPatientId).distinct().toList()))
                .stream().collect(Collectors.toMap(BizPatient::getId, p -> p));
        return list.stream().map(v -> Map.<String, Object>of(
                "id", v.getId(),
                "patientName", patients.containsKey(v.getPatientId()) ? patients.get(v.getPatientId()).getName() : "",
                "vaccineName", v.getVaccineName(),
                "doseNo", v.getDoseNo(),
                "nextDate", v.getNextDate() == null ? "" : v.getNextDate().toString())).toList();
    }

    // ---------------- 传染病上报 ----------------

    /** 上报即直写 report_status=1（文档 §10.2③） */
    @Transactional
    public Long reportInfectious(BizInfectiousReport report) {
        if (report.getPatientId() == null || !StringUtils.hasText(report.getDiseaseName())) {
            throw new BusinessException("患者与病名必填");
        }
        LoginUser reporter = SecurityUtils.getCurrentUser();
        report.setReporterId(reporter.getId());
        report.setReportDate(LocalDate.now());
        report.setReportStatus(1);
        infectiousMapper.insert(report);
        return report.getId();
    }

    public PageResult<Map<String, Object>> infectiousPage(PageQuery query) {
        Page<BizInfectiousReport> page = infectiousMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<BizInfectiousReport>lambdaQuery().orderByDesc(BizInfectiousReport::getId));
        if (page.getRecords().isEmpty()) {
            return PageResult.of(0, page.getCurrent(), page.getSize(), List.of());
        }
        Map<Long, BizPatient> patients = patientMapper.selectList(Wrappers.<BizPatient>lambdaQuery()
                        .in(BizPatient::getId, page.getRecords().stream()
                                .map(BizInfectiousReport::getPatientId).distinct().toList()))
                .stream().collect(Collectors.toMap(BizPatient::getId, p -> p));
        List<Map<String, Object>> rows = page.getRecords().stream().map(r -> Map.<String, Object>of(
                "id", r.getId(),
                "patientName", patients.containsKey(r.getPatientId()) ? patients.get(r.getPatientId()).getName() : "",
                "diseaseName", r.getDiseaseName(),
                "icdCode", r.getIcdCode() == null ? "" : r.getIcdCode(),
                "reportDate", r.getReportDate() == null ? "" : r.getReportDate().toString(),
                "reportStatus", r.getReportStatus(),
                "cdcResponse", r.getCdcResponse() == null ? "" : r.getCdcResponse())).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }

    // ---------------- 体检 ----------------

    public PageResult<Map<String, Object>> checkupPage(PageQuery query) {
        Page<BizHealthCheckup> page = checkupMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<BizHealthCheckup>lambdaQuery().orderByDesc(BizHealthCheckup::getId));
        if (page.getRecords().isEmpty()) {
            return PageResult.of(0, page.getCurrent(), page.getSize(), List.of());
        }
        Map<Long, BizPatient> patients = patientMapper.selectList(Wrappers.<BizPatient>lambdaQuery()
                        .in(BizPatient::getId, page.getRecords().stream()
                                .map(BizHealthCheckup::getPatientId).distinct().toList()))
                .stream().collect(Collectors.toMap(BizPatient::getId, p -> p));
        List<Map<String, Object>> rows = page.getRecords().stream().map(c -> Map.<String, Object>of(
                "id", c.getId(),
                "checkupNo", c.getCheckupNo(),
                "patientName", patients.containsKey(c.getPatientId()) ? patients.get(c.getPatientId()).getName() : "",
                "checkupDate", c.getCheckupDate() == null ? "" : c.getCheckupDate().toString(),
                "checkupType", c.getCheckupType() == null ? "" : c.getCheckupType(),
                "examItems", c.getExamItems() == null ? "" : c.getExamItems(),
                "conclusion", c.getConclusion() == null ? "" : c.getConclusion())).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }

    @Transactional
    public Long addCheckup(BizHealthCheckup checkup) {
        if (checkup.getPatientId() == null) {
            throw new BusinessException("患者不能为空");
        }
        LoginUser user = SecurityUtils.getCurrentUser();
        checkup.setDoctorId(user.getId());
        if (checkup.getCheckupDate() == null) {
            checkup.setCheckupDate(LocalDate.now());
        }
        checkupMapper.insert(checkup);
        BizHealthCheckup patch = new BizHealthCheckup();
        patch.setId(checkup.getId());
        patch.setCheckupNo(String.format("TJ%05d", checkup.getId()));
        checkupMapper.updateById(patch);
        return checkup.getId();
    }
}
