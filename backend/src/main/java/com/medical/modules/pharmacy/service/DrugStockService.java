package com.medical.modules.pharmacy.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.modules.pharmacy.dto.DrugRequest;
import com.medical.modules.pharmacy.dto.DrugStockVO;
import com.medical.modules.pharmacy.entity.BizStock;
import com.medical.modules.pharmacy.mapper.StockMapper;
import com.medical.modules.clinic.entity.BizDrug;
import com.medical.modules.clinic.mapper.DrugMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DrugStockService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    /** 近效期窗口：有效期 ≤ 今日 + 90 天 */
    public static final int NEAR_EXPIRE_DAYS = 90;

    private final DrugMapper drugMapper;
    private final StockMapper stockMapper;

    /**
     * 药品库存聚合分页（药品全字段 + 库存 + stockStatus）
     * 筛选仅按药品启停状态下推；预警/近效期做视觉呈现
     */
    public PageResult<DrugStockVO> page(PageQuery query, Integer drugStatus) {
        Page<BizDrug> page = drugMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<BizDrug>lambdaQuery()
                        .and(StringUtils.hasText(query.getKeyword()), w -> w
                                .like(BizDrug::getDrugName, query.getKeyword())
                                .or().like(BizDrug::getDrugCode, query.getKeyword()))
                        .eq(StringUtils.hasText(query.getKeyword()) == false && drugStatus != null,
                                BizDrug::getStatus, drugStatus == null ? 1 : drugStatus)
                        .orderByAsc(BizDrug::getId));

        Map<Long, BizStock> stocks = stockMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.toMap(BizStock::getDrugId, s -> s, (a, b) -> a));

        List<DrugStockVO> rows = page.getRecords().stream()
                .map(d -> toVO(d, stocks.get(d.getId())))
                .toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }

    private DrugStockVO toVO(BizDrug d, BizStock stock) {
        DrugStockVO vo = new DrugStockVO();
        vo.setId(d.getId());
        vo.setDrugCode(d.getDrugCode());
        vo.setDrugName(d.getDrugName());
        vo.setGenericName(d.getGenericName());
        vo.setSpec(d.getSpec());
        vo.setDosageForm(d.getDosageForm());
        vo.setUnit(d.getUnit());
        vo.setManufacturer(d.getManufacturer());
        vo.setCategory(d.getCategory());
        vo.setSalePrice(d.getSalePrice());
        vo.setPurchasePrice(d.getPurchasePrice());
        vo.setIsSpecial(d.getIsSpecial());
        vo.setDrugStatus(d.getStatus());
        if (stock != null) {
            vo.setQuantity(stock.getQuantity());
            vo.setWarnNum(stock.getWarnNum());
            vo.setProduceDate(stock.getProduceDate() == null ? null : stock.getProduceDate().format(DATE_FMT));
            vo.setExpireDate(stock.getExpireDate() == null ? null : stock.getExpireDate().format(DATE_FMT));
            vo.setStockStatus(stockStatus(stock));
        }
        return vo;
    }

    private String stockStatus(BizStock stock) {
        if (stock.getExpireDate() != null
                && !stock.getExpireDate().isAfter(LocalDate.now().plusDays(NEAR_EXPIRE_DAYS))) {
            return "nearExpire";
        }
        if (stock.getQuantity() <= stock.getWarnNum()) {
            return "lowStock";
        }
        return "normal";
    }

    @Transactional
    public Long create(DrugRequest request) {
        // 药品编码：D + 5位自增（先插后回补）
        BizDrug drug = new BizDrug();
        applyRequest(drug, request);
        drug.setStatus(1);
        drugMapper.insert(drug);
        BizDrug patch = new BizDrug();
        patch.setId(drug.getId());
        patch.setDrugCode(String.format("D%05d", drug.getId()));
        drugMapper.updateById(patch);

        // 新增药品自动建库存行（数量 0、预警默认 50）
        BizStock stock = new BizStock();
        stock.setDrugId(drug.getId());
        stock.setQuantity(0);
        stock.setWarnNum(50);
        stockMapper.insert(stock);
        return drug.getId();
    }

    @Transactional
    public void update(Long id, DrugRequest request) {
        BizDrug drug = drugMapper.selectById(id);
        if (drug == null) {
            throw new BusinessException("药品不存在");
        }
        applyRequest(drug, request);
        drugMapper.updateById(drug);
    }

    /**
     * 删除：仅库存 0 可删（同删库存行）；有库存拒绝
     */
    @Transactional
    public void delete(Long id) {
        BizStock stock = stockMapper.selectOne(Wrappers.<BizStock>lambdaQuery()
                .eq(BizStock::getDrugId, id));
        if (stock != null && stock.getQuantity() > 0) {
            throw new BusinessException("该药品仍有库存，不可删除");
        }
        drugMapper.deleteById(id);
        if (stock != null) {
            stockMapper.deleteById(stock.getId());
        }
    }

    private void applyRequest(BizDrug drug, DrugRequest request) {
        drug.setDrugName(request.getDrugName());
        drug.setGenericName(request.getGenericName());
        drug.setSpec(request.getSpec());
        drug.setDosageForm(request.getDosageForm());
        drug.setUnit(request.getUnit());
        drug.setManufacturer(request.getManufacturer());
        drug.setApprovalNo(request.getApprovalNo());
        drug.setSalePrice(request.getSalePrice());
        drug.setPurchasePrice(request.getPurchasePrice());
        drug.setCategory(request.getCategory());
        drug.setIsSpecial(request.getIsSpecial() == null ? 0 : request.getIsSpecial());
    }
}
