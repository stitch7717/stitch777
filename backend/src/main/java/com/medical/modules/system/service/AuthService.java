package com.medical.modules.system.service;

import com.medical.common.BusinessException;
import com.medical.framework.JwtUtil;
import com.medical.modules.system.dto.LoginRequest;
import com.medical.modules.system.dto.LoginResponse;
import com.medical.modules.system.entity.SysRole;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysRoleMapper;
import com.medical.modules.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String LOCK_MSG = "登录失败次数过多，账号已锁定10分钟";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginFailureService loginFailureService;
    private final com.medical.modules.audit.service.AuditService auditService;
    private final jakarta.servlet.http.HttpServletRequest request;

    public LoginResponse login(LoginRequest request0) {
        String ip = request == null ? "" : request.getRemoteAddr();
        String username = request0.getUsername();
        int maxFails = failCount();

        // 锁定检查：Redis 优先，Redis 不可用时回退到内存
        if (loginFailureService.isLocked(username, maxFails)) {
            auditService.saveLoginLog(username, false, LOCK_MSG, ip);
            throw new BusinessException(423, LOCK_MSG);
        }

        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
        if (user == null || !passwordEncoder.matches(request0.getPassword(), user.getPassword())) {
            // 密码错（含用户名不存在）计数；密码正确但账号禁用不计数
            loginFailureService.recordFailure(username, maxFails);
            auditService.saveLoginLog(username, false, "用户名或密码错误", ip);
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            auditService.saveLoginLog(username, false, "账号已被禁用", ip);
            throw new BusinessException("账号已被禁用");
        }
        SysRole role = null;
        if (user.getRoleId() != null) {
            role = roleMapper.selectById(user.getRoleId());
        }
        if (role == null || role.getStatus() != 1) {
            auditService.saveLoginLog(username, false, "账号角色不可用", ip);
            throw new BusinessException("账号角色不可用，请联系管理员");
        }
        loginFailureService.clear(username);
        auditService.saveLoginLog(username, true, "登录成功", ip);

        LoginResponse resp = new LoginResponse();
        resp.setToken(jwtUtil.generate(user.getId(), user.getUsername(),
                user.getTokenVersion() == null ? 0 : user.getTokenVersion()));
        LoginResponse.UserInfo info = new LoginResponse.UserInfo();
        info.setId(user.getId());
        info.setRealName(user.getRealName());
        info.setRoleCode(role.getRoleCode());
        info.setRoleName(role.getRoleName());
        resp.setUserInfo(info);
        return resp;
    }

    private int failCount() {
        try {
            String v = auditServiceConfig();
            return v == null ? 5 : Integer.parseInt(v);
        } catch (Exception e) {
            return 5;
        }
    }

    private String auditServiceConfig() {
        try {
            return auditService.configList().stream()
                    .filter(c -> "security.fail-count".equals(c.getConfigKey()))
                    .map(com.medical.modules.audit.entity.SysConfig::getConfigValue)
                    .findFirst().orElse("5");
        } catch (Exception e) {
            return "5";
        }
    }
}
