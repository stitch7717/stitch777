package com.medical.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.common.BusinessException;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysUserAdminService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /** username 唯一口径：含已删除行（删除后不可重建同名账号） */
    private void checkUsername(String username, Long excludeId) {
        Long dup = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .ne(excludeId != null, SysUser::getId, excludeId));
        if (dup > 0) {
            throw new BusinessException("登录账号已存在");
        }
    }

    @Transactional
    public Long create(SysUser user) {
        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            throw new BusinessException("账号与密码必填");
        }
        checkUsername(user.getUsername(), null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) user.setStatus(1);
        user.setId(null);
        user.setAvatar(null);
        userMapper.insert(user);
        return user.getId();
    }

    @Transactional
    public void update(Long id, SysUser patch) {
        SysUser exist = require(id);
        if (StringUtils.hasText(patch.getUsername()) && !patch.getUsername().equals(exist.getUsername())) {
            checkUsername(patch.getUsername(), id);
            exist.setUsername(patch.getUsername());
        }
        if (StringUtils.hasText(patch.getRealName())) exist.setRealName(patch.getRealName());
        if (patch.getGender() != null) exist.setGender(patch.getGender());
        if (patch.getPhone() != null) exist.setPhone(patch.getPhone());
        if (patch.getEmail() != null) exist.setEmail(patch.getEmail());
        if (patch.getDeptId() != null) exist.setDeptId(patch.getDeptId());
        if (patch.getRoleId() != null) exist.setRoleId(patch.getRoleId());
        userMapper.updateById(exist);
    }

    /** 禁止删除自己与内置 admin */
    @Transactional
    public void delete(Long id) {
        SysUser user = require(id);
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException("内置管理员不可删除");
        }
        LoginCheck(id);
        userMapper.deleteById(id);
    }

    private void LoginCheck(Long id) {
        var current = com.medical.framework.SecurityUtils.getCurrentUser();
        if (current != null && id.equals(current.getId())) {
            throw new BusinessException("不可删除当前登录账号");
        }
    }

    @Transactional
    public void changeStatus(Long id, Integer status) {
        SysUser user = require(id);
        if ("admin".equals(user.getUsername()) && status != null && status == 0) {
            throw new BusinessException("内置管理员不可禁用");
        }
        SysUser patch = new SysUser();
        patch.setId(id);
        patch.setStatus(status);
        userMapper.updateById(patch);
    }

    /** 重置密码：缺省重置为 123456 */
    @Transactional
    public void resetPassword(Long id, String password) {
        SysUser user = require(id);
        String raw = StringUtils.hasText(password) ? password : "123456";
        SysUser patch = new SysUser();
        patch.setId(id);
        patch.setPassword(passwordEncoder.encode(raw));
        // 版本 +1：该账号所有已签发的 token 立即失效，需要重新登录
        patch.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userMapper.updateById(patch);
    }

    private SysUser require(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BusinessException("用户不存在");
        return u;
    }
}
