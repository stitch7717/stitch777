package com.medical.modules.registration.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.common.Result;
import com.medical.modules.registration.dto.RegisterRequest;
import com.medical.modules.registration.service.RegistrationService;
import com.medical.modules.system.entity.SysDept;
import com.medical.modules.system.entity.SysRole;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysDeptMapper;
import com.medical.modules.system.mapper.SysRoleMapper;
import com.medical.modules.system.mapper.SysUserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/registration")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;
    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    @PostMapping
    @PreAuthorize("hasAuthority('registration:register')")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(registrationService.register(request));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('registration:register')")
    public Result<Void> cancel(@PathVariable Long id) {
        registrationService.cancel(id);
        return Result.ok();
    }

    @GetMapping("/queue/{deptId}")
    @PreAuthorize("hasAuthority('registration:list')")
    public Result<List<RegistrationService.QueueVO>> queue(@PathVariable Long deptId) {
        return Result.ok(registrationService.queue(deptId));
    }

    @PostMapping("/call/{id}")
    @PreAuthorize("hasAuthority('registration:call')")
    public Result<Void> call(@PathVariable Long id) {
        registrationService.call(id);
        return Result.ok();
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('registration:list')")
    public Result<Object> page(@RequestParam(defaultValue = "1") long pageNum,
                               @RequestParam(defaultValue = "10") long pageSize,
                               Long patientId, Integer status) {
        return Result.ok(registrationService.page(pageNum, pageSize, patientId, status));
    }

    /** 科室下拉（挂号页专用最小字段） */
    @GetMapping("/depts")
    @PreAuthorize("hasAuthority('registration:list')")
    public Result<List<Map<String, Object>>> depts() {
        List<Map<String, Object>> list = deptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                        .eq(SysDept::getStatus, 1).orderByAsc(SysDept::getSort))
                .stream().map(d -> Map.<String, Object>of("id", d.getId(), "name", d.getDeptName()))
                .toList();
        return Result.ok(list);
    }

    /** 医生下拉（按科室可选，最小字段） */
    @GetMapping("/doctors")
    @PreAuthorize("hasAuthority('registration:list')")
    public Result<List<Map<String, Object>>> doctors(Long deptId) {
        SysRole doctorRole = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, "doctor"));
        if (doctorRole == null) {
            return Result.ok(List.of());
        }
        List<Map<String, Object>> list = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getRoleId, doctorRole.getId())
                        .eq(SysUser::getStatus, 1)
                        .eq(deptId != null, SysUser::getDeptId, deptId))
                .stream().map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "name", u.getRealName() != null ? u.getRealName() : u.getUsername(),
                        "deptId", u.getDeptId() == null ? 0L : u.getDeptId()))
                .toList();
        return Result.ok(list);
    }
}
