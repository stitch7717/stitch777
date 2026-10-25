package com.medical.modules.inpatient.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 床位列表行（聚合在院患者） */
@Data
public class BedVO {

    private Long id;
    private Long wardId;
    private String wardName;
    private String bedNo;
    private Integer bedType;
    private BigDecimal price;
    private Integer status;
    /** 占用时聚合 */
    private Long admissionId;
    private Long patientId;
    private String patientName;
    private String diagnosis;
    private String admissionTime;
    private String doctorName;
}
