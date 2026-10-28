package com.medical.modules.system.controller;

import com.medical.common.Result;
import com.medical.modules.system.dto.MenuVO;
import com.medical.modules.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    /** 当前用户可见菜单（含按钮权限，无角色返回空数组） */
    @GetMapping("/current")
    public Result<List<MenuVO>> current() {
        return Result.ok(menuService.currentUserMenus());
    }

    /** 全量菜单树（供菜单管理、角色权限分配） */
    @PreAuthorize("hasAnyAuthority('system:menu:edit','system:role:perm')")
    @GetMapping("/tree")
    public Result<List<MenuVO>> tree() {
        return Result.ok(menuService.fullTree());
    }
}
