package com.medical.modules.inpatient.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 住院详情（含费用清单预演） */
@Data
public class AdmissionDetailVO {

    private Long id;
    private String admissionNo;
    private Long patientId;
    private String patientName;
    private Long deptId;
    private String deptName;
    private String bedNo;
    private String doctorName;
    private String nurseName;
    private String diagnosis;
    private BigDecimal deposit;
    private Integer status;
    private String admissionTime;
    private String dischargeTime;
    private Long inDays;
    /** 费用预演：押金 + 床位费估算 + 药品医嘱清单（不做结算） */
    private BigDecimal depositPart;
    private BigDecimal bedFeeEstimate;
    private List<OrderVO> orders;
}
