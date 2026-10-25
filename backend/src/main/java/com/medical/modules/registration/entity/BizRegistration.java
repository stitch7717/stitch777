package com.medical.modules.registration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_registration")
public class BizRegistration {

    /** 待就诊 */
    public static final int ST_WAITING = 0;
    /** 就诊中 */
    public static final int ST_IN_CONSULT = 1;
    /** 已完成 */
    public static final int ST_FINISHED = 2;
    /** 已退号 */
    public static final int ST_CANCELLED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String regNo;

    private Long patientId;

    /** 姓名快照（凭证语义） */
    private String patientName;

    private Long doctorId;

    private Long deptId;

    private Long scheduleId;

    private Integer regType;

    private BigDecimal regFee;

    /** 当日该科室排队号（退号不回收） */
    private Integer queueNo;

    private Integer status;

    private LocalDateTime regTime;

    private Long creatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
