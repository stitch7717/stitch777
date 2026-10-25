package com.medical.modules.pharmacy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_purchase_order")
public class BizPurchaseOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;

    private Long supplierId;

    private Long drugId;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal total;

    /** 0待入库 1已入库 2已取消 */
    private Integer status;

    private Long operatorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
