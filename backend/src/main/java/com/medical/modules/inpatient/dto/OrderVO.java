package com.medical.modules.inpatient.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {

    private Long id;
    private Integer orderType;
    private String content;
    private String drugName;
    private BigDecimal drugPrice;
    private String dosage;
    private String usageMethod;
    private String frequency;
    private LocalDateTime startTime;
    private LocalDateTime stopTime;
    private String doctorName;
    private Integer status;
}
