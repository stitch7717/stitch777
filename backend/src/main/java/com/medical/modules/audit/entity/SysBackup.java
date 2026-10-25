package com.medical.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_backup")
public class SysBackup {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String backupName;

    private String backupPath;

    private Long backupSize;

    /** 1手动 2自动 */
    private Integer backupType;

    /** 0失败 1成功 */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
