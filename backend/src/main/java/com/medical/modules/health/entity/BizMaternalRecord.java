package com.medical.modules.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_maternal_record")
public class BizMaternalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;

    private LocalDate lastPeriodDate;

    private LocalDate expectedDate;

    private Integer checkupCount;

    /** 1孕中 2已分娩 */
    private Integer status;

    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
