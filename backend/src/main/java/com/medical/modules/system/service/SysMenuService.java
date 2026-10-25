package com.medical.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.modules.system.dto.MenuVO;
import com.medical.framework.LoginUser;
import com.medical.framework.SecurityUtils;
import com.medical.modules.system.entity.SysMenu;
import com.medical.modules.system.entity.SysRoleMenu;
import com.medical.modules.system.mapper.SysMenuMapper;
import com.medical.modules.system.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    /**
     * 当前用户可见菜单（目录+菜单树，不含按钮；admin 全量，其余按 role_menu）
     */
    public List<MenuVO> currentUserMenus() {
        List<SysMenu> menus = listVisibleMenus(false);
        return buildTree(menus);
    }

    /**
     * 全量菜单树（含按钮，供角色权限分配）
     */
    public List<MenuVO> fullTree() {
        List<SysMenu> menus = listVisibleMenus(true);
        return buildTree(menus);
    }

    private List<SysMenu> listVisibleMenus(boolean includeButton) {
        LoginUser current = SecurityUtils.getCurrentUser();
        boolean admin = "admin".equals(current.getRoleCode());
        List<SysMenu> menus;
        if (admin) {
            menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                    .eq(SysMenu::getStatus, 1)
                    .orderByAsc(SysMenu::getSort).orderByAsc(SysMenu::getId));
        } else {
            List<Long> menuIds = roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                            .eq(SysRoleMenu::getRoleId, current.getRoleId()))
                    .stream().map(SysRoleMenu::getMenuId).distinct().toList();
            if (menuIds.isEmpty()) {
                return List.of();
            }
            menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                    .in(SysMenu::getId, menuIds)
                    .eq(SysMenu::getStatus, 1)
                    .orderByAsc(SysMenu::getSort).orderByAsc(SysMenu::getId));
        }
        if (!includeButton) {
            menus = menus.stream()
                    .filter(m -> m.getMenuType() != SysMenu.TYPE_BUTTON)
                    .toList();
        }
        return menus;
    }

    private List<MenuVO> buildTree(List<SysMenu> menus) {
        List<MenuVO> nodes = menus.stream().map(m -> {
            MenuVO vo = new MenuVO();
            vo.setId(m.getId());
            vo.setMenuName(m.getMenuName());
            vo.setParentId(m.getParentId());
            vo.setPath(m.getPath());
            vo.setPerms(m.getPerms());
            vo.setIcon(m.getIcon());
            vo.setMenuType(m.getMenuType());
            vo.setSort(m.getSort());
            return vo;
        }).toList();
        var byId = nodes.stream().collect(java.util.stream.Collectors.toMap(MenuVO::getId, n -> n));
        List<MenuVO> roots = new java.util.ArrayList<>();
        for (MenuVO node : nodes) {
            MenuVO parent = node.getParentId() == null ? null : byId.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        roots.sort(Comparator.comparing(MenuVO::getSort));
        return roots;
    }
}
