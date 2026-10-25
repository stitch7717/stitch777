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

    /** 登录失败计数（课程级内存实现，生产换 Redis；键=用户名） */
    private static final java.util.concurrent.ConcurrentHashMap<String, int[]> FAIL_COUNT = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int LOCK_SECONDS = 600;
    private static final String LOCK_MSG = "登录失败次数过多，账号已锁定10分钟";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final com.medical.modules.audit.service.AuditService auditService;
    private final jakarta.servlet.http.HttpServletRequest request;

    public LoginResponse login(LoginRequest request0) {
        String ip = request == null ? "" : request.getRemoteAddr();
        // 锁定检查
        int[] counter = FAIL_COUNT.get(request0.getUsername());
        if (counter != null && counter[1] > System.currentTimeMillis() / 1000) {
            auditService.saveLoginLog(request0.getUsername(), false, LOCK_MSG, ip);
            throw new BusinessException(423, LOCK_MSG);
        }

        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request0.getUsername()));
        if (user == null || !passwordEncoder.matches(request0.getPassword(), user.getPassword())) {
            // 密码错（含用户名不存在）计数；密码正确但账号禁用不计数
            recordFail(request0.getUsername());
            auditService.saveLoginLog(request0.getUsername(), false, "用户名或密码错误", ip);
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            auditService.saveLoginLog(request0.getUsername(), false, "账号已被禁用", ip);
            throw new BusinessException("账号已被禁用");
        }
        SysRole role = null;
        if (user.getRoleId() != null) {
            role = roleMapper.selectById(user.getRoleId());
        }
        if (role == null || role.getStatus() != 1) {
            auditService.saveLoginLog(request0.getUsername(), false, "账号角色不可用", ip);
            throw new BusinessException("账号角色不可用，请联系管理员");
        }
        FAIL_COUNT.remove(request0.getUsername());
        auditService.saveLoginLog(request0.getUsername(), true, "登录成功", ip);

        LoginResponse resp = new LoginResponse();
        resp.setToken(jwtUtil.generate(user.getId(), user.getUsername()));
        LoginResponse.UserInfo info = new LoginResponse.UserInfo();
        info.setId(user.getId());
        info.setRealName(user.getRealName());
        info.setRoleCode(role.getRoleCode());
        info.setRoleName(role.getRoleName());
        resp.setUserInfo(info);
        return resp;
    }

    private void recordFail(String username) {
        int now = (int) (System.currentTimeMillis() / 1000);
        int[] c = FAIL_COUNT.computeIfAbsent(username, k -> new int[2]);
        // 安全配置 security.fail-count（默认 5）；DB 读取略，课程级读配置常量
        c[0]++;
        if (c[0] >= failCount()) {
            c[1] = now + LOCK_SECONDS;
            c[0] = 0;
        }
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
