package com.medical.modules.pharmacy.controller;

import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.common.Result;
import com.medical.modules.pharmacy.entity.BizSupplier;
import com.medical.modules.pharmacy.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pharmacy")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping("/supplier/list")
    @PreAuthorize("hasAuthority('pharmacy:list')")
    public Result<List<BizSupplier>> suppliers() {
        return Result.ok(purchaseService.suppliers());
    }

    @PostMapping("/supplier")
    @PreAuthorize("hasAuthority('pharmacy:purchase')")
    public Result<Map<String, Long>> addSupplier(@RequestBody BizSupplier supplier) {
        return Result.ok(Map.of("id", purchaseService.addSupplier(supplier)));
    }

    @GetMapping("/purchase/page")
    @PreAuthorize("hasAuthority('pharmacy:list')")
    public Result<PageResult<Map<String, Object>>> orderPage(PageQuery query, Integer status) {
        return Result.ok(purchaseService.orderPage(query, status));
    }

    @PostMapping("/purchase")
    @PreAuthorize("hasAuthority('pharmacy:purchase')")
    public Result<Map<String, Long>> create(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of("id", purchaseService.createOrder(
                Long.valueOf(String.valueOf(body.get("supplierId"))),
                Long.valueOf(String.valueOf(body.get("drugId"))),
                Integer.valueOf(String.valueOf(body.get("quantity"))),
                new BigDecimal(String.valueOf(body.get("unitPrice"))))));
    }

    @PutMapping("/purchase/{id}/receive")
    @PreAuthorize("hasAuthority('pharmacy:purchase')")
    public Result<Void> receive(@PathVariable Long id) {
        purchaseService.receive(id);
        return Result.ok();
    }

    @PutMapping("/purchase/{id}/cancel")
    @PreAuthorize("hasAuthority('pharmacy:purchase')")
    public Result<Void> cancel(@PathVariable Long id) {
        purchaseService.cancel(id);
        return Result.ok();
    }
}
