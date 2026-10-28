package com.medical.modules.patient.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.medical.config.EncryptTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName(value = "biz_patient", autoResultMap = true)
public class BizPatient {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String patientNo;

    private String name;

    private Integer gender;

    private LocalDate birthDate;

    @TableField(typeHandler = EncryptTypeHandler.class)
    private String idCard;

    /** 身份证 HMAC-SHA256 检索列（加密列无法直接等值查询） */
    private String idCardHash;

    private String phone;

    private String address;

    private String bloodType;

    private String allergyHistory;

    private String medicalHistory;

    private String familyHistory;

    private String cardNo;

    /** 0挂失 1正常 */
    private Integer cardStatus;

    private BigDecimal cardBalance;

    /** 逗号分隔标签文本（dict_value 同值） */
    private String tags;

    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
