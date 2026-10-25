package com.medical.modules.pharmacy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_stock_record")
public class BizStockRecord {

    /** 入库 */
    public static final int TYPE_IN = 1;
    /** 出库 */
    public static final int TYPE_OUT = 2;
    /** 盘点（预留） */
    public static final int TYPE_CHECK = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long drugId;

    private Integer recordType;

    private Integer quantity;

    private String batchNo;

    private Long operatorId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
