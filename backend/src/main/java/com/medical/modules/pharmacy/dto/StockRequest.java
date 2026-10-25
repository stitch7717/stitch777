package com.medical.modules.pharmacy.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 入库/出库请求（type: 1入库 2出库）
 */
@Data
public class StockRequest {

    @NotNull(message = "药品不能为空")
    private Long drugId;

    /** 1入库 2出库 */
    @NotNull(message = "操作类型不能为空")
    private Integer type;

    @NotNull(message = "数量不能为空")
    private Integer quantity;

    private String batchNo;

    private LocalDate produceDate;

    private LocalDate expireDate;

    private String remark;
}
