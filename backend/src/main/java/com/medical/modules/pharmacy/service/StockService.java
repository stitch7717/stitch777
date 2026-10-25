package com.medical.modules.pharmacy.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.common.BusinessException;
import com.medical.framework.LoginUser;
import com.medical.framework.SecurityUtils;
import com.medical.modules.clinic.entity.BizDrug;
import com.medical.modules.clinic.mapper.DrugMapper;
import com.medical.modules.clinic.entity.BizPrescription;
import com.medical.modules.clinic.entity.BizPrescriptionItem;
import com.medical.modules.clinic.mapper.PrescriptionItemMapper;
import com.medical.modules.clinic.mapper.PrescriptionMapper;
import com.medical.modules.pharmacy.dto.StockRequest;
import com.medical.modules.pharmacy.entity.BizStock;
import com.medical.modules.pharmacy.entity.BizStockRecord;
import com.medical.modules.pharmacy.mapper.StockMapper;
import com.medical.modules.pharmacy.mapper.StockRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockMapper stockMapper;
    private final StockRecordMapper recordMapper;
    private final DrugMapper drugMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper itemMapper;

    /**
     * 入库：库存行 upsert（非空批号/效期才覆盖），原子累加数量，写入库流水
     */
    @Transactional
    public void stockIn(StockRequest request) {
        requireDrug(request.getDrugId());
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException("入库数量必须大于 0");
        }
        BizStock stock = stockMapper.selectOne(Wrappers.<BizStock>lambdaQuery()
                .eq(BizStock::getDrugId, request.getDrugId()));
        if (stock == null) {
            throw new BusinessException("库存行不存在");
        }
        int updated = stockMapper.update(null, Wrappers.<BizStock>lambdaUpdate()
                .setSql("quantity = quantity + " + request.getQuantity())
                .eq(BizStock::getId, stock.getId()));
        if (updated == 0) {
            throw new BusinessException("入库失败");
        }
        // 批号/生产日期/有效期非空才覆盖（空值保留旧值）；批号仅入流水
        BizStock patch = new BizStock();
        patch.setId(stock.getId());
        if (request.getProduceDate() != null) {
            patch.setProduceDate(request.getProduceDate());
        }
        if (request.getExpireDate() != null) {
            patch.setExpireDate(request.getExpireDate());
        }
        if (patch.getProduceDate() != null || patch.getExpireDate() != null) {
            stockMapper.updateById(patch);
        }
        saveRecord(request.getDrugId(), BizStockRecord.TYPE_IN, request.getQuantity(),
                request.getBatchNo(), request.getRemark());
    }

    /**
     * 出库：quantity >= n 条件内扣，防负库存
     */
    @Transactional
    public void stockOut(StockRequest request) {
        requireDrug(request.getDrugId());
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException("出库数量必须大于 0");
        }
        int updated = stockMapper.update(null, Wrappers.<BizStock>lambdaUpdate()
                .setSql("quantity = quantity - " + request.getQuantity())
                .eq(BizStock::getDrugId, request.getDrugId())
                .apply("quantity >= {0}", request.getQuantity()));
        if (updated == 0) {
            throw new BusinessException("库存不足");
        }
        saveRecord(request.getDrugId(), BizStockRecord.TYPE_OUT, request.getQuantity(),
                request.getBatchNo(), request.getRemark());
    }

    /**
     * 发药：整单发放，1已审核/2已收费 → 3已发药；
     * 同药多明细按 drugId 汇总一批扣减，任一不足整单失败回滚；写出库流水（备注处方号）
     */
    @Transactional
    public void dispense(String prescNo) {
        BizPrescription presc = prescriptionMapper.selectOne(Wrappers.<BizPrescription>lambdaQuery()
                .eq(BizPrescription::getPrescNo, prescNo));
        if (presc == null) {
            throw new BusinessException("处方不存在");
        }
        if (presc.getStatus() != BizPrescription.ST_AUDITED && presc.getStatus() != BizPrescription.ST_CHARGED) {
            throw new BusinessException("仅已审核/已收费处方可发药");
        }
        List<BizPrescriptionItem> items = itemMapper.selectList(Wrappers.<BizPrescriptionItem>lambdaQuery()
                .eq(BizPrescriptionItem::getPrescId, presc.getId()));

        Map<Long, Integer> needByDrug = items.stream().collect(Collectors.groupingBy(
                BizPrescriptionItem::getDrugId, Collectors.summingInt(BizPrescriptionItem::getQuantity)));

        // 先批量条件扣减（任一命中 0 行即回滚）
        for (Map.Entry<Long, Integer> e : needByDrug.entrySet()) {
            int updated = stockMapper.update(null, Wrappers.<BizStock>lambdaUpdate()
                    .setSql("quantity = quantity - " + e.getValue())
                    .eq(BizStock::getDrugId, e.getKey())
                    .apply("quantity >= {0}", e.getValue()));
            if (updated == 0) {
                throw new BusinessException("库存不足，发药失败：药品ID=" + e.getKey());
            }
        }
        // 状态迁移仲裁
        int updated = prescriptionMapper.update(null, Wrappers.<BizPrescription>lambdaUpdate()
                .set(BizPrescription::getStatus, BizPrescription.ST_DISPENSED)
                .eq(BizPrescription::getId, presc.getId())
                .in(BizPrescription::getStatus, BizPrescription.ST_AUDITED, BizPrescription.ST_CHARGED));
        if (updated == 0) {
            throw new BusinessException("发药失败：处方状态已变更");
        }
        for (Map.Entry<Long, Integer> e : needByDrug.entrySet()) {
            saveRecord(e.getKey(), BizStockRecord.TYPE_OUT, e.getValue(), null, "发药：" + prescNo);
        }
    }

    /**
     * 退药：仅 3已发药 → 4已退，回补库存写入库流水（退药不退款，退费衔接收费阶段）
     */
    @Transactional
    public void returnDrug(String prescNo) {
        BizPrescription presc = prescriptionMapper.selectOne(Wrappers.<BizPrescription>lambdaQuery()
                .eq(BizPrescription::getPrescNo, prescNo));
        if (presc == null) {
            throw new BusinessException("处方不存在");
        }
        if (presc.getStatus() != BizPrescription.ST_DISPENSED) {
            throw new BusinessException("仅已发药处方可退药");
        }
        List<BizPrescriptionItem> items = itemMapper.selectList(Wrappers.<BizPrescriptionItem>lambdaQuery()
                .eq(BizPrescriptionItem::getPrescId, presc.getId()));
        Map<Long, Integer> byDrug = items.stream().collect(Collectors.groupingBy(
                BizPrescriptionItem::getDrugId, Collectors.summingInt(BizPrescriptionItem::getQuantity)));

        for (Map.Entry<Long, Integer> e : byDrug.entrySet()) {
            stockMapper.update(null, Wrappers.<BizStock>lambdaUpdate()
                    .setSql("quantity = quantity + " + e.getValue())
                    .eq(BizStock::getDrugId, e.getKey()));
        }
        int updated = prescriptionMapper.update(null, Wrappers.<BizPrescription>lambdaUpdate()
                .set(BizPrescription::getStatus, BizPrescription.ST_RETURNED)
                .eq(BizPrescription::getId, presc.getId())
                .eq(BizPrescription::getStatus, BizPrescription.ST_DISPENSED));
        if (updated == 0) {
            throw new BusinessException("退药失败：处方状态已变更");
        }
        for (Map.Entry<Long, Integer> e : byDrug.entrySet()) {
            saveRecord(e.getKey(), BizStockRecord.TYPE_IN, e.getValue(), null, "退药：" + prescNo);
        }
    }

    /**
     * 预警列表：低库存 + 近效期（有效期 ≤ 今日+90天）
     */
    public List<Map<String, Object>> warnList() {
        List<BizStock> stocks = stockMapper.selectList(Wrappers.emptyWrapper());
        LocalDate nearLine = LocalDate.now().plusDays(DrugStockService.NEAR_EXPIRE_DAYS);
        return stocks.stream()
                .filter(s -> s.getQuantity() <= s.getWarnNum()
                        || (s.getExpireDate() != null && !s.getExpireDate().isAfter(nearLine)))
                .map(s -> {
                    BizDrug d = drugMapper.selectById(s.getDrugId());
                    boolean nearExpire = s.getExpireDate() != null && !s.getExpireDate().isAfter(nearLine);
                    boolean lowStock = s.getQuantity() <= s.getWarnNum();
                    return Map.<String, Object>of(
                            "drugId", s.getDrugId(),
                            "drugName", d == null ? String.valueOf(s.getDrugId()) : d.getDrugName(),
                            "quantity", s.getQuantity(),
                            "warnNum", s.getWarnNum(),
                            "expireDate", s.getExpireDate() == null ? "" : s.getExpireDate().toString(),
                            "type", nearExpire && !lowStock ? "expire" : (nearExpire && lowStock ? "both" : "lowStock"));
                })
                .toList();
    }

    /**
     * 发药页签数据源：按状态过滤处方列表
     */
    public List<Map<String, Object>> prescriptionList(List<Integer> statuses) {
        List<BizPrescription> list = prescriptionMapper.selectList(Wrappers.<BizPrescription>lambdaQuery()
                .in(BizPrescription::getStatus, statuses)
                .orderByDesc(BizPrescription::getId)
                .last("LIMIT 100"));
        return list.stream().map(p -> Map.<String, Object>of(
                "id", p.getId(),
                "prescNo", p.getPrescNo(),
                "status", p.getStatus(),
                "totalAmount", p.getTotalAmount(),
                "createTime", p.getCreateTime() == null ? "" : p.getCreateTime().toString())).toList();
    }

    private void requireDrug(Long drugId) {
        if (drugMapper.selectById(drugId) == null) {
            throw new BusinessException("药品不存在");
        }
    }

    private void saveRecord(Long drugId, int type, int quantity, String batchNo, String remark) {
        LoginUser operator = SecurityUtils.getCurrentUser();
        BizStockRecord record = new BizStockRecord();
        record.setDrugId(drugId);
        record.setRecordType(type);
        record.setQuantity(quantity);
        record.setBatchNo(batchNo);
        record.setRemark(remark);
        record.setOperatorId(operator.getId());
        recordMapper.insert(record);
    }
}
