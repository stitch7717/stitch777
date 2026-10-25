package com.medical.modules.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("biz_prescription_item")
public class BizPrescriptionItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long prescId;

    private Long drugId;

    private String drugName;

    private String spec;

    /** 用法（MySQL 保留字列，实体映射转义） */
    @TableField("`usage`")
    private String usage;

    private String dosage;

    private String frequency;

    private Integer days;

    private Integer quantity;

    private BigDecimal price;

    private BigDecimal amount;
}
