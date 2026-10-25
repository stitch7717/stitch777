package com.medical.common;

import lombok.Data;

/**
 * 通用分页请求参数（接口文档 §1.1）
 */
@Data
public class PageQuery {

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String startTime;
    private String endTime;
}
