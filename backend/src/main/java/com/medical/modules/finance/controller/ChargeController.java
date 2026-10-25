package com.medical.modules.finance.controller;

import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.common.Result;
import com.medical.modules.finance.dto.ChargeRequest;
import com.medical.modules.finance.dto.ChargeVO;
import com.medical.modules.finance.service.ChargeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeService chargeService;

    /** 待收费列表（收费台数据源） */
    @GetMapping("/api/charge/pending")
    @PreAuthorize("hasAuthority('charge:list')")
    public Result<List<Map<String, Object>>> pending() {
        return Result.ok(chargeService.pendingList());
    }

    /** 待收费明细（收费确认弹窗，含挂号费首项） */
    @GetMapping("/api/charge/pending/{visitId}")
    @PreAuthorize("hasAuthority('charge:list')")
    public Result<Map<String, Object>> pendingDetail(@PathVariable Long visitId) {
        return Result.ok(chargeService.pendingDetail(visitId));
    }

    /** 整单收费 */
    @PostMapping("/api/charge")
    @PreAuthorize("hasAuthority('charge:operate')")
    public Result<Map<String, Object>> charge(@Valid @RequestBody ChargeRequest request) {
        return Result.ok(chargeService.charge(request));
    }

    /** 退费 */
    @PostMapping("/api/charge/{id}/refund")
    @PreAuthorize("hasAuthority('charge:operate')")
    public Result<Void> refund(@PathVariable Long id) {
        chargeService.refund(id);
        return Result.ok();
    }

    /** 收费记录分页 */
    @GetMapping("/api/charge/page")
    @PreAuthorize("hasAuthority('charge:list')")
    public Result<PageResult<ChargeVO>> page(PageQuery query, Integer payStatus) {
        return Result.ok(chargeService.page(query, payStatus));
    }

    /** 收费明细 */
    @GetMapping("/api/charge/{id}/detail")
    @PreAuthorize("hasAuthority('charge:list')")
    public Result<ChargeVO> detail(@PathVariable Long id) {
        return Result.ok(chargeService.detail(id));
    }

    /** 日结 */
    @GetMapping("/api/finance/daily")
    @PreAuthorize("hasAuthority('charge:list')")
    public Result<Map<String, Object>> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(chargeService.daily(date));
    }
}
