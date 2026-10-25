package com.medical.modules.patient.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_follow_up")
public class BizFollowUp {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientId;

    private LocalDate planDate;

    /** 1电话 2上门 3短信 */
    private Integer followType;

    private String content;

    private String result;

    private Long operatorId;

    /** 0待随访 1已完成 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
