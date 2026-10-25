package com.medical.modules.report.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.framework.LoginUser;
import com.medical.framework.SecurityUtils;
import com.medical.modules.report.entity.SysReportConfig;
import com.medical.modules.report.mapper.SysReportConfigMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 自定义报表：数据集白名单（绝不执行用户 SQL）
 * 列名/表名/运算符/聚合全部取自服务端注册表，LIMIT 500 兜底
 */
@Service
@RequiredArgsConstructor
public class CustomReportService {

    private final SysReportConfigMapper configMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /** 数据集白名单：dataset -> (展示名 -> 物理列) */
    private static final Map<String, Map<String, String>> DATASETS = Map.of(
            "charge", new LinkedHashMap<>(Map.of(
                    "收费单号", "charge_no", "患者姓名", "patient_name",
                    "收费类型", "charge_type", "支付方式", "pay_type", "支付状态", "pay_status",
                    "实收金额", "actual_amount", "收费时间", "charge_time")),
            "patient", new LinkedHashMap<>(Map.of(
                    "患者编号", "patient_no", "姓名", "name", "性别", "gender",
                    "血型", "blood_type", "手机号", "phone", "标签", "tags")));

    private static final Set<String> OPS = Set.of("=", "!=", "like", ">", "<", ">=", "<=");
    private static final Set<String> FUNCS = Set.of("count", "sum", "avg", "max", "min");

    public Map<String, Object> datasets() {
        return Map.of(
                "datasets", DATASETS.entrySet().stream()
                        .map(e -> Map.of("key", e.getKey(),
                                "fields", List.copyOf(e.getValue().keySet())))
                        .toList(),
                "ops", OPS, "funcs", FUNCS);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Map<String, Object> config) {
        String dataset = (String) config.get("dataset");
        Map<String, String> schema = DATASETS.get(dataset);
        if (schema == null) {
            throw new BusinessException("数据集不存在：" + dataset);
        }
        List<String> fields = (List<String>) config.getOrDefault("fields", List.of());
        if (fields.isEmpty()) {
            throw new BusinessException("请至少选择一个字段");
        }
        List<String> selectCols = new ArrayList<>();
        for (String f : fields) {
            String col = schema.get(f);
            if (col == null) {
                throw new BusinessException("字段不在白名单：" + f);
            }
            selectCols.add(col + " AS `" + f + "`");
        }

        Map<String, Object> orderBy = (Map<String, Object>) config.get("orderBy");
        String orderClause = "";
        if (orderBy != null && orderBy.get("field") != null) {
            String of = schema.get(String.valueOf(orderBy.get("field")));
            if (of != null) {
                String dir = "desc".equalsIgnoreCase(String.valueOf(orderBy.get("dir"))) ? "DESC" : "ASC";
                orderClause = " ORDER BY " + of + " " + dir;
            }
        }

        String where = buildWhere(schema, config);
        String sql = "SELECT " + String.join(", ", selectCols)
                + " FROM `biz_" + dataset + "` WHERE deleted=0" + where + orderClause + " LIMIT 500";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        return Map.of("sql", sql, "rows", rows, "total", rows.size());
    }

    private String esc(String v) {
        return v.replace("\\", "\\\\").replace("'", "''");
    }

    private String buildWhere(Map<String, String> schema, Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions =
                (List<Map<String, Object>>) config.getOrDefault("conditions", List.of());
        StringBuilder where = new StringBuilder();
        for (Map<String, Object> c : conditions) {
            String field = (String) c.get("field");
            String op = String.valueOf(c.get("op"));
            Object value = c.get("value");
            String col = schema.get(field);
            if (col == null || !OPS.contains(op) || value == null
                    || String.valueOf(value).isBlank()) {
                continue;
            }
            if (where.length() > 0) {
                where.append(" AND ");
            }
            String v = String.valueOf(value);
            if ("like".equals(op)) {
                where.append(col).append(" LIKE '%").append(esc(v)).append("%'");
            } else if (v.matches("-?\\d+(\\.\\d+)?")) {
                where.append(col).append(" ").append(op).append(" ").append(v);
            } else {
                where.append(col).append(" ").append(op).append(" '").append(esc(v)).append("'");
            }
        }
        return where.length() == 0 ? "" : " AND " + where;
    }

    /** 保存配置：query_sql 仅存展示用 SQL，执行一律走白名单构建 */
    @Transactional
    public Long save(Map<String, Object> body) {
        String name = (String) body.get("reportName");
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) body.get("config");
        if (!org.springframework.util.StringUtils.hasText(name)) {
            throw new BusinessException("报表名称必填");
        }
        if (config == null) {
            throw new BusinessException("报表配置不能为空");
        }
        Map<String, Object> preview = execute(config);
        LoginUser user = SecurityUtils.getCurrentUser();
        SysReportConfig rc = new SysReportConfig();
        rc.setReportName(name);
        rc.setReportType("custom");
        rc.setQuerySql((String) preview.get("sql"));
        try {
            rc.setParams(objectMapper.writeValueAsString(config));
        } catch (Exception e) {
            throw new BusinessException("配置序列化失败");
        }
        rc.setCreatorId(user.getId());
        configMapper.insert(rc);
        return rc.getId();
    }

    public PageResult<SysReportConfig> page(PageQuery query) {
        Page<SysReportConfig> page = configMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<SysReportConfig>lambdaQuery()
                        .eq(SysReportConfig::getReportType, "custom")
                        .orderByDesc(SysReportConfig::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> detail(Long id) {
        SysReportConfig rc = configMapper.selectById(id);
        if (rc == null) {
            throw new BusinessException("配置不存在");
        }
        Map<String, Object> config;
        try {
            config = rc.getParams() == null ? Map.of() : objectMapper.readValue(rc.getParams(), Map.class);
        } catch (Exception e) {
            config = Map.of();
        }
        return Map.of("id", rc.getId(), "reportName", rc.getReportName(), "config", config);
    }

    @Transactional
    public void delete(Long id) {
        if (configMapper.selectById(id) == null) {
            throw new BusinessException("配置不存在");
        }
        configMapper.deleteById(id);
    }
}
