package com.medical.modules.patient.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.common.crypto.IdCardHasher;
import com.medical.modules.patient.dto.PatientPageVO;
import com.medical.modules.patient.dto.PatientRequest;
import com.medical.modules.patient.entity.BizPatient;
import com.medical.modules.patient.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PatientService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PatientMapper patientMapper;
    private final com.medical.modules.audit.mapper.SysConfigMapper configMapper;

    /** 脱敏开关：security.data-mask=1 时列表手机号/身份证打码（详情/编辑回显保持原值） */
    private boolean maskEnabled() {
        try {
            var cfg = configMapper.selectOne(Wrappers.<com.medical.modules.audit.entity.SysConfig>lambdaQuery()
                    .eq(com.medical.modules.audit.entity.SysConfig::getConfigKey, "security.data-mask"));
            return cfg == null || "1".equals(cfg.getConfigValue());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 患者分页：keyword 对姓名/手机号 OR 模糊；tags FIND_IN_SET 精确匹配；
     * 列表手机号/身份证号脱敏
     */
    public PageResult<PatientPageVO> page(PageQuery query, Integer gender,
                                          Integer cardStatus, String tags) {
        String tag = StringUtils.hasText(tags) ? tags.trim() : null;
        Page<BizPatient> page = patientMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<BizPatient>lambdaQuery()
                        .and(StringUtils.hasText(query.getKeyword()), w -> w
                                .like(BizPatient::getName, query.getKeyword())
                                .or().like(BizPatient::getPhone, query.getKeyword()))
                        .eq(gender != null, BizPatient::getGender, gender)
                        .eq(cardStatus != null, BizPatient::getCardStatus, cardStatus)
                        .apply(StringUtils.hasText(tag), "FIND_IN_SET({0}, tags)", tag)
                        .orderByDesc(BizPatient::getId));

        List<PatientPageVO> rows = page.getRecords().stream().map(p -> {
            PatientPageVO vo = new PatientPageVO();
            vo.setId(p.getId());
            vo.setPatientNo(p.getPatientNo());
            vo.setName(p.getName());
            vo.setGender(p.getGender());
            vo.setAge(p.getBirthDate() == null ? null : Period.between(p.getBirthDate(), LocalDate.now()).getYears());
            vo.setBirthDate(p.getBirthDate() == null ? null : p.getBirthDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            vo.setPhone(maskPhone(p.getPhone()));
            vo.setBloodType(p.getBloodType());
            vo.setCardNo(p.getCardNo());
            vo.setCardStatus(p.getCardStatus());
            vo.setTags(p.getTags());
            vo.setCreateTime(p.getCreateTime() == null ? null : p.getCreateTime().format(FMT));
            return vo;
        }).toList();

        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }

    public BizPatient detail(Long id) {
        BizPatient patient = patientMapper.selectById(id);
        if (patient == null) {
            throw new BusinessException("患者不存在");
        }
        return patient;
    }

    /**
     * 现场登记：按手机号查重，已有档案直接复用，没有才新建
     */
    @Transactional
    public Map<String, Object> registerOrGet(PatientRequest request) {
        if (!org.springframework.util.StringUtils.hasText(request.getPhone())) {
            throw new BusinessException("手机号必填（用于识别已有档案）");
        }
        if (!org.springframework.util.StringUtils.hasText(request.getName())) {
            throw new BusinessException("姓名必填");
        }
        List<BizPatient> exist = patientMapper.selectList(Wrappers.<BizPatient>lambdaQuery()
                .eq(BizPatient::getPhone, request.getPhone().trim()));
        if (!exist.isEmpty()) {
            // 已有档案直接复用（同号多人时取首个，姓名不同提示）
            BizPatient p = exist.get(0);
            return Map.of("id", p.getId(), "patientNo", p.getPatientNo(),
                    "name", p.getName(), "created", false);
        }
        Long id = create(request);
        BizPatient p = patientMapper.selectById(id);
        return Map.of("id", id, "patientNo", p.getPatientNo(),
                "name", p.getName(), "created", true);
    }

    @Transactional
    public Long create(PatientRequest request) {
        checkIdCardDuplicate(request.getIdCard(), null);
        BizPatient patient = new BizPatient();
        applyRequest(patient, request);
        patient.setCardStatus(1);
        patient.setCardBalance(java.math.BigDecimal.ZERO);
        patientMapper.insert(patient);

        // 编号先插后回补：患者编号 P%05d、就诊卡号 C1%04d（同收费单先例）
        BizPatient patch = new BizPatient();
        patch.setId(patient.getId());
        patch.setPatientNo(String.format("P%05d", patient.getId()));
        patch.setCardNo(String.format("C1%04d", patient.getId()));
        patientMapper.updateById(patch);
        return patient.getId();
    }

    @Transactional
    public void update(Long id, PatientRequest request) {
        BizPatient exist = patientMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("患者不存在");
        }
        checkIdCardDuplicate(request.getIdCard(), id);
        applyRequest(exist, request);
        patientMapper.updateById(exist);
    }

    @Transactional
    public void delete(Long id) {
        if (patientMapper.selectById(id) == null) {
            throw new BusinessException("患者不存在");
        }
        patientMapper.deleteById(id);
    }

    /** 挂失 */
    @Transactional
    public void loss(Long id) {
        BizPatient patient = requirePatient(id);
        if (patient.getCardStatus() != null && patient.getCardStatus() == 0) {
            throw new BusinessException("就诊卡已挂失");
        }
        BizPatient patch = new BizPatient();
        patch.setId(id);
        patch.setCardStatus(0);
        patientMapper.updateById(patch);
    }

    /** 补办：按签发轮次换新卡号（C1 首发 → C2/C3... 补办）并恢复正常状态 */
    @Transactional
    public String reissue(Long id) {
        BizPatient patient = requirePatient(id);
        if (patient.getCardStatus() == null || patient.getCardStatus() == 1) {
            throw new BusinessException("就诊卡状态正常，无需补办");
        }
        int round = 1;
        String old = patient.getCardNo();
        if (old != null && old.length() > 1 && Character.isDigit(old.charAt(1))) {
            round = Math.min(9, Character.getNumericValue(old.charAt(1)) + 1);
        }
        String newCardNo = String.format("C%d%04d", round, patient.getId());
        BizPatient patch = new BizPatient();
        patch.setId(id);
        patch.setCardNo(newCardNo);
        patch.setCardStatus(1);
        patientMapper.updateById(patch);
        return newCardNo;
    }

    private BizPatient requirePatient(Long id) {
        BizPatient patient = patientMapper.selectById(id);
        if (patient == null) {
            throw new BusinessException("患者不存在");
        }
        return patient;
    }

    /**
     * 按身份证查询：加密列每次密文不同，只能走 HMAC 检索列。
     */
    public BizPatient findByIdCard(String idCard) {
        String hash = IdCardHasher.hash(idCard);
        if (hash == null) {
            throw new BusinessException("身份证号不能为空");
        }
        BizPatient patient = patientMapper.selectOne(Wrappers.<BizPatient>lambdaQuery()
                .eq(BizPatient::getIdCardHash, hash)
                .last("LIMIT 1"));
        if (patient == null) {
            throw new BusinessException("未找到该身份证对应的患者");
        }
        return patient;
    }

    private void checkIdCardDuplicate(String idCard, Long excludeId) {
        String hash = IdCardHasher.hash(idCard);
        if (hash == null) {
            return;
        }
        Long count = (long) patientMapper.selectCount(Wrappers.<BizPatient>lambdaQuery()
                .eq(BizPatient::getIdCardHash, hash)
                .ne(excludeId != null, BizPatient::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(409, "该身份证号已建档");
        }
    }

    private void applyRequest(BizPatient patient, PatientRequest request) {
        patient.setName(request.getName());
        patient.setGender(request.getGender());
        patient.setBirthDate(request.getBirthDate());
        patient.setIdCard(request.getIdCard());
        patient.setIdCardHash(IdCardHasher.hash(request.getIdCard()));
        patient.setPhone(request.getPhone());
        patient.setAddress(request.getAddress());
        patient.setBloodType(request.getBloodType());
        patient.setAllergyHistory(request.getAllergyHistory());
        patient.setMedicalHistory(request.getMedicalHistory());
        patient.setFamilyHistory(request.getFamilyHistory());
        patient.setTags(normalizeTags(request.getTags()));
    }

    /** 标签写入端归一化：去空白项、去重、逗号分隔（文档 §2.1 字典语义） */
    private String normalizeTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return null;
        }
        List<String> list = java.util.Arrays.stream(tags.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        return list.isEmpty() ? null : String.join(",", list);
    }

    private String maskPhone(String phone) {
        if (!maskEnabled()) {
            return phone;
        }
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
