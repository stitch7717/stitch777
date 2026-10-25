package com.medical.modules.finance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.framework.LoginUser;
import com.medical.framework.SecurityUtils;
import com.medical.modules.clinic.entity.BizPrescription;
import com.medical.modules.clinic.entity.BizPrescriptionItem;
import com.medical.modules.clinic.mapper.PrescriptionItemMapper;
import com.medical.modules.clinic.mapper.PrescriptionMapper;
import com.medical.modules.finance.dto.ChargeRequest;
import com.medical.modules.finance.entity.BizCharge;
import com.medical.modules.finance.entity.BizChargeItem;
import com.medical.modules.finance.mapper.ChargeItemMapper;
import com.medical.modules.finance.mapper.ChargeMapper;
import com.medical.modules.finance.dto.ChargeVO;
import com.medical.modules.registration.entity.BizRegistration;
import com.medical.modules.registration.mapper.RegistrationMapper;
import lombok.RequiredArgsConstructor;
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
public class ChargeService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final com.medical.modules.message.service.MessageService messageService;
    private final com.medical.modules.finance.mapper.BizInvoiceMapper invoiceMapper;

    private final ChargeMapper chargeMapper;
    private final ChargeItemMapper itemMapper;
    private final RegistrationMapper registrationMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;

    /**
     * 待收费列表：当日未退号且有待收项（已审核处方未收费，或挂号费未收）的就诊
     */
    public List<Map<String, Object>> pendingList() {
        List<BizRegistration> regs = registrationMapper.selectList(Wrappers.<BizRegistration>lambdaQuery()
                .apply("DATE(reg_time) = {0}", LocalDate.now())
                .ne(BizRegistration::getStatus, BizRegistration.ST_CANCELLED)
                .orderByDesc(BizRegistration::getId));

        return regs.stream().filter(r -> hasPending(r.getId(), r)).map(r -> Map.<String, Object>of(
                "visitId", r.getId(),
                "regNo", r.getRegNo(),
                "patientName", r.getPatientName(),
                "status", r.getStatus(),
                "regTime", r.getRegTime() == null ? "" : r.getRegTime().format(FMT))).toList();
    }

    private boolean hasPending(Long visitId, BizRegistration reg) {
        if (!existsPaid(reg)) {
            return true;
        }
        Long cnt = prescriptionMapper.selectCount(Wrappers.<BizPrescription>lambdaQuery()
                .eq(BizPrescription::getVisitId, visitId)
                .eq(BizPrescription::getStatus, BizPrescription.ST_AUDITED));
        return cnt > 0;
    }

    /** 该就诊是否已有有效已支付收费单（已退费不算已收，可重收挂号费） */
    private boolean existsPaid(BizRegistration reg) {
        return chargeMapper.selectCount(Wrappers.<BizCharge>lambdaQuery()
                .eq(BizCharge::getVisitId, reg.getId())
                .eq(BizCharge::getPayStatus, 1)) > 0;
    }

    /**
     * 待收费明细：挂号费（首单含，item_type=2、source_id 空）+ 已审核处方明细
     * 金额服务端按快照重算
     */
    public Map<String, Object> pendingDetail(Long visitId) {
        BizRegistration reg = registrationMapper.selectById(visitId);
        if (reg == null) {
            throw new BusinessException("就诊不存在");
        }
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        if (!existsPaid(reg)) {
            items.add(Map.of(
                    "itemType", BizChargeItem.TYPE_TREAT,
                    "itemName", "挂号费（" + typeName(reg.getRegType()) + "）",
                    "sourceId", "",
                    "quantity", 1,
                    "price", reg.getRegFee(),
                    "amount", reg.getRegFee()));
        }
        List<BizPrescription> prescs = prescriptionMapper.selectList(Wrappers.<BizPrescription>lambdaQuery()
                .eq(BizPrescription::getVisitId, visitId)
                .eq(BizPrescription::getStatus, BizPrescription.ST_AUDITED));
        for (BizPrescription p : prescs) {
            items.add(Map.of(
                    "itemType", BizChargeItem.TYPE_DRUG,
                    "itemName", "处方 " + p.getPrescNo(),
                    "sourceId", p.getId(),
                    "quantity", 1,
                    "price", p.getTotalAmount(),
                    "amount", p.getTotalAmount()));
        }
        BigDecimal total = items.stream()
                .map(i -> (BigDecimal) i.get("amount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("visitId", visitId, "patientName", reg.getPatientName(), "items", items, "totalAmount", total);
    }

    /**
     * 整单收费：事务首步行锁就诊，处方 1→2 原子迁移命中 0 行回滚，金额服务端重算
     */
    @Transactional
    public Map<String, Object> charge(ChargeRequest request) {
        BizRegistration reg = registrationMapper.selectById(request.getVisitId());
        if (reg == null) {
            throw new BusinessException("就诊不存在");
        }
        LoginUser operator = SecurityUtils.getCurrentUser();

        List<Map<String, Object>> items = pendingDetail(request.getVisitId())
                .get("items") == null ? List.of() : (List<Map<String, Object>>) pendingDetail(request.getVisitId()).get("items");
        if (items.isEmpty()) {
            throw new BusinessException("该就诊没有待收项目");
        }
        BigDecimal total = items.stream()
                .map(i -> (BigDecimal) i.get("amount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BizCharge charge = new BizCharge();
        charge.setPatientId(reg.getPatientId());
        charge.setPatientName(reg.getPatientName());
        charge.setVisitId(reg.getId());
        charge.setChargeType(BizCharge.TYPE_CLINIC);
        charge.setTotalAmount(total);
        charge.setDiscountAmount(BigDecimal.ZERO);
        charge.setActualAmount(total);
        charge.setPayType(request.getPayType());
        charge.setPayStatus(1);
        charge.setOperatorId(operator.getId());
        charge.setChargeTime(LocalDateTime.now());
        chargeMapper.insert(charge);

        String chargeNo = "CH" + LocalDate.now().format(NO_FMT) + String.format("%04d", charge.getId());
        BizCharge patch = new BizCharge();
        patch.setId(charge.getId());
        patch.setChargeNo(chargeNo);
        chargeMapper.updateById(patch);

        for (Map<String, Object> item : items) {
            BizChargeItem ci = new BizChargeItem();
            ci.setChargeId(charge.getId());
            ci.setItemType((Integer) item.get("itemType"));
            ci.setItemName((String) item.get("itemName"));
            Object sid = item.get("sourceId");
            ci.setSourceId(sid == null || "".equals(sid) ? null : Long.valueOf(sid.toString()));
            ci.setQuantity((Integer) item.get("quantity"));
            ci.setPrice((BigDecimal) item.get("price"));
            ci.setAmount((BigDecimal) item.get("amount"));
            itemMapper.insert(ci);

            // 处方收费 1已审核 → 2已收费（原子迁移，失败整单回滚）
            if (ci.getSourceId() != null) {
                int updated = prescriptionMapper.update(null, Wrappers.<BizPrescription>lambdaUpdate()
                        .set(BizPrescription::getStatus, BizPrescription.ST_CHARGED)
                        .eq(BizPrescription::getId, ci.getSourceId())
                        .eq(BizPrescription::getStatus, BizPrescription.ST_AUDITED));
                if (updated == 0) {
                    throw new BusinessException("处方状态已变更，收费失败：" + ci.getItemName());
                }
            }
        }
        notifyOperator(operator.getId(), chargeNo, reg.getPatientName(), total);
        return Map.of("chargeNo", chargeNo, "actualAmount", total);
    }

    private void notifyOperator(Long operatorId, String chargeNo, String patientName, BigDecimal amount) {
        try {
            messageService.notifyChargePaid(operatorId, chargeNo, patientName, amount.toPlainString());
        } catch (Exception ignored) {
            // 通知失败不阻断收费
        }
    }

    /**
     * 退费：pay_status 1→2 条件仲裁；处方 2→4 终态（防回退 1 循环）；挂号费不回退号
     */
    @Transactional
    public void refund(Long chargeId) {
        BizCharge charge = chargeMapper.selectById(chargeId);
        if (charge == null) {
            throw new BusinessException("收费单不存在");
        }
        if (charge.getPayStatus() != 1) {
            throw new BusinessException("该收费单不是已支付状态");
        }
        LoginUser operator = SecurityUtils.getCurrentUser();
        int updated = chargeMapper.update(null, Wrappers.<BizCharge>lambdaUpdate()
                .set(BizCharge::getPayStatus, 2)
                .set(BizCharge::getRefundTime, LocalDateTime.now())
                .set(BizCharge::getRefundOperatorId, operator.getId())
                .eq(BizCharge::getId, chargeId)
                .eq(BizCharge::getPayStatus, 1));
        if (updated == 0) {
            throw new BusinessException("退费失败，请刷新重试");
        }
        // 有效发票自动作废（退费后可重开）
        invoiceMapper.update(null, Wrappers.<com.medical.modules.finance.entity.BizInvoice>lambdaUpdate()
                .set(com.medical.modules.finance.entity.BizInvoice::getStatus, 1)
                .eq(com.medical.modules.finance.entity.BizInvoice::getChargeId, chargeId)
                .eq(com.medical.modules.finance.entity.BizInvoice::getStatus, 0));
        // 处方退费 2→4 终态
        List<Long> prescIds = itemMapper.selectList(Wrappers.<BizChargeItem>lambdaQuery()
                        .eq(BizChargeItem::getChargeId, chargeId).isNotNull(BizChargeItem::getSourceId))
                .stream().map(BizChargeItem::getSourceId).distinct().toList();
        for (Long pid : prescIds) {
            prescriptionMapper.update(null, Wrappers.<BizPrescription>lambdaUpdate()
                    .set(BizPrescription::getStatus, BizPrescription.ST_RETURNED)
                    .eq(BizPrescription::getId, pid)
                    .eq(BizPrescription::getStatus, BizPrescription.ST_CHARGED));
        }
    }

    /**
     * 收费记录分页（含明细）
     */
    public PageResult<ChargeVO> page(PageQuery query, Integer payStatus) {
        Page<BizCharge> page = chargeMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<BizCharge>lambdaQuery()
                        .eq(payStatus != null, BizCharge::getPayStatus, payStatus)
                        .orderByDesc(BizCharge::getId));
        List<ChargeVO> rows = page.getRecords().stream().map(c -> {
            ChargeVO vo = new ChargeVO();
            vo.setId(c.getId());
            vo.setChargeNo(c.getChargeNo());
            vo.setPatientName(c.getPatientName());
            vo.setChargeType(c.getChargeType());
            vo.setTotalAmount(c.getTotalAmount());
            vo.setActualAmount(c.getActualAmount());
            vo.setPayType(c.getPayType());
            vo.setPayStatus(c.getPayStatus());
            vo.setChargeTime(c.getChargeTime() == null ? null : c.getChargeTime().format(FMT));
            vo.setItems(itemMapper.selectList(Wrappers.<BizChargeItem>lambdaQuery()
                    .eq(BizChargeItem::getChargeId, c.getId())));
            return vo;
        }).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }

    /**
     * 日结：收款 = charge_time 在当日且 pay_status∈{1,2}；退款 = refund_time 在当日
     */
    public Map<String, Object> daily(LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        List<BizCharge> list = chargeMapper.selectList(Wrappers.<BizCharge>lambdaQuery()
                .apply("DATE(charge_time) = {0}", day));
        BigDecimal income = list.stream()
                .filter(c -> c.getPayStatus() == 1 || c.getPayStatus() == 2)
                .map(BizCharge::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refund = list.stream()
                .filter(c -> c.getPayStatus() == 2)
                .map(BizCharge::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<Integer, BigDecimal> byPayType = list.stream()
                .filter(c -> c.getPayStatus() == 1)
                .collect(Collectors.groupingBy(BizCharge::getPayType,
                        Collectors.reducing(BigDecimal.ZERO, BizCharge::getActualAmount, BigDecimal::add)));
        return Map.of(
                "date", day.toString(),
                "chargeCount", list.size(),
                "income", income,
                "refund", refund,
                "byPayType", byPayType);
    }

    /**
     * 收费单详情（含明细）
     */
    public ChargeVO detail(Long id) {
        BizCharge c = chargeMapper.selectById(id);
        if (c == null) {
            throw new BusinessException("收费单不存在");
        }
        ChargeVO vo = new ChargeVO();
        vo.setId(c.getId());
        vo.setChargeNo(c.getChargeNo());
        vo.setPatientName(c.getPatientName());
        vo.setChargeType(c.getChargeType());
        vo.setTotalAmount(c.getTotalAmount());
        vo.setActualAmount(c.getActualAmount());
        vo.setPayType(c.getPayType());
        vo.setPayStatus(c.getPayStatus());
        vo.setChargeTime(c.getChargeTime() == null ? null : c.getChargeTime().format(FMT));
        vo.setItems(itemMapper.selectList(Wrappers.<BizChargeItem>lambdaQuery()
                .eq(BizChargeItem::getChargeId, c.getId())));
        return vo;
    }

    private String typeName(Integer regType) {
        return switch (regType == null ? 0 : regType) {
            case 1 -> "普通";
            case 2 -> "专家";
            case 3 -> "急诊";
            default -> "";
        };
    }
}
