package com.medical.modules.inpatient.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 护理记录请求 */
@Data
public class NursingRequest {

    @NotNull(message = "住院ID不能为空")
    private Long admissionId;

    private BigDecimal temperature;

    private Integer pulse;

    private Integer breath;

    private String bloodPressure;

    private String content;
}
