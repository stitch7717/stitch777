package com.medical.modules.finance.controller;

import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.common.Result;
import com.medical.modules.finance.entity.BizInvoice;
import com.medical.modules.finance.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/api/invoice/print")
    @PreAuthorize("hasAuthority('finance:invoice')")
    public Result<Map<String, Object>> issue(@RequestBody Map<String, Object> body) {
        Long chargeId = Long.valueOf(String.valueOf(body.get("chargeId")));
        Integer type = body.get("invoiceType") == null ? 1
                : Integer.valueOf(String.valueOf(body.get("invoiceType")));
        return Result.ok(invoiceService.issue(chargeId, type));
    }

    @GetMapping("/api/invoice/page")
    @PreAuthorize("hasAuthority('charge:list')")
    public Result<PageResult<BizInvoice>> page(PageQuery query, Long chargeId, Integer status) {
        return Result.ok(invoiceService.page(query, chargeId, status));
    }

    @PutMapping("/api/invoice/{id}/void")
    @PreAuthorize("hasAuthority('finance:invoice')")
    public Result<Void> voidInvoice(@PathVariable Long id) {
        invoiceService.voidInvoice(id);
        return Result.ok();
    }
}
