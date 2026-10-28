package com.medical.config;

import com.medical.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.modules.audit.entity.SysConfig;
import com.medical.modules.audit.mapper.SysConfigMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * IP 白名单过滤器：security.ip-whitelist 非空时生效（自锁风险由前端保存时提示）。
 * 空值不限制；支持逗号分隔精确 IP。每次请求热读配置。
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class IpWhitelistFilter extends OncePerRequestFilter {

    private final SysConfigMapper configMapper;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String whitelist = getWhitelist();
        if (StringUtils.hasText(whitelist)) {
            String remote = request.getRemoteAddr();
            List<String> allowed = Arrays.stream(whitelist.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
            boolean pass = allowed.stream().anyMatch(ip ->
                    ip.equals(remote)
                            // 简单 CIDR /24 支持
                            || (ip.contains("/") && cidrMatch(remote, ip)));
            if (!pass) {
                response.setStatus(403);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(objectMapper.writeValueAsString(
                        Result.error(403, "当前 IP 不在访问白名单内")));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getWhitelist() {
        try {
            SysConfig cfg = configMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                    .eq(SysConfig::getConfigKey, "security.ip-whitelist"));
            return cfg == null ? null : cfg.getConfigValue();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean cidrMatch(String remote, String cidr) {
        try {
            String[] parts = cidr.split("/");
            byte[] base = java.net.InetAddress.getByName(parts[0]).getAddress();
            byte[] addr = java.net.InetAddress.getByName(remote).getAddress();
            int prefix = Integer.parseInt(parts[1]);
            if (base.length != 4 || addr.length != 4) return false;
            int fullBytes = prefix / 8;
            for (int i = 0; i < fullBytes && i < 4; i++) {
                if (base[i] != addr[i]) return false;
            }
            int remain = prefix % 8;
            if (remain > 0 && fullBytes < 4) {
                int mask = (0xFF << (8 - remain)) & 0xFF;
                if ((base[fullBytes] & mask) != (addr[fullBytes] & mask)) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
