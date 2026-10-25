package com.medical.modules.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_charge_item")
public class BizChargeItem {

    /** 药品 */
    public static final int TYPE_DRUG = 1;
    /** 诊疗（含挂号费） */
    public static final int TYPE_TREAT = 2;
    /** 检查 */
    public static final int TYPE_EXAM = 3;
    /** 床位 */
    public static final int TYPE_BED = 4;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long chargeId;

    private Integer itemType;

    private String itemName;

    /** 处方/来源单 ID（退费精确回退） */
    private Long sourceId;

    private Integer quantity;

    private BigDecimal price;

    private BigDecimal amount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
