package com.medical.modules.finance.service;

import com.medical.common.BusinessException;
import com.medical.framework.LoginUser;
import com.medical.modules.finance.dto.ChargeRequest;
import com.medical.modules.patient.entity.BizPatient;
import com.medical.modules.patient.mapper.PatientMapper;
import com.medical.modules.registration.entity.BizRegistration;
import com.medical.modules.registration.entity.BizSchedule;
import com.medical.modules.registration.mapper.RegistrationMapper;
import com.medical.modules.registration.mapper.ScheduleMapper;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 收费 / 退费并发：验证就诊行锁 + 条件更新只允许一次成功。
 */
@SpringBootTest
@ActiveProfiles("test")
class ChargeConcurrencyTest {

    private static final int THREADS = 20;
    private static final long TEST_DEPT_ID = 999L;

    @Autowired
    private ChargeService chargeService;

    @Autowired
    private PatientMapper patientMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long visitId;

    @BeforeEach
    void setUp() {
        cleanTestData();

        BizPatient patient = new BizPatient();
        patient.setName("收费并发测试患者");
        patient.setGender(1);
        patient.setPhone("13900000001");
        patientMapper.insert(patient);

        BizSchedule schedule = new BizSchedule();
        schedule.setDoctorId(2L);
        schedule.setDeptId(TEST_DEPT_ID);
        schedule.setScheduleDate(LocalDate.now());
        schedule.setTimePeriod(1);
        schedule.setRegType(1);
        schedule.setTotalNum(100);
        schedule.setRemainNum(100);
        schedule.setRegFee(new BigDecimal("20.00"));
        schedule.setStatus(1);
        scheduleMapper.insert(schedule);

        BizRegistration registration = new BizRegistration();
        registration.setPatientId(patient.getId());
        registration.setPatientName(patient.getName());
        registration.setDoctorId(2L);
        registration.setDeptId(TEST_DEPT_ID);
        registration.setScheduleId(schedule.getId());
        registration.setRegType(1);
        registration.setRegFee(new BigDecimal("20.00"));
        registration.setQueueNo(1);
        registration.setStatus(BizRegistration.ST_WAITING);
        registration.setRegTime(LocalDateTime.now());
        registration.setCreatorId(1L);
        registrationMapper.insert(registration);
        visitId = registration.getId();
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
    }

    @Test
    @DisplayName("仅挂号费的就诊并发收费 20 次：只生成 1 张收费单，其余 409")
    void concurrentCharge_onlyOneChargeCreated() throws Exception {
        Map<String, Integer> result = runConcurrently(() -> {
            ChargeRequest request = new ChargeRequest();
            request.setVisitId(visitId);
            request.setPayType(1);
            chargeService.charge(request);
            return 200;
        });

        assertThat(result.get("success")).isEqualTo(1);
        assertThat(result.get("conflict")).isEqualTo(THREADS - 1);
        Integer chargeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_charge WHERE visit_id = ? AND deleted = 0",
                Integer.class, visitId);
        assertThat(chargeCount).isEqualTo(1);
    }

    @Test
    @DisplayName("同一收费单并发退费 20 次：仅 1 次成功，其余 409")
    void concurrentRefund_onlyOneSucceeds() throws Exception {
        ChargeRequest request = new ChargeRequest();
        request.setVisitId(visitId);
        request.setPayType(1);
        SecurityContextHolder.getContext().setAuthentication(authentication());
        chargeService.charge(request);
        SecurityContextHolder.clearContext();

        Long chargeId = jdbcTemplate.queryForObject(
                "SELECT id FROM biz_charge WHERE visit_id = ? AND deleted = 0", Long.class, visitId);

        Map<String, Integer> result = runConcurrently(() -> {
            chargeService.refund(chargeId);
            return 200;
        });

        assertThat(result.get("success")).isEqualTo(1);
        assertThat(result.get("conflict")).isEqualTo(THREADS - 1);
        Integer refunded = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_charge WHERE id = ? AND pay_status = 2", Integer.class, chargeId);
        assertThat(refunded).isEqualTo(1);
    }

    private Map<String, Integer> runConcurrently(Callable<Integer> action) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            futures.add(pool.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(authentication());
                ready.countDown();
                start.await();
                try {
                    return action.call();
                } catch (BusinessException e) {
                    return e.getCode();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }));
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        int success = 0;
        int conflict = 0;
        for (Future<Integer> future : futures) {
            int code = future.get(30, TimeUnit.SECONDS);
            if (code == 200) {
                success++;
            } else if (code == 409) {
                conflict++;
            }
        }
        pool.shutdown();
        return Map.of("success", success, "conflict", conflict);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        LoginUser user = new LoginUser(1L, "admin", "系统管理员", 1L, "admin");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private void cleanTestData() {
        jdbcTemplate.update("""
                DELETE ci FROM biz_charge_item ci
                JOIN biz_charge c ON ci.charge_id = c.id
                JOIN biz_registration r ON c.visit_id = r.id
                WHERE r.dept_id = ?
                """, TEST_DEPT_ID);
        jdbcTemplate.update("""
                DELETE c FROM biz_charge c
                JOIN biz_registration r ON c.visit_id = r.id
                WHERE r.dept_id = ?
                """, TEST_DEPT_ID);
        jdbcTemplate.update("DELETE FROM biz_registration WHERE dept_id = ?", TEST_DEPT_ID);
        jdbcTemplate.update("DELETE FROM biz_schedule WHERE dept_id = ?", TEST_DEPT_ID);
        jdbcTemplate.update("DELETE FROM biz_patient WHERE name LIKE '收费并发测试患者%'");
    }
}
