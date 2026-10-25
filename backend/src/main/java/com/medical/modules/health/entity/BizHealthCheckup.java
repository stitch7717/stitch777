package com.medical.modules.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_health_checkup")
public class BizHealthCheckup {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;

    private String checkupNo;

    private LocalDate checkupDate;

    private String checkupType;

    private String examItems;

    private String examResult;

    private Long doctorId;

    private String conclusion;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
