package com.medical.modules.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_vaccination")
public class BizVaccination {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;

    private String vaccineName;

    private String batchNo;

    private Integer doseNo;

    private LocalDate vaccinateDate;

    private Long operatorId;

    private LocalDate nextDate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
