package com.medical.modules.clinic.dto;

import com.medical.modules.clinic.entity.BizPrescriptionItem;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PrescriptionVO {

    private Long id;
    private String prescNo;
    private Long visitId;
    private String patientName;
    private String doctorName;
    private Integer prescType;
    private String diagnosis;
    private BigDecimal totalAmount;
    private Integer status;
    private String createTime;
    private List<BizPrescriptionItem> items;
}
