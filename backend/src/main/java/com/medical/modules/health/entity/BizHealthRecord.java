package com.medical.modules.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_health_record")
public class BizHealthRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;

    private String recordNo;

    /** 慢病（逗号分隔） */
    private String chronicDisease;

    private Integer smoking;

    private Integer drinking;

    private String exercise;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
