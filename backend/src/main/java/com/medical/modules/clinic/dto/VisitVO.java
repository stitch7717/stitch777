package com.medical.modules.clinic.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 就诊列表行（数据源：当日挂号）
 */
@Data
public class VisitVO {

    private Long id;
    private String regNo;
    private Long patientId;
    private String patientName;
    private Integer gender;
    private Integer age;
    private Long deptId;
    private String deptName;
    private Long doctorId;
    private String doctorName;
    private Integer queueNo;
    private Integer regType;
    /** 0待就诊 1就诊中 2已完成 3已退号 */
    private Integer status;
    private String allergyHistory;
    private String regTime;
}
