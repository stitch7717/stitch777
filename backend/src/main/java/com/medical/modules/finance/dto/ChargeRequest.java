package com.medical.modules.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 收费请求：payType 1现金 2微信 3支付宝 4医保 5银行卡 */
@Data
public class ChargeRequest {

    @NotNull(message = "就诊不能为空")
    private Long visitId;

    @NotNull(message = "支付方式不能为空")
    private Integer payType;
}
