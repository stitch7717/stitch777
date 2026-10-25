package com.medical.modules.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_prescription")
public class BizPrescription {

    /** 待审核 */
    public static final int ST_PENDING = 0;
    /** 已审核 */
    public static final int ST_AUDITED = 1;
    /** 已收费 */
    public static final int ST_CHARGED = 2;
    /** 已发药 */
    public static final int ST_DISPENSED = 3;
    /** 已退 */
    public static final int ST_RETURNED = 4;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String prescNo;

    private Long patientId;

    private Long doctorId;

    /** 就诊ID = 挂号记录ID */
    private Long visitId;

    /** 1西药 2中药 3中成药 */
    private Integer prescType;

    private String diagnosis;

    private BigDecimal totalAmount;

    private Integer status;

    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
