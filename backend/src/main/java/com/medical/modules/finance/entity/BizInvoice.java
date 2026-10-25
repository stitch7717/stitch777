package com.medical.modules.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_invoice")
public class BizInvoice {

    public static final int ST_NORMAL = 0;
    public static final int ST_VOIDED = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String invoiceNo;

    private Long chargeId;

    /** 1电子 2纸质 */
    private Integer invoiceType;

    private BigDecimal amount;

    /** 0正常 1作废 */
    private Integer status;

    private LocalDateTime printTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
