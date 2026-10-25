package com.medical.modules.system.controller;

import com.medical.common.Result;
import com.medical.modules.system.dto.MenuVO;
import com.medical.modules.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @GetMapping("/tree")
    public Result<List<MenuVO>> tree() {
        return Result.ok(deptService.tree());
    }
}
