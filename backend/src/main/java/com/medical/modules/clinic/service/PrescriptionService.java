package com.medical.modules.clinic.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.framework.LoginUser;
import com.medical.framework.SecurityUtils;
import com.medical.modules.clinic.dto.PrescriptionRequest;
import com.medical.modules.clinic.dto.PrescriptionVO;
import com.medical.modules.clinic.entity.BizDrug;
import com.medical.modules.clinic.entity.BizPrescription;
import com.medical.modules.clinic.entity.BizPrescriptionItem;
import com.medical.modules.clinic.mapper.DrugMapper;
import com.medical.modules.clinic.mapper.PrescriptionItemMapper;
import com.medical.modules.clinic.mapper.PrescriptionMapper;
import com.medical.modules.registration.entity.BizRegistration;
import com.medical.modules.registration.mapper.RegistrationMapper;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper itemMapper;
    private final DrugMapper drugMapper;
    private final RegistrationMapper registrationMapper;
    private final SysUserMapper userMapper;

    /**
     * 开处方：金额由服务端按药品当刻售价快照计算（不收客户端金额，防篡改）
     */
    @Transactional
    public Map<String, Object> create(PrescriptionRequest request) {
        BizRegistration reg = registrationMapper.selectById(request.getVisitId());
        if (reg == null) {
            throw new BusinessException("就诊记录不存在");
        }
        if (reg.getStatus() != BizRegistration.ST_IN_CONSULT && reg.getStatus() != BizRegistration.ST_FINISHED) {
            throw new BusinessException("仅接诊后可开处方（提交病历后仍可补开）");
        }
        LoginUser doctor = SecurityUtils.getCurrentUser();

        // 服务端取售价快照并计算金额
        List<Long> drugIds = request.getItems().stream().map(PrescriptionRequest.Item::getDrugId).distinct().toList();
        Map<Long, BizDrug> drugs = drugMapper.selectList(Wrappers.<BizDrug>lambdaQuery()
                        .in(BizDrug::getId, drugIds).eq(BizDrug::getStatus, 1))
                .stream().collect(Collectors.toMap(BizDrug::getId, Function.identity()));
        for (PrescriptionRequest.Item item : request.getItems()) {
            if (!drugs.containsKey(item.getDrugId())) {
                throw new BusinessException("药品不存在或已停用：drugId=" + item.getDrugId());
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("药品数量必须大于 0");
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        BizPrescription presc = new BizPrescription();
        presc.setPatientId(reg.getPatientId());
        presc.setDoctorId(doctor.getId());
        presc.setVisitId(request.getVisitId());
        presc.setPrescType(request.getPrescType() == null ? 1 : request.getPrescType());
        presc.setDiagnosis(request.getDiagnosis());
        prescriptionMapper.insert(presc);

        for (PrescriptionRequest.Item item : request.getItems()) {
            BizDrug drug = drugs.get(item.getDrugId());
            BigDecimal price = drug.getSalePrice();
            BigDecimal amount = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(amount);

            BizPrescriptionItem pi = new BizPrescriptionItem();
            pi.setPrescId(presc.getId());
            pi.setDrugId(drug.getId());
            pi.setDrugName(drug.getDrugName());
            pi.setSpec(drug.getSpec());
            pi.setUsage(item.getUsage());
            pi.setDosage(item.getDosage());
            pi.setFrequency(item.getFrequency());
            pi.setDays(item.getDays());
            pi.setQuantity(item.getQuantity());
            pi.setPrice(price);
            pi.setAmount(amount);
            itemMapper.insert(pi);
        }

        String prescNo = "PR" + LocalDate.now().format(DATE_FMT) + String.format("%04d", presc.getId());
        BizPrescription patch = new BizPrescription();
        patch.setId(presc.getId());
        patch.setPrescNo(prescNo);
        patch.setTotalAmount(total);
        prescriptionMapper.updateById(patch);

        return Map.of("prescNo", prescNo, "totalAmount", total);
    }

    /**
     * 处方分页：随页返回 items（一次批量填充）
     */
    public PageResult<PrescriptionVO> page(PageQuery query, Long visitId, Integer status) {
        LoginUser current = SecurityUtils.getCurrentUser();
        Page<BizPrescription> page = prescriptionMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<BizPrescription>lambdaQuery()
                        .eq(visitId != null, BizPrescription::getVisitId, visitId)
                        .eq(status != null, BizPrescription::getStatus, status)
                        .orderByDesc(BizPrescription::getId));
        List<BizPrescription> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(0, page.getCurrent(), page.getSize(), List.of());
        }

        List<Long> prescIds = records.stream().map(BizPrescription::getId).toList();
        Map<Long, List<BizPrescriptionItem>> itemsByPresc = itemMapper.selectList(
                        Wrappers.<BizPrescriptionItem>lambdaQuery().in(BizPrescriptionItem::getPrescId, prescIds))
                .stream().collect(Collectors.groupingBy(BizPrescriptionItem::getPrescId));

        List<BizRegistration> regs = registrationMapper.selectList(Wrappers.<BizRegistration>lambdaQuery()
                .in(BizRegistration::getId, records.stream().map(BizPrescription::getVisitId).distinct().toList()));
        Map<Long, String> patientNames = regs.stream()
                .collect(Collectors.toMap(BizRegistration::getId, BizRegistration::getPatientName, (a, b) -> a));

        List<PrescriptionVO> rows = records.stream().map(p -> {
            PrescriptionVO vo = new PrescriptionVO();
            vo.setId(p.getId());
            vo.setPrescNo(p.getPrescNo());
            vo.setVisitId(p.getVisitId());
            vo.setPatientName(patientNames.get(p.getVisitId()));
            vo.setDoctorName(userName(p.getDoctorId()));
            vo.setPrescType(p.getPrescType());
            vo.setDiagnosis(p.getDiagnosis());
            vo.setTotalAmount(p.getTotalAmount());
            vo.setStatus(p.getStatus());
            vo.setCreateTime(p.getCreateTime() == null ? null : p.getCreateTime().format(FMT));
            vo.setItems(itemsByPresc.getOrDefault(p.getId(), List.of()));
            return vo;
        }).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }

    /**
     * 审核：0待审核 → 1已审核（条件更新仲裁）
     */
    @Transactional
    public void audit(Long id) {
        int updated = prescriptionMapper.update(null, Wrappers.<BizPrescription>lambdaUpdate()
                .set(BizPrescription::getStatus, BizPrescription.ST_AUDITED)
                .eq(BizPrescription::getId, id)
                .eq(BizPrescription::getStatus, BizPrescription.ST_PENDING));
        if (updated == 0) {
            throw new BusinessException(409, "审核失败：处方不是待审核状态");
        }
    }

    /**
     * 作废：仅待审核可作废（逻辑删除），已审核走退药/退费链（后续阶段）
     */
    @Transactional
    public void voidPrescription(Long id) {
        BizPrescription presc = prescriptionMapper.selectById(id);
        if (presc == null) {
            throw new BusinessException("处方不存在");
        }
        if (presc.getStatus() != BizPrescription.ST_PENDING) {
            throw new BusinessException(409, "仅待审核处方可作废");
        }
        prescriptionMapper.deleteById(id);
    }

    private String userName(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            return null;
        }
        return u.getRealName() != null ? u.getRealName() : u.getUsername();
    }
}
