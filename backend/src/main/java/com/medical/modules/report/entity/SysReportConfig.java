package com.medical.modules.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_report_config")
public class SysReportConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String reportName;

    private String reportType;

    /** 展示用SQL（服务端白名单构建，不执行） */
    private String querySql;

    /** 设计器配置JSON */
    private String params;

    private Long creatorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
