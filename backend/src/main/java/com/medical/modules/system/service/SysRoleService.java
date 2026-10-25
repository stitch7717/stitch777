package com.medical.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.common.BusinessException;
import com.medical.modules.system.entity.SysMenu;
import com.medical.modules.system.entity.SysRole;
import com.medical.modules.system.entity.SysRoleMenu;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysMenuMapper;
import com.medical.modules.system.mapper.SysRoleMapper;
import com.medical.modules.system.mapper.SysRoleMenuMapper;
import com.medical.modules.system.mapper.SysUserMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    public List<RoleItem> list() {
        List<SysRole> roles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .orderByAsc(SysRole::getId));
        Map<Long, Long> counts = userMapper.selectList(Wrappers.emptyWrapper()).stream()
                .filter(u -> u.getRoleId() != null)
                .collect(Collectors.groupingBy(SysUser::getRoleId, Collectors.counting()));
        return roles.stream().map(r -> {
            RoleItem item = new RoleItem();
            item.setId(r.getId());
            item.setRoleName(r.getRoleName());
            item.setRoleCode(r.getRoleCode());
            item.setRemark(r.getRemark());
            item.setStatus(r.getStatus());
            item.setUserCount(counts.getOrDefault(r.getId(), 0L));
            return item;
        }).toList();
    }

    /**
     * 角色详情：含已分配 menuIds 与用户数（供权限分配回显）
     */
    public Map<String, Object> detail(Long id) {
        SysRole role = requireRole(id);
        List<Long> menuIds = roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                        .eq(SysRoleMenu::getRoleId, id))
                .stream().map(SysRoleMenu::getMenuId).distinct().toList();
        long userCount = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getRoleId, id));
        return Map.of(
                "id", role.getId(),
                "roleName", role.getRoleName(),
                "roleCode", role.getRoleCode(),
                "remark", role.getRemark() == null ? "" : role.getRemark(),
                "status", role.getStatus(),
                "userCount", userCount,
                "menuIds", menuIds);
    }

    @Transactional
    public Long create(RoleRequest request) {
        if (!StringUtils.hasText(request.getRoleName())) {
            throw new BusinessException("角色名称不能为空");
        }
        Long dup = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleName, request.getRoleName()));
        if (dup > 0) {
            throw new BusinessException("角色名称已存在");
        }
        SysRole role = new SysRole();
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setRemark(request.getRemark());
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        roleMapper.insert(role);
        return role.getId();
    }

    /**
     * 修改角色：roleName/remark/status；roleCode 不可改
     */
    @Transactional
    public void update(Long id, RoleRequest request) {
        SysRole role = requireRole(id);
        if (!StringUtils.hasText(request.getRoleName())) {
            throw new BusinessException("角色名称不能为空");
        }
        Long dup = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleName, request.getRoleName())
                .ne(SysRole::getId, id));
        if (dup > 0) {
            throw new BusinessException("角色名称已存在");
        }
        role.setRoleName(request.getRoleName());
        role.setRemark(request.getRemark());
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }
        roleMapper.updateById(role);
    }

    /**
     * 分配权限：重建 role_menu 关联（逻辑删旧行 + 插入新行）
     */
    @Transactional
    public void assignPermissions(Long id, List<Long> menuIds) {
        requireRole(id);
        if (menuIds == null) {
            menuIds = List.of();
        }
        // 仅接受真实存在的菜单
        List<Long> validIds = menuIds.isEmpty() ? List.of()
                : menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery().in(SysMenu::getId, menuIds))
                        .stream().map(SysMenu::getId).toList();
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
        for (Long menuId : validIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(id);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    /**
     * 删除角色：硬删除；内置角色保护，有用户引用拒绝
     */
    @Transactional
    public void delete(Long id) {
        SysRole role = requireRole(id);
        if ("admin".equals(role.getRoleCode())) {
            throw new BusinessException("内置角色不可删除");
        }
        Long users = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getRoleId, id));
        if (users > 0) {
            throw new BusinessException("该角色下有 " + users + " 个用户，不可删除");
        }
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
        roleMapper.deleteById(id);
    }

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    @Data
    public static class RoleItem {
        private Long id;
        private String roleName;
        private String roleCode;
        private String remark;
        private Integer status;
        private Long userCount;
    }

    @Data
    public static class RoleRequest {
        private String roleName;
        private String roleCode;
        private String remark;
        private Integer status;
    }
}
