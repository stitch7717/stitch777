package com.medical.modules.finance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.modules.finance.entity.BizCharge;
import com.medical.modules.finance.entity.BizInvoice;
import com.medical.modules.finance.mapper.BizInvoiceMapper;
import com.medical.modules.finance.mapper.ChargeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final BizInvoiceMapper invoiceMapper;
    private final ChargeMapper chargeMapper;

    /** 开票：一单一有效（重复开票 400），作废后可重开 */
    @Transactional
    public Map<String, Object> issue(Long chargeId, Integer invoiceType) {
        BizCharge charge = chargeMapper.selectById(chargeId);
        if (charge == null) throw new BusinessException("收费单不存在");
        if (charge.getPayStatus() != 1) throw new BusinessException("仅已支付收费单可开票");
        Long active = invoiceMapper.selectCount(Wrappers.<BizInvoice>lambdaQuery()
                .eq(BizInvoice::getChargeId, chargeId)
                .eq(BizInvoice::getStatus, BizInvoice.ST_NORMAL));
        if (active > 0) throw new BusinessException("该收费单已有有效发票");
        BizInvoice inv = new BizInvoice();
        inv.setChargeId(chargeId);
        inv.setInvoiceType(invoiceType == null ? 1 : invoiceType);
        inv.setAmount(charge.getActualAmount());
        inv.setStatus(BizInvoice.ST_NORMAL);
        inv.setPrintTime(LocalDateTime.now());
        invoiceMapper.insert(inv);
        String no = "INV" + LocalDate.now().format(NO_FMT) + String.format("%06d", inv.getId());
        BizInvoice patch = new BizInvoice();
        patch.setId(inv.getId());
        patch.setInvoiceNo(no);
        invoiceMapper.updateById(patch);
        return Map.of("id", inv.getId(), "invoiceNo", no);
    }

    /** 作废：退费后可重开 */
    @Transactional
    public void voidInvoice(Long id) {
        BizInvoice inv = invoiceMapper.selectById(id);
        if (inv == null) throw new BusinessException("发票不存在");
        if (inv.getStatus() == BizInvoice.ST_VOIDED) throw new BusinessException("发票已作废");
        BizInvoice patch = new BizInvoice();
        patch.setId(id);
        patch.setStatus(BizInvoice.ST_VOIDED);
        invoiceMapper.updateById(patch);
    }

    public PageResult<BizInvoice> page(PageQuery query, Long chargeId, Integer status) {
        Page<BizInvoice> page = invoiceMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<BizInvoice>lambdaQuery()
                        .eq(chargeId != null, BizInvoice::getChargeId, chargeId)
                        .eq(status != null, BizInvoice::getStatus, status)
                        .orderByDesc(BizInvoice::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}
