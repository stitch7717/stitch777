package com.medical.modules.registration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotNull(message = "患者不能为空")
    private Long patientId;

    @NotNull(message = "排班不能为空")
    private Long scheduleId;
}
