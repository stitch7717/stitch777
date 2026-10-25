package com.medical.modules.exam.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 申请列表行（聚合患者姓名） */
@Data
public class ApplyVO {

    private Long id;
    private String applyNo;
    private Long registrationId;
    private Long patientId;
    private String patientName;
    private String doctorName;
    private Integer examType;
    private String examItem;
    private String bodyPart;
    private String clinicalDiagnosis;
    private BigDecimal fee;
    private Integer status;
    private String applyTime;
    /** 报告摘要（已发布/草稿） */
    private Long reportId;
    private Integer isCritical;
    private Integer criticalHandled;
    private Integer reportStatus;
    private Long auditorId;
}
