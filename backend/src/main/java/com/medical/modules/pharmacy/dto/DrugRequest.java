package com.medical.modules.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DrugRequest {

    @NotBlank(message = "药品名称不能为空")
    private String drugName;

    private String genericName;

    private String spec;

    private String dosageForm;

    private String unit;

    private String manufacturer;

    private String approvalNo;

    @NotNull(message = "售价不能为空")
    private BigDecimal salePrice;

    private BigDecimal purchasePrice;

    private String category;

    private Integer isSpecial;
}
