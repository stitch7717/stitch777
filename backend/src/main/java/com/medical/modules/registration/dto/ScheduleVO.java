package com.medical.modules.registration.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 排班列表行（聚合医生/科室名称）
 */
@Data
public class ScheduleVO {

    private Long id;
    private Long doctorId;
    private String doctorName;
    private Long deptId;
    private String deptName;
    private String scheduleDate;
    private Integer timePeriod;
    private Integer regType;
    private Integer totalNum;
    private Integer remainNum;
    private BigDecimal regFee;
    private Integer status;
}
