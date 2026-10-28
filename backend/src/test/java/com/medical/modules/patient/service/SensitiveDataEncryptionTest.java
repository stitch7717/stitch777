package com.medical.modules.patient.service;

import com.medical.common.BusinessException;
import com.medical.config.EncryptTypeHandler;
import com.medical.modules.patient.dto.PatientRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SensitiveDataEncryptionTest {

    private static final String ID_CARD = "440300199001019999";

    @Autowired
    private PatientService patientService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> createdIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        createdIds.forEach(id -> jdbcTemplate.update("DELETE FROM biz_patient WHERE id = ?", id));
        createdIds.clear();
    }

    @Test
    @DisplayName("AES-256-GCM 加解密往返，同一明文两次密文不同")
    void encryptDecrypt_roundTripAndRandomIv() {
        String first = EncryptTypeHandler.encrypt(ID_CARD);
        String second = EncryptTypeHandler.encrypt(ID_CARD);

        assertThat(first).startsWith("ENC:");
        assertThat(second).startsWith("ENC:");
        assertThat(first).isNotEqualTo(second);
        assertThat(EncryptTypeHandler.decrypt(first)).isEqualTo(ID_CARD);
        assertThat(EncryptTypeHandler.decrypt(second)).isEqualTo(ID_CARD);
    }

    @Test
    @DisplayName("历史明文身份证读取时原样返回（迁移兼容）")
    void legacyPlaintext_isReturnedAsIs() {
        String plain = "440300199001010000";
        assertThat(EncryptTypeHandler.decrypt(plain)).isEqualTo(plain);
    }

    @Test
    @DisplayName("现场登记：同手机号复用已有档案")
    @SuppressWarnings("unchecked")
    void registerOrGet_reusesExistingArchive() {
        PatientRequest request = request("现场登记测试患者");
        request.setIdCard(null);
        request.setPhone("13700008888");

        Map<String, Object> first = patientService.registerOrGet(request);
        createdIds.add((Long) first.get("id"));

        Map<String, Object> second = patientService.registerOrGet(request);

        assertThat(first.get("created")).isEqualTo(true);
        assertThat(second.get("created")).isEqualTo(false);
        assertThat(second.get("id")).isEqualTo(first.get("id"));
    }

    @Test
    @DisplayName("建档：身份证加密入库、可按身份证查询、重复建档被拒")
    void createPatient_encryptsAndAllowsLookupByIdCard() {
        Long id = patientService.create(request("加密测试患者"));
        createdIds.add(id);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT id_card, id_card_hash FROM biz_patient WHERE id = ?", id);
        String stored = (String) row.get("id_card");
        String hash = (String) row.get("id_card_hash");

        assertThat(stored).startsWith("ENC:");
        assertThat(stored.length()).isGreaterThan(20);
        assertThat(hash).hasSize(64);

        assertThat(patientService.findByIdCard(ID_CARD).getId()).isEqualTo(id);
        assertThat(patientService.detail(id).getIdCard()).isEqualTo(ID_CARD);

        assertThatThrownBy(() -> patientService.create(request("重复患者")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已建档");
    }

    private PatientRequest request(String name) {
        PatientRequest request = new PatientRequest();
        request.setName(name);
        request.setGender(1);
        request.setBirthDate(LocalDate.of(1990, 1, 1));
        request.setIdCard(ID_CARD);
        request.setPhone("13700009999");
        return request;
    }
}
