package com.medical.modules.inpatient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 入院请求 */
@Data
public class AdmissionRequest {

    @NotNull(message = "患者不能为空")
    private Long patientId;

    @NotNull(message = "病区不能为空")
    private Long deptId;

    @NotNull(message = "床位不能为空")
    private Long bedId;

    private Long doctorId;

    private Long nurseId;

    private String diagnosis;

    private BigDecimal deposit;
}
