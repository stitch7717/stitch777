package com.medical.framework;

import com.medical.modules.system.entity.SysMenu;
import com.medical.modules.system.entity.SysRole;
import com.medical.modules.system.entity.SysRoleMenu;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysMenuMapper;
import com.medical.modules.system.mapper.SysRoleMapper;
import com.medical.modules.system.mapper.SysRoleMenuMapper;
import com.medical.modules.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：解析 token，加载用户、角色与权限码（@PreAuthorize 用）写入 SecurityContext
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parse(token);
                Long userId = Long.valueOf(claims.getSubject());
                SysUser user = userMapper.selectById(userId);
                if (user != null && user.getStatus() == 1 && user.getDeleted() == 0) {
                    // 改密/强制下线后 token_version 已 +1，旧 token 在这里失效
                    Integer issuedVersion = claims.get("ver", Integer.class);
                    int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
                    if ((issuedVersion == null ? 0 : issuedVersion) != currentVersion) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    String roleCode = null;
                    if (user.getRoleId() != null) {
                        SysRole role = roleMapper.selectById(user.getRoleId());
                        if (role != null && role.getStatus() == 1) {
                            roleCode = role.getRoleCode();
                        }
                    }
                    if (roleCode != null) {
                        LoginUser loginUser = new LoginUser(
                                user.getId(), user.getUsername(), user.getRealName(),
                                user.getRoleId(), roleCode);
                        List<SimpleGrantedAuthority> authorities = loadAuthorities(
                                "admin".equals(roleCode), user.getRoleId());
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception ignored) {
                // 无效/过期 token：保持未认证状态，由入口点统一返回 401
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 权限码集合：admin 全量菜单 perms，其余角色经 role_menu
     */
    private List<SimpleGrantedAuthority> loadAuthorities(boolean admin, Long roleId) {
        List<String> perms;
        if (admin) {
            perms = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                            .eq(SysMenu::getStatus, 1).isNotNull(SysMenu::getPerms))
                    .stream().map(SysMenu::getPerms).distinct().toList();
        } else {
            List<Long> menuIds = roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                            .eq(SysRoleMenu::getRoleId, roleId))
                    .stream().map(SysRoleMenu::getMenuId).distinct().toList();
            if (menuIds.isEmpty()) {
                return List.of();
            }
            perms = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                            .in(SysMenu::getId, menuIds)
                            .eq(SysMenu::getStatus, 1).isNotNull(SysMenu::getPerms))
                    .stream().map(SysMenu::getPerms).distinct().toList();
        }
        return perms.stream().map(SimpleGrantedAuthority::new).toList();
    }
}
