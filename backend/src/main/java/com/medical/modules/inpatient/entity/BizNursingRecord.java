package com.medical.modules.inpatient.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_nursing_record")
public class BizNursingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long admissionId;

    private LocalDateTime recordTime;

    private BigDecimal temperature;

    private Integer pulse;

    private Integer breath;

    private String bloodPressure;

    private String content;

    private Long nurseId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
