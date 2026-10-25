package com.medical.modules.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_charge")
public class BizCharge {

    /** 挂号费 */
    public static final int TYPE_REG = 1;
    /** 门诊 */
    public static final int TYPE_CLINIC = 2;
    /** 住院（预留） */
    public static final int TYPE_INPATIENT = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String chargeNo;

    private Long patientId;

    private String patientName;

    private Long visitId;

    private Integer chargeType;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal actualAmount;

    /** 1现金 2微信 3支付宝 4医保 5银行卡 */
    private Integer payType;

    /** 0待支付 1已支付 2已退费 */
    private Integer payStatus;

    private Long operatorId;

    private LocalDateTime chargeTime;

    private LocalDateTime refundTime;

    private Long refundOperatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
