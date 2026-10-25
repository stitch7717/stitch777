package com.medical.modules.registration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_schedule")
public class BizSchedule {

    /** 时段：上午 */
    public static final int PERIOD_AM = 1;
    /** 时段：下午 */
    public static final int PERIOD_PM = 2;
    /** 时段：晚上 */
    public static final int PERIOD_NIGHT = 3;

    /** 号别：普通 */
    public static final int TYPE_NORMAL = 1;
    /** 号别：专家 */
    public static final int TYPE_EXPERT = 2;
    /** 号别：急诊 */
    public static final int TYPE_EMERGENCY = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long doctorId;

    private Long deptId;

    private LocalDate scheduleDate;

    private Integer timePeriod;

    private Integer regType;

    private Integer totalNum;

    private Integer remainNum;

    private BigDecimal regFee;

    /** 0停诊 1正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
