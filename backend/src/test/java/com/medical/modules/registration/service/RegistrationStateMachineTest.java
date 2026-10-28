package com.medical.modules.registration.service;

import com.medical.common.BusinessException;
import com.medical.framework.LoginUser;
import com.medical.modules.patient.entity.BizPatient;
import com.medical.modules.patient.mapper.PatientMapper;
import com.medical.modules.registration.dto.RegisterRequest;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 挂号状态机：待就诊 → 就诊中 → 已完成；退号回补号源。
 */
@SpringBootTest
@ActiveProfiles("test")
class RegistrationStateMachineTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private PatientMapper patientMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long patientId;
    private Long scheduleId;
    private Long registrationId;

    @BeforeEach
    void setUp() {
        BizPatient patient = new BizPatient();
        patient.setName("状态机测试患者");
        patient.setGender(1);
        patient.setPhone("13800009999");
        patientMapper.insert(patient);
        patientId = patient.getId();

        BizSchedule schedule = new BizSchedule();
        schedule.setDoctorId(2L);
        schedule.setDeptId(998L);
        schedule.setScheduleDate(LocalDate.now());
        schedule.setTimePeriod(3);
        schedule.setRegType(1);
        schedule.setTotalNum(5);
        schedule.setRemainNum(5);
        schedule.setRegFee(new BigDecimal("20.00"));
        schedule.setStatus(1);
        scheduleMapper.insert(schedule);
        scheduleId = schedule.getId();

        SecurityContextHolder.getContext().setAuthentication(authentication());
        try {
            RegisterRequest request = new RegisterRequest();
            request.setPatientId(patientId);
            request.setScheduleId(scheduleId);
            registrationService.register(request);
        } finally {
            SecurityContextHolder.clearContext();
        }
        registrationId = jdbcTemplate.queryForObject(
                "SELECT id FROM biz_registration WHERE patient_id = ? AND schedule_id = ? AND deleted = 0",
                Long.class, patientId, scheduleId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM biz_registration WHERE dept_id = 998");
        jdbcTemplate.update("DELETE FROM biz_schedule WHERE id = ?", scheduleId);
        jdbcTemplate.update("DELETE FROM biz_patient WHERE id = ?", patientId);
    }

    @Test
    @DisplayName("叫号 → 完成：状态按 0 → 1 → 2 推进")
    void callThenFinish_updatesStatus() {
        registrationService.call(registrationId);
        assertStatus(BizRegistration.ST_IN_CONSULT);

        registrationService.finish(registrationId);
        assertStatus(BizRegistration.ST_FINISHED);

        assertThatThrownBy(() -> registrationService.finish(registrationId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(409));
    }

    @Test
    @DisplayName("已完成的挂号不能再次叫号，返回 409")
    void callFinishedRegistration_conflicts() {
        registrationService.call(registrationId);
        registrationService.finish(registrationId);

        assertThatThrownBy(() -> registrationService.call(registrationId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(409));
    }

    @Test
    @DisplayName("退号成功并将号源回补 1")
    void cancel_waitingRegistration_restoresSlot() {
        Integer before = jdbcTemplate.queryForObject(
                "SELECT remain_num FROM biz_schedule WHERE id = ?", Integer.class, scheduleId);
        assertThat(before).isEqualTo(4);

        registrationService.cancel(registrationId);

        assertStatus(BizRegistration.ST_CANCELLED);
        Integer after = jdbcTemplate.queryForObject(
                "SELECT remain_num FROM biz_schedule WHERE id = ?", Integer.class, scheduleId);
        assertThat(after).isEqualTo(5);
    }

    private void assertStatus(int expected) {
        Integer status = jdbcTemplate.queryForObject(
                "SELECT status FROM biz_registration WHERE id = ?", Integer.class, registrationId);
        assertThat(status).isEqualTo(expected);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        LoginUser user = new LoginUser(1L, "admin", "系统管理员", 1L, "admin");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }
}
