package com.medical.modules.registration.controller;

import com.medical.common.Result;
import com.medical.modules.registration.dto.ScheduleRequest;
import com.medical.modules.registration.dto.ScheduleVO;
import com.medical.modules.registration.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/registration/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    @PreAuthorize("hasAuthority('registration:list')")
    public Result<List<ScheduleVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Long deptId, Long doctorId) {
        return Result.ok(scheduleService.list(startDate, endDate, deptId, doctorId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('registration:schedule')")
    public Result<Void> create(@Valid @RequestBody ScheduleRequest request) {
        scheduleService.create(request);
        return Result.ok();
    }

    @PutMapping("/{id}/stop")
    @PreAuthorize("hasAuthority('registration:schedule')")
    public Result<Void> stop(@PathVariable Long id) {
        scheduleService.stop(id);
        return Result.ok();
    }
}
