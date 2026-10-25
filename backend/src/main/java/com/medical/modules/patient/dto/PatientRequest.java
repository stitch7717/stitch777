package com.medical.modules.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 患者新增/编辑请求体（编辑不携带卡信息，卡走专用接口）
 */
@Data
public class PatientRequest {

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotNull(message = "性别不能为空")
    private Integer gender;

    private LocalDate birthDate;

    private String idCard;

    private String phone;

    private String address;

    private String bloodType;

    private String allergyHistory;

    private String medicalHistory;

    private String familyHistory;

    /** 逗号分隔标签文本（前端由字典多选拼装） */
    private String tags;

    /** 仅编辑回显用，服务端忽略 */
    private BigDecimal cardBalance;
}
