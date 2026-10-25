package com.medical.modules.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_medical_record")
public class BizMedicalRecord {

    /** 草稿 */
    public static final int ST_DRAFT = 0;
    /** 已提交 */
    public static final int ST_SUBMITTED = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientId;

    /** 就诊流水号 = 挂号单号，upsert 键 */
    private String visitNo;

    /** 1门诊 2住院 */
    private Integer visitType;

    private String chiefComplaint;

    private String presentIllness;

    private String physicalExam;

    private String diagnosis;

    private String icdCode;

    private Long doctorId;

    private LocalDateTime visitTime;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
