package com.medical.modules.inpatient.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.common.BusinessException;
import com.medical.modules.inpatient.dto.AdmissionDetailVO;
import com.medical.modules.inpatient.dto.AdmissionRequest;
import com.medical.modules.inpatient.dto.OrderVO;
import com.medical.modules.inpatient.entity.BizAdmission;
import com.medical.modules.inpatient.entity.BizBed;
import com.medical.modules.inpatient.entity.BizMedicalOrder;
import com.medical.modules.inpatient.mapper.BizAdmissionMapper;
import com.medical.modules.inpatient.mapper.BizBedMapper;
import com.medical.modules.inpatient.mapper.BizMedicalOrderMapper;
import com.medical.modules.patient.entity.BizPatient;
import com.medical.modules.patient.mapper.PatientMapper;
import com.medical.modules.system.entity.SysDept;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysDeptMapper;
import com.medical.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdmissionService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BizAdmissionMapper admissionMapper;
    private final BizBedMapper bedMapper;
    private final BizMedicalOrderMapper orderMapper;
    private final PatientMapper patientMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    /**
     * 入院：床位条件占用仲裁（防并发抢床）+ 在院查重（同一患者不可重复入院）
     */
    @Transactional
    public Map<String, Object> admit(AdmissionRequest request) {
        BizPatient patient = patientMapper.selectById(request.getPatientId());
        if (patient == null) {
            throw new BusinessException("患者不存在");
        }
        Long inCount = admissionMapper.selectCount(Wrappers.<BizAdmission>lambdaQuery()
                .eq(BizAdmission::getPatientId, request.getPatientId())
                .eq(BizAdmission::getStatus, BizAdmission.ST_IN));
        if (inCount > 0) {
            throw new BusinessException("该患者已在院，不可重复入院");
        }
        BizBed bed = bedMapper.selectById(request.getBedId());
        if (bed == null) {
            throw new BusinessException("床位不存在");
        }
        if (bed.getStatus() != BizBed.ST_FREE) {
            throw new BusinessException("床位已占用或维修中");
        }
        // 条件占用仲裁：命中 0 行即被并发抢床
        int occupied = bedMapper.update(null, Wrappers.<BizBed>lambdaUpdate()
                .set(BizBed::getStatus, BizBed.ST_OCCUPIED)
                .eq(BizBed::getId, request.getBedId())
                .eq(BizBed::getStatus, BizBed.ST_FREE));
        if (occupied == 0) {
            throw new BusinessException("床位已被占用，请刷新重选");
        }

        BizAdmission adm = new BizAdmission();
        adm.setPatientId(patient.getId());
        adm.setPatientName(patient.getName());
        adm.setDeptId(request.getDeptId());
        adm.setBedId(request.getBedId());
        adm.setDoctorId(request.getDoctorId());
        adm.setNurseId(request.getNurseId());
        adm.setDiagnosis(request.getDiagnosis());
        adm.setDeposit(request.getDeposit() == null ? BigDecimal.ZERO : request.getDeposit());
        adm.setStatus(BizAdmission.ST_IN);
        adm.setAdmissionTime(LocalDateTime.now());
        admissionMapper.insert(adm);

        String no = "ZY" + LocalDateTime.now().format(NO_FMT) + String.format("%04d", adm.getId());
        BizAdmission patch = new BizAdmission();
        patch.setId(adm.getId());
        patch.setAdmissionNo(no);
        admissionMapper.updateById(patch);

        return Map.of("id", adm.getId(), "admissionNo", no);
    }

    /**
     * 出院：0在院→1出院，释放床位
     */
    @Transactional
    public void discharge(Long id) {
        BizAdmission adm = require(id);
        if (adm.getStatus() != BizAdmission.ST_IN) {
            throw new BusinessException("仅住院中患者可办理出院");
        }
        int updated = admissionMapper.update(null, Wrappers.<BizAdmission>lambdaUpdate()
                .set(BizAdmission::getStatus, BizAdmission.ST_DISCHARGED)
                .set(BizAdmission::getDischargeTime, LocalDateTime.now())
                .eq(BizAdmission::getId, id)
                .eq(BizAdmission::getStatus, BizAdmission.ST_IN));
        if (updated == 0) {
            throw new BusinessException("出院办理失败，请刷新重试");
        }
        bedMapper.update(null, Wrappers.<BizBed>lambdaUpdate()
                .set(BizBed::getStatus, BizBed.ST_FREE)
                .eq(BizBed::getId, adm.getBedId())
                .eq(BizBed::getStatus, BizBed.ST_OCCUPIED));
    }

    /**
     * 转科：原记录终结（0→2，discharge_time=转出时刻）+ 新建在院记录（新床位）
     */
    @Transactional
    public Map<String, Object> transfer(Long id, Long newDeptId, Long newBedId, Long newDoctorId) {
        BizAdmission old = require(id);
        if (old.getStatus() != BizAdmission.ST_IN) {
            throw new BusinessException("仅住院中患者可转科");
        }
        if (old.getDeptId().equals(newDeptId)) {
            throw new BusinessException("目标病区与当前病区相同");
        }
        BizBed bed = bedMapper.selectById(newBedId);
        if (bed == null || !bed.getWardId().equals(newDeptId)) {
            throw new BusinessException("目标床位不属于目标病区");
        }
        int moved = bedMapper.update(null, Wrappers.<BizBed>lambdaUpdate()
                .set(BizBed::getStatus, BizBed.ST_OCCUPIED)
                .eq(BizBed::getId, newBedId)
                .eq(BizBed::getStatus, BizBed.ST_FREE));
        if (moved == 0) {
            throw new BusinessException("目标床位已被占用，请刷新重选");
        }
        // 原记录终结
        int ended = admissionMapper.update(null, Wrappers.<BizAdmission>lambdaUpdate()
                .set(BizAdmission::getStatus, BizAdmission.ST_TRANSFERRED)
                .set(BizAdmission::getDischargeTime, LocalDateTime.now())
                .eq(BizAdmission::getId, id)
                .eq(BizAdmission::getStatus, BizAdmission.ST_IN));
        if (ended == 0) {
            throw new BusinessException("转科失败，请刷新重试");
        }
        bedMapper.update(null, Wrappers.<BizBed>lambdaUpdate()
                .set(BizBed::getStatus, BizBed.ST_FREE)
                .eq(BizBed::getId, old.getBedId())
                .eq(BizBed::getStatus, BizBed.ST_OCCUPIED));
        // 新建在院记录（诊断/押金延续）
        BizAdmission adm = new BizAdmission();
        adm.setPatientId(old.getPatientId());
        adm.setPatientName(old.getPatientName());
        adm.setDeptId(newDeptId);
        adm.setBedId(newBedId);
        adm.setDoctorId(newDoctorId != null ? newDoctorId : old.getDoctorId());
        adm.setNurseId(old.getNurseId());
        adm.setDiagnosis(old.getDiagnosis());
        adm.setDeposit(old.getDeposit());
        adm.setStatus(BizAdmission.ST_IN);
        adm.setAdmissionTime(LocalDateTime.now());
        admissionMapper.insert(adm);
        String no = "ZY" + LocalDateTime.now().format(NO_FMT) + String.format("%04d", adm.getId());
        BizAdmission patch = new BizAdmission();
        patch.setId(adm.getId());
        patch.setAdmissionNo(no);
        admissionMapper.updateById(patch);
        return Map.of("id", adm.getId(), "admissionNo", no);
    }

    /**
     * 住院详情（含费用清单预演：押金 + 床位费估算 + 医嘱药品清单，不做结算）
     */
    public AdmissionDetailVO detail(Long id) {
        BizAdmission adm = require(id);
        AdmissionDetailVO vo = new AdmissionDetailVO();
        vo.setId(adm.getId());
        vo.setAdmissionNo(adm.getAdmissionNo());
        vo.setPatientId(adm.getPatientId());
        vo.setPatientName(adm.getPatientName());
        vo.setDeptId(adm.getDeptId());
        SysDept dept = deptMapper.selectById(adm.getDeptId());
        vo.setDeptName(dept == null ? null : dept.getDeptName());
        BizBed bed = bedMapper.selectById(adm.getBedId());
        vo.setBedNo(bed == null ? null : bed.getBedNo());
        vo.setDoctorName(userName(adm.getDoctorId()));
        vo.setNurseName(userName(adm.getNurseId()));
        vo.setDiagnosis(adm.getDiagnosis());
        vo.setDeposit(adm.getDeposit());
        vo.setStatus(adm.getStatus());
        vo.setAdmissionTime(adm.getAdmissionTime() == null ? null : adm.getAdmissionTime().format(FMT));
        vo.setDischargeTime(adm.getDischargeTime() == null ? null : adm.getDischargeTime().format(FMT));

        // 在院天数（向上取整 ≥1）
        LocalDateTime end = adm.getDischargeTime() != null ? adm.getDischargeTime() : LocalDateTime.now();
        long days = Math.max(1, ChronoUnit.HOURS.between(adm.getAdmissionTime(), end) / 24
                + (ChronoUnit.MINUTES.between(adm.getAdmissionTime(), end) % 1440 > 0 ? 1 : 0));
        vo.setInDays(days);
        if (bed != null) {
            vo.setDepositPart(adm.getDeposit());
            vo.setBedFeeEstimate(bed.getPrice().multiply(BigDecimal.valueOf(days)));
        }
        vo.setOrders(listOrders(id));
        return vo;
    }

    /**
     * 医嘱列表
     */
    public List<OrderVO> listOrders(Long admissionId) {
        List<BizMedicalOrder> orders = orderMapper.selectList(Wrappers.<BizMedicalOrder>lambdaQuery()
                .eq(BizMedicalOrder::getAdmissionId, admissionId)
                .orderByDesc(BizMedicalOrder::getId));
        List<Long> drugIds = orders.stream().map(BizMedicalOrder::getDrugId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, com.medical.modules.clinic.entity.BizDrug> drugs = drugIds.isEmpty() ? Map.of()
                : drugOrderMap(drugIds);
        return orders.stream().map(o -> {
            OrderVO vo = new OrderVO();
            vo.setId(o.getId());
            vo.setOrderType(o.getOrderType());
            vo.setContent(o.getContent());
            if (o.getDrugId() != null) {
                var d = drugs.get(o.getDrugId());
                if (d != null) {
                    vo.setDrugName(d.getDrugName());
                    vo.setDrugPrice(d.getSalePrice());
                }
            }
            vo.setDosage(o.getDosage());
            vo.setUsageMethod(o.getUsageMethod());
            vo.setFrequency(o.getFrequency());
            vo.setStartTime(o.getStartTime());
            vo.setStopTime(o.getStopTime());
            vo.setDoctorName(userName(o.getDoctorId()));
            vo.setStatus(o.getStatus());
            return vo;
        }).toList();
    }

    private Map<Long, com.medical.modules.clinic.entity.BizDrug> drugOrderMap(List<Long> ids) {
        return drugMapper.selectList(Wrappers.<com.medical.modules.clinic.entity.BizDrug>lambdaQuery()
                        .in(com.medical.modules.clinic.entity.BizDrug::getId, ids))
                .stream().collect(Collectors.toMap(
                        com.medical.modules.clinic.entity.BizDrug::getId,
                        d -> d, (a, b) -> a));
    }

    private final com.medical.modules.clinic.mapper.DrugMapper drugMapper;

    private String userName(Long id) {
        if (id == null) return null;
        SysUser u = userMapper.selectById(id);
        if (u == null) return null;
        return StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername();
    }

    private BizAdmission require(Long id) {
        BizAdmission adm = admissionMapper.selectById(id);
        if (adm == null) {
            throw new BusinessException("住院记录不存在");
        }
        return adm;
    }
}
