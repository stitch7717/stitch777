package com.medical.modules.finance.controller;

import com.medical.common.Result;
import com.medical.modules.finance.service.ChargeService;
import com.medical.modules.finance.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FinanceExtraController {

    private final ChargeService chargeService;
    private final SettlementService settlementService;

    /** 住院费用预演 */
    @GetMapping("/api/inpatient/admission/{id}/settle/preview")
    @PreAuthorize("hasAuthority('charge:list')")
    public Result<Map<String, Object>> settlePreview(@PathVariable Long id) {
        return Result.ok(settlementService.preview(id));
    }

    /** 出院结算 */
    @PostMapping("/api/inpatient/admission/{id}/settle")
    @PreAuthorize("hasAuthority('charge:operate')")
    public Result<Map<String, Object>> settle(@PathVariable Long id) {
        return Result.ok(settlementService.settle(id));
    }

    /** 月结：指定月份按日汇总（复用日结口径） */
    @GetMapping("/api/finance/monthly")
    @PreAuthorize("hasAuthority('charge:list')")
    public Result<List<Map<String, Object>>> monthly(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") String month) {
        LocalDate base = month == null ? LocalDate.now().withDayOfMonth(1)
                : LocalDate.parse(month + "-01");
        List<Map<String, Object>> days = new ArrayList<>();
        for (LocalDate d = base; !d.isAfter(base.withDayOfMonth(base.lengthOfMonth())); d = d.plusDays(1)) {
            Map<String, Object> day = chargeService.daily(d);
            days.add(Map.of("date", d.toString(), "income", day.get("income"),
                    "refund", day.get("refund"), "chargeCount", day.get("chargeCount")));
        }
        return Result.ok(days);
    }
}
