package com.medical.modules.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_child_care")
public class BizChildCare {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;

    private BigDecimal birthWeight;

    private BigDecimal height;

    private BigDecimal weight;

    private LocalDate checkDate;

    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
