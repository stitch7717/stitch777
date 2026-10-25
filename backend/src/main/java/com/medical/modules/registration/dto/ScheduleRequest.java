package com.medical.modules.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ScheduleRequest {

    @NotNull(message = "医生不能为空")
    private Long doctorId;

    @NotNull(message = "科室不能为空")
    private Long deptId;

    @NotNull(message = "排班日期不能为空")
    private LocalDate scheduleDate;

    /** 1上午 2下午 3晚上 */
    @NotNull(message = "时段不能为空")
    private Integer timePeriod;

    /** 1普通 2专家 3急诊 */
    @NotNull(message = "号别不能为空")
    private Integer regType;

    @NotNull(message = "总号源不能为空")
    private Integer totalNum;

    @NotNull(message = "挂号费不能为空")
    private BigDecimal regFee;
}
