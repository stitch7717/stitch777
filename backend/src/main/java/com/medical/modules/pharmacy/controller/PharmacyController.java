package com.medical.modules.pharmacy.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.common.Result;
import com.medical.modules.pharmacy.dto.DrugRequest;
import com.medical.modules.pharmacy.dto.DrugStockVO;
import com.medical.modules.pharmacy.dto.StockRequest;
import com.medical.modules.pharmacy.service.DrugStockService;
import com.medical.modules.pharmacy.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PharmacyController {

    private final DrugStockService drugStockService;
    private final StockService stockService;

    /** 药品库存聚合分页（一药一行同构数据） */
    @GetMapping("/api/drug/page")
    @PreAuthorize("hasAuthority('pharmacy:list')")
    public Result<PageResult<DrugStockVO>> drugPage(PageQuery query, Integer drugStatus) {
        return Result.ok(drugStockService.page(query, drugStatus));
    }

    /** 库存分页（同构别名，文档 §7.2） */
    @GetMapping("/api/drug/stock/page")
    @PreAuthorize("hasAuthority('pharmacy:list')")
    public Result<PageResult<DrugStockVO>> stockPage(PageQuery query, Integer drugStatus) {
        return Result.ok(drugStockService.page(query, drugStatus));
    }

    @PostMapping("/api/drug")
    @PreAuthorize("hasAuthority('pharmacy:drug')")
    public Result<Map<String, Long>> createDrug(@Valid @RequestBody DrugRequest request) {
        return Result.ok(Map.of("id", drugStockService.create(request)));
    }

    @PutMapping("/api/drug/{id}")
    @PreAuthorize("hasAuthority('pharmacy:drug')")
    public Result<Void> updateDrug(@PathVariable Long id, @Valid @RequestBody DrugRequest request) {
        drugStockService.update(id, request);
        return Result.ok();
    }

    @DeleteMapping("/api/drug/{id}")
    @PreAuthorize("hasAuthority('pharmacy:drug')")
    public Result<Void> deleteDrug(@PathVariable Long id) {
        drugStockService.delete(id);
        return Result.ok();
    }

    @PostMapping("/api/drug/stock/in")
    @PreAuthorize("hasAuthority('pharmacy:stock')")
    public Result<Void> stockIn(@Valid @RequestBody StockRequest request) {
        stockService.stockIn(request);
        return Result.ok();
    }

    @PostMapping("/api/drug/stock/out")
    @PreAuthorize("hasAuthority('pharmacy:stock')")
    public Result<Void> stockOut(@Valid @RequestBody StockRequest request) {
        stockService.stockOut(request);
        return Result.ok();
    }

    /** 预警列表（低库存 + 近效期） */
    @GetMapping("/api/drug/stock/warn")
    @PreAuthorize("hasAuthority('pharmacy:list')")
    public Result<List<Map<String, Object>>> warn() {
        return Result.ok(stockService.warnList());
    }

    /** 效期预警列表（近效期口径，文档 §7.2） */
    @GetMapping("/api/drug/stock/expire")
    @PreAuthorize("hasAuthority('pharmacy:list')")
    public Result<List<Map<String, Object>>> expire() {
        return Result.ok(stockService.warnList().stream()
                .filter(w -> !"lowStock".equals(w.get("type")))
                .toList());
    }

    /** 发药 */
    @PostMapping("/api/pharmacy/dispense")
    @PreAuthorize("hasAuthority('pharmacy:dispense:add')")
    public Result<Void> dispense(@RequestBody Map<String, String> body) {
        stockService.dispense(body.get("prescNo"));
        return Result.ok();
    }

    /** 退药 */
    @PostMapping("/api/pharmacy/return")
    @PreAuthorize("hasAuthority('pharmacy:dispense:return')")
    public Result<Void> returnDrug(@RequestBody Map<String, String> body) {
        stockService.returnDrug(body.get("prescNo"));
        return Result.ok();
    }

    /** 发药页 tabs 数据源（默认 1,2 待发药） */
    @GetMapping("/api/pharmacy/prescription/list")
    @PreAuthorize("hasAuthority('pharmacy:list')")
    public Result<List<Map<String, Object>>> prescriptionList(
            @RequestParam(defaultValue = "1,2") List<Integer> statuses) {
        return Result.ok(stockService.prescriptionList(statuses));
    }
}
