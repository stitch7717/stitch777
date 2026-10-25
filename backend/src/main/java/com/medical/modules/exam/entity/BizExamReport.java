package com.medical.modules.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_exam_report")
public class BizExamReport {

    public static final int ST_DRAFT = 0;
    public static final int ST_PUBLISHED = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applyId;

    private String reportNo;

    /** JSON 明细数组 [{"name","value","ref","unit","abnormal"}] */
    private String result;

    private String conclusion;

    private Integer isCritical;

    private Long reporterId;

    /** 非空即已审核 */
    private Long auditorId;

    private LocalDateTime reportTime;

    private Integer status;

    private String attachmentUrl;

    private Integer criticalHandled;

    private String criticalHandleRemark;

    private LocalDateTime criticalHandleTime;

    private Long criticalHandlerId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
