package com.medical.modules.inpatient.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_admission")
public class BizAdmission {

    public static final int ST_IN = 0;
    public static final int ST_DISCHARGED = 1;
    public static final int ST_TRANSFERRED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String admissionNo;

    private Long patientId;

    private String patientName;

    private Long deptId;

    private Long bedId;

    private Long doctorId;

    private Long nurseId;

    private LocalDateTime admissionTime;

    private LocalDateTime dischargeTime;

    private String diagnosis;

    private BigDecimal deposit;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
