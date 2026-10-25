package com.medical.modules.pharmacy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_supplier")
public class BizSupplier {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;

    private String contact;

    private String phone;

    private String address;

    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
