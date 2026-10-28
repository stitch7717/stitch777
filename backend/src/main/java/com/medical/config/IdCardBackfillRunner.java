package com.medical.config;

import com.medical.common.crypto.IdCardHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 一次性回填：把历史明文身份证加密，并补齐 HMAC 检索列。
 * 启用方式：启动参数加 --app.backfill=true（或 APP_BACKFILL=true）。
 * 可重复执行：已加密的行会解密后用新 IV 重新加密，哈希保持一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.backfill", havingValue = "true")
public class IdCardBackfillRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, id_card FROM biz_patient WHERE deleted = 0 AND id_card IS NOT NULL AND id_card <> ''");
        int updated = 0;
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            String plain = EncryptTypeHandler.decrypt((String) row.get("id_card"));
            String encrypted = EncryptTypeHandler.encrypt(plain);
            String hash = IdCardHasher.hash(plain);
            jdbcTemplate.update(
                    "UPDATE biz_patient SET id_card = ?, id_card_hash = ? WHERE id = ?",
                    encrypted, hash, id);
            updated++;
        }
        log.info("身份证字段回填完成：{} 条", updated);
    }
}
