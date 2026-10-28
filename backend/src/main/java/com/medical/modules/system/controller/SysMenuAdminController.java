package com.medical.modules.system.controller;

import com.medical.common.Result;
import com.medical.modules.system.entity.SysMenu;
import com.medical.modules.system.entity.SysRoleMenu;
import com.medical.modules.system.mapper.SysMenuMapper;
import com.medical.modules.system.mapper.SysRoleMenuMapper;
import com.medical.modules.system.service.PermissionCacheService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class SysMenuAdminController {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final PermissionCacheService permissionCacheService;

    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:edit')")
    public Result<Void> create(@RequestBody SysMenu menu) {
        save(menu);
        permissionCacheService.evictAll();
        return Result.ok();
    }

    private void save(SysMenu menu) {
        if (!org.springframework.util.StringUtils.hasText(menu.getMenuName())) {
            throw new com.medical.common.BusinessException("菜单名称必填");
        }
        if (menu.getParentId() == null) menu.setParentId(0L);
        if (menu.getStatus() == null) menu.setStatus(1);
        if (menu.getMenuType() == null) menu.setMenuType(2);
        menu.setId(null);
        menuMapper.insert(menu);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysMenu patch) {
        SysMenu exist = menuMapper.selectById(id);
        if (exist == null) throw new com.medical.common.BusinessException("菜单不存在");
        if (org.springframework.util.StringUtils.hasText(patch.getMenuName())) exist.setMenuName(patch.getMenuName());
        if (patch.getPath() != null) exist.setPath(patch.getPath());
        if (patch.getPerms() != null) exist.setPerms(patch.getPerms());
        if (patch.getIcon() != null) exist.setIcon(patch.getIcon());
        if (patch.getSort() != null) exist.setSort(patch.getSort());
        if (patch.getStatus() != null) exist.setStatus(patch.getStatus());
        menuMapper.updateById(exist);
        permissionCacheService.evictAll();
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:edit')")
    @Transactional
    public Result<Void> delete(@PathVariable Long id) {
        if (menuMapper.selectCount(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getParentId, id)) > 0) {
            throw new com.medical.common.BusinessException("存在子菜单，先删除子菜单");
        }
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getMenuId, id));
        menuMapper.deleteById(id);
        // 事务提交后再删缓存，避免并发请求在提交前缓存旧权限
        permissionCacheService.evictAllAfterCommit();
        return Result.ok();
    }
}
