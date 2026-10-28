package com.medical.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 以 app.backfill=true 启动上下文，验证历史明文身份证被加密并补齐 HMAC 检索列。
 */
@SpringBootTest(properties = "app.backfill=true")
@ActiveProfiles("test")
class IdCardBackfillTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("种子数据中的明文身份证全部回填为密文 + 64 位哈希")
    void seedRowsAreBackfilled() {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_patient WHERE deleted = 0 AND id_card IS NOT NULL AND id_card <> ''",
                Integer.class);
        Integer encrypted = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM biz_patient
                WHERE deleted = 0 AND id_card LIKE 'ENC:%'
                  AND id_card_hash IS NOT NULL AND CHAR_LENGTH(id_card_hash) = 64
                """, Integer.class);

        assertThat(total).isPositive();
        assertThat(encrypted).isEqualTo(total);
    }
}
