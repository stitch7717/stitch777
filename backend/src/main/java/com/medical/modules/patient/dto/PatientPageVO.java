package com.medical.modules.patient.dto;

import lombok.Data;

/**
 * 患者列表行：手机号/身份证号脱敏（详情与编辑回显走 /{id} 原值）
 */
@Data
public class PatientPageVO {

    private Long id;
    private String patientNo;
    private String name;
    private Integer gender;
    private Integer age;
    private String birthDate;
    /** 138****0101 */
    private String phone;
    private String bloodType;
    private String cardNo;
    private Integer cardStatus;
    private String tags;
    private String createTime;
}
