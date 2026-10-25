package com.medical.modules.inpatient.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_medical_order")
public class BizMedicalOrder {

    public static final int ST_PENDING = 0;
    public static final int ST_EXECUTED = 1;
    public static final int ST_STOPPED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long admissionId;

    /** 1长期 2临时 */
    private Integer orderType;

    private String content;

    private Long drugId;

    private String dosage;

    private String usageMethod;

    private String frequency;

    private LocalDateTime startTime;

    private LocalDateTime stopTime;

    private Long doctorId;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
