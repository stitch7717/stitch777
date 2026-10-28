package com.medical.modules.registration.service;

import com.medical.common.BusinessException;
import com.medical.framework.LoginUser;
import com.medical.modules.patient.entity.BizPatient;
import com.medical.modules.patient.mapper.PatientMapper;
import com.medical.modules.registration.dto.RegisterRequest;
import com.medical.modules.registration.entity.BizSchedule;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 挂号并发：验证唯一约束 + 排队号原子自增在真实数据库上的行为。
 * 依赖 medical_test（scripts/init-test-db.sh 初始化）。
 */
@SpringBootTest
@ActiveProfiles("test")
class RegistrationConcurrencyTest {

    private static final int THREADS = 20;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private PatientMapper patientMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long scheduleId;
    private final List<Long> createdPatientIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        BizSchedule schedule = new BizSchedule();
        schedule.setDoctorId(2L);
        schedule.setDeptId(1L);
        schedule.setScheduleDate(LocalDate.now().plusDays(2));
        schedule.setTimePeriod(3);
        schedule.setRegType(1);
        schedule.setTotalNum(1000);
        schedule.setRemainNum(1000);
        schedule.setRegFee(new BigDecimal("20.00"));
        schedule.setStatus(1);
        scheduleMapper.insert(schedule);
        scheduleId = schedule.getId();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM biz_registration WHERE schedule_id = ?", scheduleId);
        jdbcTemplate.update("DELETE FROM biz_schedule WHERE id = ?", scheduleId);
        createdPatientIds.forEach(id -> jdbcTemplate.update("DELETE FROM biz_patient WHERE id = ?", id));
        createdPatientIds.clear();
    }

    @Test
    @DisplayName("同一患者并发挂号 20 次：仅 1 次成功，其余返回 409，号源只扣 1")
    void samePatient_concurrentRegister_onlyOneSucceeds() throws Exception {
        Long patientId = newPatient("并发测试患者A");

        Map<String, Object> result = runConcurrently(java.util.Collections.nCopies(THREADS, patientId));

        assertThat(result.get("success")).isEqualTo(1);
        assertThat(result.get("conflict")).isEqualTo(THREADS - 1);

        Integer registrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_registration WHERE patient_id = ? AND schedule_id = ? AND deleted = 0",
                Integer.class, patientId, scheduleId);
        assertThat(registrations).isEqualTo(1);

        Integer remain = jdbcTemplate.queryForObject(
                "SELECT remain_num FROM biz_schedule WHERE id = ?", Integer.class, scheduleId);
        assertThat(remain).isEqualTo(999);
    }

    @Test
    @DisplayName("同一排班 20 个患者并发挂号：全部成功且排队号不重复")
    void differentPatients_concurrentRegister_allSucceedWithUniqueQueueNo() throws Exception {
        List<Long> patientIds = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            patientIds.add(newPatient("并发测试患者B" + i));
        }

        Map<String, Object> result = runConcurrently(patientIds);

        assertThat(result.get("success")).isEqualTo(THREADS);
        assertThat(result.get("conflict")).isEqualTo(0);

        @SuppressWarnings("unchecked")
        List<Integer> queues = (List<Integer>) result.get("queues");
        assertThat(queues).hasSize(THREADS);
        assertThat(queues).doesNotHaveDuplicates();

        Integer remain = jdbcTemplate.queryForObject(
                "SELECT remain_num FROM biz_schedule WHERE id = ?", Integer.class, scheduleId);
        assertThat(remain).isEqualTo(1000 - THREADS);
    }

    @Test
    @DisplayName("退号后同一患者可再次挂同一排班")
    void cancelledRegistration_canRegisterAgain() {
        Long patientId = newPatient("并发测试患者C");

        Map<String, Object> first = register(patientId);
        Long registrationId = jdbcTemplate.queryForObject(
                "SELECT id FROM biz_registration WHERE patient_id = ? AND schedule_id = ? AND deleted = 0",
                Long.class, patientId, scheduleId);
        jdbcTemplate.update("UPDATE biz_registration SET status = 3 WHERE id = ?", registrationId);

        Map<String, Object> second = register(patientId);

        assertThat(first.get("queueNo")).isNotNull();
        assertThat(second.get("queueNo")).isNotNull();
        assertThat(second.get("queueNo")).isNotEqualTo(first.get("queueNo"));
    }

    private Map<String, Object> register(Long patientId) {
        SecurityContextHolder.getContext().setAuthentication(authentication());
        try {
            RegisterRequest request = new RegisterRequest();
            request.setPatientId(patientId);
            request.setScheduleId(scheduleId);
            return registrationService.register(request);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Map<String, Object> runConcurrently(List<Long> patientIds) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(patientIds.size());
        CountDownLatch ready = new CountDownLatch(patientIds.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<int[]>> futures = new ArrayList<>();

        for (Long patientId : patientIds) {
            futures.add(pool.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(authentication());
                RegisterRequest request = new RegisterRequest();
                request.setPatientId(patientId);
                request.setScheduleId(scheduleId);
                ready.countDown();
                start.await();
                try {
                    Map<String, Object> response = registrationService.register(request);
                    return new int[]{200, (Integer) response.get("queueNo")};
                } catch (BusinessException e) {
                    return new int[]{e.getCode(), -1};
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }));
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        int success = 0;
        int conflict = 0;
        List<Integer> queues = new ArrayList<>();
        for (Future<int[]> future : futures) {
            int[] outcome = future.get(30, TimeUnit.SECONDS);
            if (outcome[0] == 200) {
                success++;
                queues.add(outcome[1]);
            } else if (outcome[0] == 409) {
                conflict++;
            }
        }
        pool.shutdown();

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("conflict", conflict);
        result.put("queues", queues);
        return result;
    }

    private Long newPatient(String name) {
        BizPatient patient = new BizPatient();
        patient.setName(name);
        patient.setGender(1);
        patient.setPhone("13900000000");
        patientMapper.insert(patient);
        createdPatientIds.add(patient.getId());
        return patient.getId();
    }

    private UsernamePasswordAuthenticationToken authentication() {
        LoginUser user = new LoginUser(1L, "admin", "系统管理员", 1L, "admin");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }
}
