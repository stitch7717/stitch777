package com.medical.modules.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 录入结果（草稿 upsert） */
@Data
public class ReportRequest {

    @NotNull(message = "申请ID不能为空")
    private Long applyId;

    private String conclusion;

    private Integer isCritical = 0;

    private String attachmentUrl;

    /** 明细行 [{name,value,ref,unit,abnormal}] */
    private List<ResultItem> items;

    @Data
    public static class ResultItem {
        private String name;
        private String value;
        private String ref;
        private String unit;
        private Integer abnormal;
    }
}
