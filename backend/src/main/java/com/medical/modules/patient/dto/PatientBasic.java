package com.medical.modules.patient.dto;

/**
 * 患者基础信息缓存值：只放挂号/接诊回填需要的字段，不放过敏史、病史等临床数据。
 */
public record PatientBasic(Long id, String patientNo, String name) {
}
