package com.medical.modules.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_drug")
public class BizDrug {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String drugCode;

    private String drugName;

    private String genericName;

    private String spec;

    private String dosageForm;

    private String unit;

    private String manufacturer;

    private String approvalNo;

    private BigDecimal purchasePrice;

    private BigDecimal salePrice;

    private String category;

    private Integer isSpecial;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
