package com.medical.modules.pharmacy.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.modules.clinic.entity.BizDrug;
import com.medical.modules.clinic.mapper.DrugMapper;
import com.medical.modules.pharmacy.dto.StockRequest;
import com.medical.modules.pharmacy.entity.BizPurchaseOrder;
import com.medical.modules.pharmacy.entity.BizSupplier;
import com.medical.modules.pharmacy.mapper.BizPurchaseOrderMapper;
import com.medical.modules.pharmacy.mapper.BizSupplierMapper;
import com.medical.framework.LoginUser;
import com.medical.framework.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final BizSupplierMapper supplierMapper;
    private final BizPurchaseOrderMapper orderMapper;
    private final DrugMapper drugMapper;
    private final StockService stockService;

    // ---- 供应商 ----
    public List<BizSupplier> suppliers() {
        return supplierMapper.selectList(Wrappers.<BizSupplier>lambdaQuery()
                .eq(BizSupplier::getStatus, 1).orderByAsc(BizSupplier::getId));
    }

    @Transactional
    public Long addSupplier(BizSupplier supplier) {
        if (!org.springframework.util.StringUtils.hasText(supplier.getName())) {
            throw new BusinessException("供应商名称必填");
        }
        supplier.setStatus(1);
        supplierMapper.insert(supplier);
        return supplier.getId();
    }

    // ---- 采购订单 ----
    public PageResult<Map<String, Object>> orderPage(PageQuery query, Integer status) {
        Page<BizPurchaseOrder> page = orderMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<BizPurchaseOrder>lambdaQuery()
                        .eq(status != null, BizPurchaseOrder::getStatus, status)
                        .orderByDesc(BizPurchaseOrder::getId));
        List<Map<String, Object>> rows = page.getRecords().stream().map(o -> {
            BizSupplier s = supplierMapper.selectById(o.getSupplierId());
            BizDrug d = drugMapper.selectById(o.getDrugId());
            return Map.<String, Object>of(
                    "id", o.getId(), "orderNo", o.getOrderNo() == null ? "" : o.getOrderNo(),
                    "supplierName", s == null ? "" : s.getName(),
                    "drugName", d == null ? "" : d.getDrugName(),
                    "quantity", o.getQuantity(), "unitPrice", o.getUnitPrice(),
                    "total", o.getTotal(), "status", o.getStatus(),
                    "createTime", o.getCreateTime() == null ? "" : o.getCreateTime().toString());
        }).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }

    /** 创建采购订单：先插后回补 PO+日期+序号 */
    @Transactional
    public Long createOrder(Long supplierId, Long drugId, Integer quantity, BigDecimal unitPrice) {
        if (supplierMapper.selectById(supplierId) == null) throw new BusinessException("供应商不存在");
        if (drugMapper.selectById(drugId) == null) throw new BusinessException("药品不存在");
        if (quantity == null || quantity <= 0) throw new BusinessException("采购数量必须大于0");
        LoginUser user = SecurityUtils.getCurrentUser();
        BizPurchaseOrder order = new BizPurchaseOrder();
        order.setSupplierId(supplierId);
        order.setDrugId(drugId);
        order.setQuantity(quantity);
        order.setUnitPrice(unitPrice);
        order.setTotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(0);
        order.setOperatorId(user.getId());
        orderMapper.insert(order);
        String no = "PO" + LocalDate.now().format(NO_FMT) + String.format("%04d", order.getId());
        BizPurchaseOrder patch = new BizPurchaseOrder();
        patch.setId(order.getId());
        patch.setOrderNo(no);
        orderMapper.updateById(patch);
        return order.getId();
    }

    /** 采购入库：0待入库→1已入库，联动库存入库 */
    @Transactional
    public void receive(Long id) {
        BizPurchaseOrder order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException("采购单不存在");
        if (order.getStatus() != 0) throw new BusinessException("仅待入库采购单可入库");
        int updated = orderMapper.update(null, Wrappers.<BizPurchaseOrder>lambdaUpdate()
                .set(BizPurchaseOrder::getStatus, 1)
                .eq(BizPurchaseOrder::getId, id)
                .eq(BizPurchaseOrder::getStatus, 0));
        if (updated == 0) throw new BusinessException("入库失败，请刷新重试");
        StockRequest req = new StockRequest();
        req.setDrugId(order.getDrugId());
        req.setType(1);
        req.setQuantity(order.getQuantity());
        req.setRemark("采购入库：" + order.getOrderNo());
        stockService.stockIn(req);
    }

    @Transactional
    public void cancel(Long id) {
        BizPurchaseOrder order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException("采购单不存在");
        if (order.getStatus() != 0) throw new BusinessException("仅待入库采购单可取消");
        BizPurchaseOrder patch = new BizPurchaseOrder();
        patch.setId(id);
        patch.setStatus(2);
        orderMapper.updateById(patch);
    }
}
