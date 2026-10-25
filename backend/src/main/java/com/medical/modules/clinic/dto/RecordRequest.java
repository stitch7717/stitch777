package com.medical.modules.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 病历保存：按就诊流水号（visit_no = 挂号单号）upsert，提交时联动就诊完成
 */
@Data
public class RecordRequest {

    @NotBlank(message = "就诊流水号不能为空")
    private String visitNo;

    private String chiefComplaint;

    private String presentIllness;

    private String physicalExam;

    private String diagnosis;

    private String icdCode;

    /** true=提交（联动就诊完成），false=草稿 */
    @NotNull(message = "提交标记不能为空")
    private Boolean submit;
}
