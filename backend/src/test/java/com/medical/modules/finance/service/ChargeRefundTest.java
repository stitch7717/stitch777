package com.medical.modules.finance.service;

import com.medical.common.BusinessException;
import com.medical.framework.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 退费边界：已发药的处方必须先退药，不能直接退费。
 */
@SpringBootTest
@ActiveProfiles("test")
class ChargeRefundTest {

    private static final String PRESC_NO = "TST-REFUND-PRESC";
    private static final String CHARGE_NO = "TST-REFUND-CHARGE";

    @Autowired
    private ChargeService chargeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long chargeId;

    @BeforeEach
    void setUp() {
        cleanTestData();

        jdbcTemplate.update("""
                INSERT INTO biz_prescription
                    (presc_no, patient_id, doctor_id, visit_id, presc_type, total_amount, status)
                VALUES (?, 1, 2, 9999, 1, 10.00, 3)
                """, PRESC_NO);
        Long prescId = jdbcTemplate.queryForObject(
                "SELECT id FROM biz_prescription WHERE presc_no = ?", Long.class, PRESC_NO);

        jdbcTemplate.update("""
                INSERT INTO biz_charge
                    (charge_no, patient_id, visit_id, charge_type, total_amount, actual_amount, pay_status)
                VALUES (?, 1, 9999, 2, 10.00, 10.00, 1)
                """, CHARGE_NO);
        chargeId = jdbcTemplate.queryForObject(
                "SELECT id FROM biz_charge WHERE charge_no = ?", Long.class, CHARGE_NO);

        jdbcTemplate.update("""
                INSERT INTO biz_charge_item
                    (charge_id, item_type, item_name, source_id, quantity, price, amount)
                VALUES (?, 1, '测试处方', ?, 1, 10.00, 10.00)
                """, chargeId, prescId);
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
    }

    @Test
    @DisplayName("处方已发药时退费返回 409，收费单保持已支付")
    void refundDispensedPrescription_returnsConflict() {
        SecurityContextHolder.getContext().setAuthentication(authentication());
        try {
            assertThatThrownBy(() -> chargeService.refund(chargeId))
                    .isInstanceOfSatisfying(BusinessException.class, e -> {
                        assertThat(e.getCode()).isEqualTo(409);
                        assertThat(e.getMessage()).contains("已发药");
                    });
        } finally {
            SecurityContextHolder.clearContext();
        }

        Integer payStatus = jdbcTemplate.queryForObject(
                "SELECT pay_status FROM biz_charge WHERE id = ?", Integer.class, chargeId);
        assertThat(payStatus).isEqualTo(1);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        LoginUser user = new LoginUser(1L, "admin", "系统管理员", 1L, "admin");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private void cleanTestData() {
        jdbcTemplate.update("""
                DELETE FROM biz_charge_item
                WHERE charge_id IN (SELECT id FROM biz_charge WHERE charge_no = ?)
                """, CHARGE_NO);
        jdbcTemplate.update("DELETE FROM biz_charge WHERE charge_no = ?", CHARGE_NO);
        jdbcTemplate.update("DELETE FROM biz_prescription WHERE presc_no = ?", PRESC_NO);
    }
}
