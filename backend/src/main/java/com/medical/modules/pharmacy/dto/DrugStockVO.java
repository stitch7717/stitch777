package com.medical.modules.pharmacy.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 药品库存聚合行（药品全字段 + 库存字段 + stockStatus，与 /api/drug/page 同构）
 * stockStatus 优先级：近效期 > 库存预警 > 正常
 */
@Data
public class DrugStockVO {

    private Long id;
    private String drugCode;
    private String drugName;
    private String genericName;
    private String spec;
    private String dosageForm;
    private String unit;
    private String manufacturer;
    private String category;
    private BigDecimal salePrice;
    private BigDecimal purchasePrice;
    private Integer isSpecial;
    private Integer drugStatus;
    private Integer quantity;
    private Integer warnNum;
    private String produceDate;
    private String expireDate;
    /** normal / lowStock / nearExpire */
    private String stockStatus;
}
