package com.medical.modules.clinic.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PrescriptionRequest {

    @NotNull(message = "就诊ID不能为空")
    private Long visitId;

    /** 1西药 2中药 3中成药 */
    private Integer prescType = 1;

    private String diagnosis;

    @NotEmpty(message = "处方明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "药品不能为空")
        private Long drugId;

        private String usage;

        private String dosage;

        private String frequency;

        private Integer days;

        @NotNull(message = "数量不能为空")
        private Integer quantity;
    }
}
