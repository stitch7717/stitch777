package com.medical.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.modules.system.dto.UserPageVO;
import com.medical.modules.system.entity.SysDept;
import com.medical.modules.system.entity.SysRole;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysDeptMapper;
import com.medical.modules.system.mapper.SysRoleMapper;
import com.medical.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;

    public PageResult<UserPageVO> page(PageQuery query, Long deptId, Long roleId, Integer status) {
        Page<SysUser> page = userMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<SysUser>lambdaQuery()
                        .and(StringUtils.hasText(query.getKeyword()), w -> w
                                .like(SysUser::getUsername, query.getKeyword())
                                .or().like(SysUser::getRealName, query.getKeyword()))
                        .eq(Objects.nonNull(deptId), SysUser::getDeptId, deptId)
                        .eq(Objects.nonNull(roleId), SysUser::getRoleId, roleId)
                        .eq(Objects.nonNull(status), SysUser::getStatus, status)
                        .orderByAsc(SysUser::getId));

        Map<Long, String> deptNames = deptMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));
        Map<Long, String> roleNames = roleMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName, (a, b) -> a));

        List<UserPageVO> rows = page.getRecords().stream().map(u -> {
            UserPageVO vo = new UserPageVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setRealName(u.getRealName());
            vo.setGender(u.getGender());
            vo.setPhone(u.getPhone());
            vo.setEmail(u.getEmail());
            vo.setDeptId(u.getDeptId());
            vo.setDeptName(u.getDeptId() == null ? null : deptNames.get(u.getDeptId()));
            vo.setRoleId(u.getRoleId());
            vo.setRoleName(u.getRoleId() == null ? null : roleNames.get(u.getRoleId()));
            vo.setStatus(u.getStatus());
            vo.setCreateTime(u.getCreateTime() == null ? null : u.getCreateTime().format(FMT));
            return vo;
        }).toList();

        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }
}
