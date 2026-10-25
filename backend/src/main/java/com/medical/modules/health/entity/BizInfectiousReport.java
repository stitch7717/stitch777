package com.medical.modules.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_infectious_report")
public class BizInfectiousReport {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;

    private String diseaseName;

    private String icdCode;

    private LocalDate reportDate;

    private Long reporterId;

    /** 0待上报(预留) 1已上报 */
    private Integer reportStatus;

    private String cdcResponse;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
