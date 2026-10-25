package com.medical.modules.finance.dto;

import com.medical.modules.finance.entity.BizChargeItem;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ChargeVO {

    private Long id;
    private String chargeNo;
    private String patientName;
    private Integer chargeType;
    private BigDecimal totalAmount;
    private BigDecimal actualAmount;
    private Integer payType;
    private Integer payStatus;
    private String chargeTime;
    private List<BizChargeItem> items;
}
