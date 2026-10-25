package com.medical.modules.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_notice")
public class SysNotice {

    public static final int TYPE_HOSPITAL = 1;
    public static final int TYPE_DEPT = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private Integer noticeType;

    private Long deptId;

    private Long publisherId;

    private LocalDateTime publishTime;

    /** 0草稿 1已发布 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
