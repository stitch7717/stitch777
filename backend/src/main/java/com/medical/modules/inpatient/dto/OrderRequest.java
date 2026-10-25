package com.medical.modules.inpatient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 开医嘱请求 */
@Data
public class OrderRequest {

    @NotNull(message = "住院ID不能为空")
    private Long admissionId;

    /** 1长期 2临时 */
    private Integer orderType = 1;

    @NotBlank(message = "医嘱内容不能为空")
    private String content;

    private Long drugId;

    private String dosage;

    private String usageMethod;

    private String frequency;
}
