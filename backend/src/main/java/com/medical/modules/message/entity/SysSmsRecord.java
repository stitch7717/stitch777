package com.medical.modules.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_sms_record")
public class SysSmsRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;

    private String templateCode;

    private String content;

    /** 0失败 1成功 */
    private Integer sendStatus;

    private LocalDateTime sendTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
