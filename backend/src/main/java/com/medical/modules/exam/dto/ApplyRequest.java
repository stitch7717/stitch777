package com.medical.modules.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 开检查申请 */
@Data
public class ApplyRequest {

    private Long registrationId;

    @NotNull(message = "患者不能为空")
    private Long patientId;

    /** 1检验 2检查(影像) */
    private Integer examType = 1;

    @NotBlank(message = "项目名称不能为空")
    private String examItem;

    private String bodyPart;

    private String clinicalDiagnosis;

    private BigDecimal fee;
}
