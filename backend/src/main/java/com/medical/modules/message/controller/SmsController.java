package com.medical.modules.message.controller;

import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.common.Result;
import com.medical.modules.message.entity.SysSmsRecord;
import com.medical.modules.message.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    @PostMapping("/api/sms/send")
    @PreAuthorize("hasAuthority('message:sms')")
    public Result<Map<String, Long>> send(@RequestBody Map<String, String> body) {
        return Result.ok(Map.of("id", smsService.send(
                body.get("phone"), body.get("templateCode"), body.get("content"))));
    }

    @GetMapping("/api/sms/page")
    @PreAuthorize("hasAuthority('message:sms')")
    public Result<PageResult<SysSmsRecord>> page(PageQuery query, String phone) {
        return Result.ok(smsService.page(query, phone));
    }
}
