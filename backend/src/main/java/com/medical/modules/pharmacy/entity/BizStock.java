package com.medical.modules.pharmacy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_stock")
public class BizStock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long drugId;

    private Integer quantity;

    private Integer warnNum;

    private LocalDate produceDate;

    private LocalDate expireDate;

    private Long warehouseId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
