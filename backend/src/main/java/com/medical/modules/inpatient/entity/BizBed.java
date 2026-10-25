package com.medical.modules.inpatient.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_bed")
public class BizBed {

    public static final int ST_FREE = 0;
    public static final int ST_OCCUPIED = 1;
    public static final int ST_REPAIR = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long wardId;

    private String bedNo;

    private Integer bedType;

    private BigDecimal price;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
