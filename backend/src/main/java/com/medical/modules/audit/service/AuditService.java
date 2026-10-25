package com.medical.modules.audit.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.modules.audit.entity.SysBackup;
import com.medical.modules.audit.entity.SysConfig;
import com.medical.modules.audit.entity.SysLoginLog;
import com.medical.modules.audit.entity.SysOperationLog;
import com.medical.modules.audit.mapper.SysBackupMapper;
import com.medical.modules.audit.mapper.SysConfigMapper;
import com.medical.modules.audit.mapper.SysLoginLogMapper;
import com.medical.modules.audit.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final SysOperationLogMapper operationMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final SysBackupMapper backupMapper;
    private final SysConfigMapper configMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // ---------------- 日志查询 ----------------

    public PageResult<SysOperationLog> operationPage(PageQuery query, String module,
                                                     Integer status, String username) {
        Page<SysOperationLog> page = operationMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<SysOperationLog>lambdaQuery()
                        .like(StringUtils.hasText(username), SysOperationLog::getUsername, username)
                        .eq(StringUtils.hasText(module), SysOperationLog::getModule, module)
                        .eq(status != null, SysOperationLog::getStatus, status)
                        .orderByDesc(SysOperationLog::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public List<String> modules() {
        return operationMapper.selectList(Wrappers.<SysOperationLog>lambdaQuery()
                        .select(SysOperationLog::getModule))
                .stream().map(SysOperationLog::getModule).filter(StringUtils::hasText)
                .distinct().collect(Collectors.toList());
    }

    public PageResult<SysLoginLog> loginPage(PageQuery query, String username, Integer status) {
        Page<SysLoginLog> page = loginLogMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<SysLoginLog>lambdaQuery()
                        .like(StringUtils.hasText(username), SysLoginLog::getUsername, username)
                        .eq(status != null, SysLoginLog::getStatus, status)
                        .orderByDesc(SysLoginLog::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public void saveLoginLog(String username, boolean success, String msg, String ip) {
        SysLoginLog entry = new SysLoginLog();
        entry.setUsername(username);
        entry.setIp(ip);
        entry.setStatus(success ? 1 : 0);
        entry.setMsg(msg);
        entry.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(entry);
    }

    public void saveOperationLog(SysOperationLog entry) {
        operationMapper.insert(entry);
    }

    // ---------------- 安全配置 ----------------

    public List<SysConfig> configList() {
        return configMapper.selectList(Wrappers.<SysConfig>lambdaQuery()
                .likeRight(SysConfig::getConfigKey, "security.")
                .orderByAsc(SysConfig::getId));
    }

    @Transactional
    public void updateConfig(Map<String, String> values) {
        values.forEach((key, value) -> {
            if (!key.startsWith("security.")) {
                throw new BusinessException("仅允许修改 security.* 配置");
            }
            configMapper.update(null, Wrappers.<SysConfig>lambdaUpdate()
                    .set(SysConfig::getConfigValue, value)
                    .eq(SysConfig::getConfigKey, key));
        });
    }

    // ---------------- 备份与恢复（Java 原生导出） ----------------

    public PageResult<SysBackup> backupPage(PageQuery query) {
        Page<SysBackup> page = backupMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<SysBackup>lambdaQuery().orderByDesc(SysBackup::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    /**
     * 手动备份：逐表 DROP/CREATE + INSERT 生成 SQL 文件（dump 排除 sys_backup 自身）
     */
    @Transactional
    public Map<String, Object> backup() throws IOException {
        String dir = "./backups";
        Files.createDirectories(Paths.get(dir));
        String fileName = "backup_" + LocalDateTime.now().format(TS_FMT) + ".sql";
        Path file = Paths.get(dir, fileName);

        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name <> 'sys_backup'",
                String.class);

        StringBuilder sql = new StringBuilder("-- medical backup " + LocalDateTime.now() + "\n");
        sql.append("SET FOREIGN_KEY_CHECKS=0;\n");
        for (String table : tables) {
            sql.append("DROP TABLE IF EXISTS `").append(table).append("`;\n");
            String ddl = jdbcTemplate.queryForObject(
                    "SHOW CREATE TABLE `" + table + "`", (rs, i) -> rs.getString(2));
            sql.append(ddl.replace("\n", " ")).append(";\n");
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM `" + table + "`");
            for (Map<String, Object> row : rows) {
                String cols = row.keySet().stream().map(c -> "`" + c + "`").collect(Collectors.joining(","));
                String vals = row.values().stream().map(v -> {
                    if (v == null) return "NULL";
                    if (v instanceof Number || v instanceof Boolean) return v.toString();
                    return "'" + v.toString().replace("\\", "\\\\").replace("'", "\\'")
                            .replace("\r", "\\r").replace("\n", "\\n") + "'";
                }).collect(Collectors.joining(","));
                sql.append("INSERT INTO `").append(table).append("` (").append(cols)
                        .append(") VALUES (").append(vals).append(");\n");
            }
        }
        sql.append("SET FOREIGN_KEY_CHECKS=1;\n");
        Files.writeString(file, sql.toString(), StandardCharsets.UTF_8);
        long size = Files.size(file);

        SysBackup backup = new SysBackup();
        backup.setBackupName(fileName.replace(".sql", ""));
        backup.setBackupPath(file.toString());
        backup.setBackupSize(size);
        backup.setBackupType(1);
        backup.setStatus(1);
        backupMapper.insert(backup);
        return Map.of("id", backup.getId(), "file", fileName, "size", size);
    }

    /**
     * 恢复：逐条执行备份文件（全库回滚语义；必带 confirm 防呆）
     */
    @Transactional
    public void restore(Long id, boolean confirm) throws IOException {
        if (!confirm) {
            throw new BusinessException("恢复为全库回滚操作，需显式确认");
        }
        SysBackup backup = backupMapper.selectById(id);
        if (backup == null) {
            throw new BusinessException("备份记录不存在");
        }
        Path file = Paths.get(backup.getBackupPath());
        if (!Files.exists(file)) {
            throw new BusinessException("备份文件已丢失");
        }
        for (String statement : Files.readString(file, StandardCharsets.UTF_8).split(";\n")) {
            String s = statement.trim();
            if (!s.isEmpty() && !s.startsWith("--")) {
                jdbcTemplate.execute(s);
            }
        }
    }

    /**
     * 删除备份：逻辑删记录 + 物理删文件（文件丢失容忍）
     */
    @Transactional
    public void deleteBackup(Long id) {
        SysBackup backup = backupMapper.selectById(id);
        if (backup == null) {
            throw new BusinessException("备份记录不存在");
        }
        try {
            Files.deleteIfExists(Paths.get(backup.getBackupPath()));
        } catch (IOException e) {
            log.warn("备份文件删除失败（容忍）：{}", backup.getBackupPath());
        }
        backupMapper.deleteById(id);
    }
}
