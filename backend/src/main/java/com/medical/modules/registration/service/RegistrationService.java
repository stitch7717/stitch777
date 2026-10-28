package com.medical.modules.registration.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.common.BusinessException;
import com.medical.framework.LoginUser;
import com.medical.framework.SecurityUtils;
import com.medical.modules.registration.dto.RegisterRequest;
import com.medical.modules.patient.entity.BizPatient;
import com.medical.modules.patient.mapper.PatientMapper;
import com.medical.modules.registration.entity.BizRegistration;
import com.medical.modules.registration.entity.BizSchedule;
import com.medical.modules.registration.mapper.QueueSeqMapper;
import com.medical.modules.registration.mapper.RegistrationMapper;
import com.medical.modules.registration.mapper.ScheduleMapper;
import com.medical.modules.system.entity.SysDept;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysDeptMapper;
import com.medical.modules.system.mapper.SysUserMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RegistrationMapper registrationMapper;
    private final ScheduleMapper scheduleMapper;
    private final QueueSeqMapper queueSeqMapper;
    private final PatientMapper patientMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    /**
     * 挂号：排班状态校验 + DB 原子扣号源（remain_num>0 AND status=1，防超卖）
     * + queueNo 当日科室序号（含退号不回收）+ regNo 先插后回补 + 姓名快照
     */
    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        BizPatient patient = patientMapper.selectById(request.getPatientId());
        if (patient == null) {
            throw new BusinessException("患者不存在");
        }
        BizSchedule schedule = scheduleMapper.selectById(request.getScheduleId());
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }
        if (schedule.getStatus() != 1) {
            throw new BusinessException("该排班已停诊");
        }

        // 条件内原子扣减，命中 0 行即号源耗尽或停诊
        int deducted = scheduleMapper.update(null, Wrappers.<BizSchedule>lambdaUpdate()
                .setSql("remain_num = remain_num - 1")
                .eq(BizSchedule::getId, schedule.getId())
                .eq(BizSchedule::getStatus, 1)
                .gt(BizSchedule::getRemainNum, 0));
        if (deducted == 0) {
            throw new BusinessException("号源已满或排班已停诊");
        }

        LoginUser operator = SecurityUtils.getCurrentUser();
        BizRegistration reg = new BizRegistration();
        reg.setPatientId(patient.getId());
        reg.setPatientName(patient.getName());
        reg.setDoctorId(schedule.getDoctorId());
        reg.setDeptId(schedule.getDeptId());
        reg.setScheduleId(schedule.getId());
        reg.setRegType(schedule.getRegType());
        reg.setRegFee(schedule.getRegFee());
        reg.setQueueNo(nextQueueNo(schedule.getDeptId(), LocalDate.now()));
        reg.setStatus(BizRegistration.ST_WAITING);
        reg.setRegTime(LocalDateTime.now());
        reg.setCreatorId(operator.getId());
        try {
            registrationMapper.insert(reg);
        } catch (DuplicateKeyException e) {
            // 唯一键 uk_patient_schedule_active 兜底并发重复挂号；事务回滚会同时归还号源
            throw new BusinessException(409, "该患者在该排班已有有效挂号，请勿重复挂号");
        }

        String regNo = "REG" + LocalDate.now().format(DATE_FMT) + String.format("%04d", reg.getId());
        BizRegistration patch = new BizRegistration();
        patch.setId(reg.getId());
        patch.setRegNo(regNo);
        registrationMapper.updateById(patch);

        reg.setRegNo(regNo);
        return Map.of(
                "regNo", regNo,
                "queueNo", reg.getQueueNo(),
                "regFee", reg.getRegFee(),
                "patientName", reg.getPatientName());
    }

    /**
     * 退号：0待就诊 → 3已退号（条件更新仲裁），号源回补（排班正常时）
     */
    @Transactional
    public void cancel(Long id) {
        BizRegistration reg = registrationMapper.selectById(id);
        if (reg == null) {
            throw new BusinessException("挂号记录不存在");
        }
        if (reg.getStatus() != BizRegistration.ST_WAITING) {
            throw new BusinessException("仅待就诊状态可退号");
        }
        int updated = registrationMapper.update(null, Wrappers.<BizRegistration>lambdaUpdate()
                .set(BizRegistration::getStatus, BizRegistration.ST_CANCELLED)
                .eq(BizRegistration::getId, id)
                .eq(BizRegistration::getStatus, BizRegistration.ST_WAITING));
        if (updated == 0) {
            throw new BusinessException(409, "退号失败，挂号状态已变更，请刷新后重试");
        }
        scheduleMapper.update(null, Wrappers.<BizSchedule>lambdaUpdate()
                .setSql("remain_num = remain_num + 1")
                .eq(BizSchedule::getId, reg.getScheduleId())
                .eq(BizSchedule::getStatus, 1));
    }

    /**
     * 候诊队列：当日该科室 待就诊/就诊中 按排队号升序
     */
    public List<QueueVO> queue(Long deptId) {
        List<BizRegistration> list = registrationMapper.selectList(Wrappers.<BizRegistration>lambdaQuery()
                .eq(BizRegistration::getDeptId, deptId)
                .apply("DATE(reg_time) = {0}", LocalDate.now())
                .in(BizRegistration::getStatus, BizRegistration.ST_WAITING, BizRegistration.ST_IN_CONSULT)
                .orderByAsc(BizRegistration::getQueueNo));
        return list.stream().map(r -> {
            QueueVO vo = new QueueVO();
            vo.setId(r.getId());
            vo.setQueueNo(r.getQueueNo());
            vo.setPatientName(r.getPatientName());
            vo.setRegNo(r.getRegNo());
            vo.setRegType(r.getRegType());
            vo.setDoctorName(doctorName(r.getDoctorId()));
            vo.setStatus(r.getStatus());
            vo.setRegTime(r.getRegTime() == null ? null : r.getRegTime().format(TIME_FMT));
            return vo;
        }).toList();
    }

    /**
     * 叫号：0待就诊 → 1就诊中（条件更新仲裁）
     */
    @Transactional
    public void call(Long id) {
        int updated = registrationMapper.update(null, Wrappers.<BizRegistration>lambdaUpdate()
                .set(BizRegistration::getStatus, BizRegistration.ST_IN_CONSULT)
                .eq(BizRegistration::getId, id)
                .eq(BizRegistration::getStatus, BizRegistration.ST_WAITING));
        if (updated == 0) {
            throw new BusinessException(409, "叫号失败：该号已叫或已退");
        }
    }

    /**
     * 就诊完成：门诊阶段提交病历时联动，此处提供基础推进
     */
    @Transactional
    public void finish(Long id) {
        int updated = registrationMapper.update(null, Wrappers.<BizRegistration>lambdaUpdate()
                .set(BizRegistration::getStatus, BizRegistration.ST_FINISHED)
                .eq(BizRegistration::getId, id)
                .eq(BizRegistration::getStatus, BizRegistration.ST_IN_CONSULT));
        if (updated == 0) {
            throw new BusinessException(409, "仅就诊中状态可完成");
        }
    }

    /**
     * 挂号记录分页（patientId 可选）
     */
    public Object page(long pageNum, long pageSize, Long patientId, Integer status) {
        var page = registrationMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize),
                Wrappers.<BizRegistration>lambdaQuery()
                        .eq(patientId != null, BizRegistration::getPatientId, patientId)
                        .eq(status != null, BizRegistration::getStatus, status)
                        .orderByDesc(BizRegistration::getId));
        return com.medical.common.PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
                page.getRecords().stream().map(r -> {
                    QueueVO vo = new QueueVO();
                    vo.setId(r.getId());
                    vo.setQueueNo(r.getQueueNo());
                    vo.setPatientName(r.getPatientName());
                    vo.setRegNo(r.getRegNo());
                    vo.setRegType(r.getRegType());
                    vo.setDoctorName(doctorName(r.getDoctorId()));
                    vo.setStatus(r.getStatus());
                    vo.setRegTime(r.getRegTime() == null ? null : r.getRegTime().format(TIME_FMT));
                    return vo;
                }).toList());
    }

    /**
     * 排队号：按「科室 + 日期」在计数器表内原子自增。
     * INSERT ... ON DUPLICATE KEY UPDATE 会持有行锁到事务结束，
     * 并发挂号在同一科室/日期上串行取号，不再出现 MAX()+1 的重复。
     */
    private Integer nextQueueNo(Long deptId, LocalDate date) {
        queueSeqMapper.bump(deptId, date);
        return queueSeqMapper.current(deptId, date);
    }

    private String doctorName(Long doctorId) {
        SysUser doctor = userMapper.selectById(doctorId);
        if (doctor == null) {
            return null;
        }
        return doctor.getRealName() != null ? doctor.getRealName() : doctor.getUsername();
    }

    @Data
    public static class QueueVO {
        private Long id;
        private Integer queueNo;
        private String patientName;
        private String regNo;
        private Integer regType;
        private String doctorName;
        private Integer status;
        private String regTime;
    }
}
