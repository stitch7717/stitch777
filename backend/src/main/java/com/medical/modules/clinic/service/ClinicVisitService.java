package com.medical.modules.clinic.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.common.BusinessException;
import com.medical.framework.LoginUser;
import com.medical.framework.SecurityUtils;
import com.medical.modules.clinic.dto.RecordRequest;
import com.medical.modules.clinic.dto.VisitVO;
import com.medical.modules.clinic.entity.BizMedicalRecord;
import com.medical.modules.clinic.mapper.MedicalRecordMapper;
import com.medical.modules.patient.entity.BizPatient;
import com.medical.modules.patient.mapper.PatientMapper;
import com.medical.modules.registration.entity.BizRegistration;
import com.medical.modules.registration.mapper.RegistrationMapper;
import com.medical.modules.system.entity.SysDept;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysDeptMapper;
import com.medical.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClinicVisitService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RegistrationMapper registrationMapper;
    private final MedicalRecordMapper recordMapper;
    private final PatientMapper patientMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    /**
     * 就诊列表：当日挂号（含已完成可回看补开），医生默认 SELF，admin/dean 全量
     */
    public List<VisitVO> todayVisits(Integer status, Long doctorId) {
        LoginUser current = SecurityUtils.getCurrentUser();
        Long scopeDoctorId = "admin".equals(current.getRoleCode()) || "dean".equals(current.getRoleCode())
                ? doctorId : current.getId();

        List<BizRegistration> regs = registrationMapper.selectList(Wrappers.<BizRegistration>lambdaQuery()
                .apply("DATE(reg_time) = {0}", LocalDate.now())
                .eq(scopeDoctorId != null, BizRegistration::getDoctorId, scopeDoctorId)
                .eq(status != null, BizRegistration::getStatus, status)
                .orderByAsc(BizRegistration::getQueueNo));
        // 当日无挂号（或该医生无号）直接返回，避免空集合 IN () SQL 错误
        if (regs.isEmpty()) {
            return List.of();
        }

        Map<Long, BizPatient> patients = patientMapper.selectList(Wrappers.<BizPatient>lambdaQuery()
                        .in(BizPatient::getId,
                                regs.stream().map(BizRegistration::getPatientId).distinct().toList()))
                .stream().collect(Collectors.toMap(BizPatient::getId, p -> p));
        Map<Long, String> deptNames = deptMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));

        return regs.stream().map(r -> {
            VisitVO vo = new VisitVO();
            vo.setId(r.getId());
            vo.setRegNo(r.getRegNo());
            vo.setPatientId(r.getPatientId());
            vo.setPatientName(r.getPatientName());
            BizPatient p = patients.get(r.getPatientId());
            if (p != null) {
                vo.setGender(p.getGender());
                vo.setAge(p.getBirthDate() == null
                        ? null : Period.between(p.getBirthDate(), LocalDate.now()).getYears());
                vo.setAllergyHistory(p.getAllergyHistory());
            }
            vo.setDeptId(r.getDeptId());
            vo.setDeptName(deptNames.get(r.getDeptId()));
            vo.setDoctorId(r.getDoctorId());
            vo.setDoctorName(userName(r.getDoctorId()));
            vo.setQueueNo(r.getQueueNo());
            vo.setRegType(r.getRegType());
            vo.setStatus(r.getStatus());
            vo.setRegTime(r.getRegTime() == null ? null : r.getRegTime().format(FMT));
            return vo;
        }).toList();
    }

    /**
     * 接诊：0待就诊 → 1就诊中；已叫号/已接诊幂等返回
     */
    @Transactional
    public void receive(Long registrationId) {
        BizRegistration reg = registrationMapper.selectById(registrationId);
        if (reg == null) {
            throw new BusinessException("就诊记录不存在");
        }
        if (reg.getStatus() == BizRegistration.ST_IN_CONSULT) {
            return;
        }
        if (reg.getStatus() != BizRegistration.ST_WAITING) {
            throw new BusinessException("仅候诊状态可接诊");
        }
        int updated = registrationMapper.update(null, Wrappers.<BizRegistration>lambdaUpdate()
                .set(BizRegistration::getStatus, BizRegistration.ST_IN_CONSULT)
                .eq(BizRegistration::getId, registrationId)
                .eq(BizRegistration::getStatus, BizRegistration.ST_WAITING));
        if (updated == 0) {
            throw new BusinessException("接诊失败，请刷新重试");
        }
    }

    /**
     * 病历保存：按 visit_no upsert；提交时联动就诊完成（2已完成）
     */
    @Transactional
    public Long saveRecord(RecordRequest request) {
        BizRegistration reg = registrationMapper.selectOne(Wrappers.<BizRegistration>lambdaQuery()
                .eq(BizRegistration::getRegNo, request.getVisitNo()));
        if (reg == null) {
            throw new BusinessException("就诊记录不存在");
        }
        LoginUser doctor = SecurityUtils.getCurrentUser();

        BizMedicalRecord record = recordMapper.selectOne(Wrappers.<BizMedicalRecord>lambdaQuery()
                .eq(BizMedicalRecord::getVisitNo, request.getVisitNo()));
        boolean isSubmit = Boolean.TRUE.equals(request.getSubmit());
        if (record == null) {
            record = new BizMedicalRecord();
            record.setPatientId(reg.getPatientId());
            record.setVisitNo(request.getVisitNo());
            record.setVisitType(1);
            record.setDoctorId(doctor.getId());
            record.setVisitTime(LocalDateTime.now());
        } else {
            if (record.getStatus() == BizMedicalRecord.ST_SUBMITTED) {
                throw new BusinessException("病历已提交，不可修改");
            }
        }
        record.setChiefComplaint(request.getChiefComplaint());
        record.setPresentIllness(request.getPresentIllness());
        record.setPhysicalExam(request.getPhysicalExam());
        record.setDiagnosis(request.getDiagnosis());
        record.setIcdCode(request.getIcdCode());
        record.setStatus(isSubmit ? BizMedicalRecord.ST_SUBMITTED : BizMedicalRecord.ST_DRAFT);
        if (record.getId() == null) {
            recordMapper.insert(record);
        } else {
            recordMapper.updateById(record);
        }

        if (isSubmit && reg.getStatus() == BizRegistration.ST_IN_CONSULT) {
            registrationMapper.update(null, Wrappers.<BizRegistration>lambdaUpdate()
                    .set(BizRegistration::getStatus, BizRegistration.ST_FINISHED)
                    .eq(BizRegistration::getId, reg.getId())
                    .eq(BizRegistration::getStatus, BizRegistration.ST_IN_CONSULT));
        }
        return record.getId();
    }

    /**
     * 按就诊流水号取病历（回显）
     */
    public BizMedicalRecord recordByVisitNo(String visitNo) {
        return recordMapper.selectOne(Wrappers.<BizMedicalRecord>lambdaQuery()
                .eq(BizMedicalRecord::getVisitNo, visitNo));
    }

    private String userName(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            return null;
        }
        return StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername();
    }
}
