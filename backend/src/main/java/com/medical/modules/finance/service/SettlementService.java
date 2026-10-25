package com.medical.modules.finance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.common.BusinessException;
import com.medical.modules.inpatient.entity.BizMedicalOrder;
import com.medical.modules.clinic.mapper.DrugMapper;
import com.medical.modules.inpatient.entity.BizAdmission;
import com.medical.modules.inpatient.entity.BizBed;
import com.medical.modules.inpatient.mapper.BizAdmissionMapper;
import com.medical.modules.inpatient.mapper.BizBedMapper;
import com.medical.modules.inpatient.mapper.BizMedicalOrderMapper;
import com.medical.modules.finance.entity.BizCharge;
import com.medical.modules.finance.entity.BizChargeItem;
import com.medical.modules.finance.mapper.ChargeItemMapper;
import com.medical.modules.finance.mapper.ChargeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final BizAdmissionMapper admissionMapper;
    private final BizBedMapper bedMapper;
    private final BizMedicalOrderMapper orderMapper;
    private final DrugMapper drugMapper;
    private final ChargeMapper chargeMapper;
    private final ChargeItemMapper itemMapper;
    private final com.medical.modules.message.service.MessageService messageService;

    /**
     * 住院费用预演：床位费（天数向上取整）+ 执行药品医嘱汇总
     */
    public Map<String, Object> preview(Long admissionId) {
        BizAdmission adm = require(admissionId);
        BigDecimal bedFee = bedFee(adm);
        BigDecimal drugFee = drugFee(admissionId);
        BigDecimal total = bedFee.add(drugFee);
        BigDecimal deposit = adm.getDeposit() == null ? BigDecimal.ZERO : adm.getDeposit();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("admissionNo", adm.getAdmissionNo());
        result.put("patientName", adm.getPatientName());
        result.put("bedFee", bedFee);
        result.put("drugFee", drugFee);
        result.put("total", total);
        result.put("deposit", deposit);
        result.put("payable", total.max(BigDecimal.ZERO));
        result.put("refundable", deposit.subtract(total).max(BigDecimal.ZERO));
        return result;
    }

    /**
     * 出院结算：生成 charge_type=3 收费单（pay_status=1 视为押金抵扣完成），
     * 联动出院（释放床位）。押金多退少补仅做金额展示，不做实际资金流。
     */
    @Transactional
    public Map<String, Object> settle(Long admissionId) {
        BizAdmission adm = require(admissionId);
        if (adm.getStatus() != BizAdmission.ST_IN) {
            throw new BusinessException("仅住院中患者可结算");
        }
        BigDecimal bedFee = bedFee(adm);
        BigDecimal drugFee = drugFee(admissionId);
        BigDecimal total = bedFee.add(drugFee);

        BizCharge charge = new BizCharge();
        charge.setPatientId(adm.getPatientId());
        charge.setPatientName(adm.getPatientName());
        charge.setVisitId(adm.getId());
        charge.setChargeType(BizCharge.TYPE_INPATIENT);
        charge.setTotalAmount(total);
        charge.setDiscountAmount(BigDecimal.ZERO);
        charge.setActualAmount(total);
        charge.setPayType(1);
        charge.setPayStatus(1);
        charge.setOperatorId(com.medical.framework.SecurityUtils.getCurrentUser().getId());
        charge.setChargeTime(LocalDateTime.now());
        chargeMapper.insert(charge);
        String no = "CH" + LocalDate.now().format(NO_FMT) + String.format("%04d", charge.getId());
        BizCharge patch = new BizCharge();
        patch.setId(charge.getId());
        patch.setChargeNo(no);
        chargeMapper.updateById(patch);

        BizChargeItem bed = new BizChargeItem();
        bed.setChargeId(charge.getId());
        bed.setItemType(BizChargeItem.TYPE_BED);
        bed.setItemName("床位费");
        bed.setQuantity(1);
        bed.setPrice(bedFee);
        bed.setAmount(bedFee);
        itemMapper.insert(bed);
        if (drugFee.compareTo(BigDecimal.ZERO) > 0) {
            BizChargeItem drug = new BizChargeItem();
            drug.setChargeId(charge.getId());
            drug.setItemType(BizChargeItem.TYPE_DRUG);
            drug.setItemName("住院药品医嘱费");
            drug.setQuantity(1);
            drug.setPrice(drugFee);
            drug.setAmount(drugFee);
            itemMapper.insert(drug);
        }

        // 办理出院（复用出院逻辑）
        int updated = admissionMapper.update(null, Wrappers.<BizAdmission>lambdaUpdate()
                .set(BizAdmission::getStatus, BizAdmission.ST_DISCHARGED)
                .set(BizAdmission::getDischargeTime, LocalDateTime.now())
                .eq(BizAdmission::getId, admissionId)
                .eq(BizAdmission::getStatus, BizAdmission.ST_IN));
        if (updated == 0) throw new BusinessException("出院失败，请刷新重试");
        bedMapper.update(null, Wrappers.<BizBed>lambdaUpdate()
                .set(BizBed::getStatus, BizBed.ST_FREE)
                .eq(BizBed::getId, adm.getBedId())
                .eq(BizBed::getStatus, BizBed.ST_OCCUPIED));

        try {
            messageService.notifyChargePaid(charge.getOperatorId(), no,
                    adm.getPatientName(), total.toPlainString());
        } catch (Exception ignored) {
        }
        return Map.of("chargeNo", no, "total", total,
                "deposit", adm.getDeposit() == null ? BigDecimal.ZERO : adm.getDeposit(),
                "refundable", adm.getDeposit() == null ? BigDecimal.ZERO
                        : adm.getDeposit().subtract(total).max(BigDecimal.ZERO));
    }

    private BizAdmission require(Long id) {
        BizAdmission adm = admissionMapper.selectById(id);
        if (adm == null) throw new BusinessException("住院记录不存在");
        return adm;
    }

    private BigDecimal bedFee(BizAdmission adm) {
        BizBed bed = bedMapper.selectById(adm.getBedId());
        if (bed == null) return BigDecimal.ZERO;
        LocalDateTime end = adm.getDischargeTime() != null ? adm.getDischargeTime() : LocalDateTime.now();
        long hours = Math.max(1, ChronoUnit.HOURS.between(adm.getAdmissionTime(), end));
        long days = (hours + 23) / 24;
        return bed.getPrice().multiply(BigDecimal.valueOf(days));
    }

    private BigDecimal drugFee(Long admissionId) {
        List<BizMedicalOrder> orders = orderMapper.selectList(Wrappers.<BizMedicalOrder>lambdaQuery()
                .eq(BizMedicalOrder::getAdmissionId, admissionId)
                .isNotNull(BizMedicalOrder::getDrugId));
        BigDecimal fee = BigDecimal.ZERO;
        for (BizMedicalOrder o : orders) {
            var d = drugMapper.selectById(o.getDrugId());
            if (d != null) fee = fee.add(d.getSalePrice());
        }
        return fee;
    }
}
