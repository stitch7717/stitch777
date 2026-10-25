package com.medical.modules.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_exam_apply")
public class BizExamApply {

    /** 待缴费 */
    public static final int ST_UNPAID = 0;
    /** 待采样 */
    public static final int ST_SAMPLE_PENDING = 1;
    /** 检验中 */
    public static final int ST_TESTING = 2;
    /** 已出报告 */
    public static final int ST_REPORTED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String applyNo;

    private Long registrationId;

    private Long patientId;

    private Long doctorId;

    /** 1检验 2检查(影像) */
    private Integer examType;

    private String examItem;

    private String bodyPart;

    private String clinicalDiagnosis;

    private BigDecimal fee;

    private Integer status;

    private LocalDateTime applyTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
