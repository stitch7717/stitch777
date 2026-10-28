package com.medical.modules.system.controller;

import com.medical.common.Result;
import com.medical.modules.system.dto.MenuVO;
import com.medical.modules.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    /** 部门树：用户管理、部门管理共用，二者任一权限即可 */
    @PreAuthorize("hasAnyAuthority('system:dept:list','system:user:list')")
    @GetMapping("/tree")
    public Result<List<MenuVO>> tree() {
        return Result.ok(deptService.tree());
    }
}
