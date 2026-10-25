-- ============================================================
-- 医疗后台管理系统 · 公共卫生模块（阶段5，接口文档 §十）
-- 一人一档在 Service 层校验（不加唯一键，逻辑删除后可重建）
-- 传染病上报 POST 直写 report_status=1
-- ============================================================
USE medical;

CREATE TABLE IF NOT EXISTS biz_health_record (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  patient_id      BIGINT       NOT NULL COMMENT '患者ID',
  record_no       VARCHAR(30)  DEFAULT NULL COMMENT '档案号 H%05d',
  chronic_disease VARCHAR(255) DEFAULT NULL COMMENT '慢病（逗号分隔）',
  smoking         TINYINT      DEFAULT 0 COMMENT '吸烟 0否1是',
  drinking        TINYINT      DEFAULT 0 COMMENT '饮酒 0否1是',
  exercise        VARCHAR(50)  DEFAULT NULL COMMENT '运动频率',
  create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '建档时间',
  update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted         TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_record_no (record_no)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '健康档案表';

CREATE TABLE IF NOT EXISTS biz_vaccination (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  patient_id     BIGINT       NOT NULL COMMENT '患者ID',
  vaccine_name   VARCHAR(100) NOT NULL COMMENT '疫苗名称',
  batch_no       VARCHAR(50)  DEFAULT NULL COMMENT '批号',
  dose_no        INT          DEFAULT 1 COMMENT '剂次',
  vaccinate_date DATE         DEFAULT NULL COMMENT '接种日期',
  operator_id    BIGINT       DEFAULT NULL COMMENT '接种人',
  next_date      DATE         DEFAULT NULL COMMENT '下次接种日期',
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted        TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  KEY idx_patient (patient_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '疫苗接种表';

CREATE TABLE IF NOT EXISTS biz_infectious_report (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  patient_id    BIGINT       NOT NULL COMMENT '患者ID',
  disease_name  VARCHAR(100) NOT NULL COMMENT '传染病名称',
  icd_code      VARCHAR(20)  DEFAULT NULL COMMENT 'ICD编码',
  report_date   DATE         DEFAULT NULL COMMENT '报告日期',
  reporter_id   BIGINT       DEFAULT NULL COMMENT '报告人',
  report_status TINYINT      DEFAULT 1 COMMENT '0待上报(预留)1已上报',
  cdc_response  VARCHAR(255) DEFAULT NULL COMMENT '疾控反馈',
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  KEY idx_patient (patient_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '传染病上报表';

CREATE TABLE IF NOT EXISTS biz_health_checkup (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  patient_id   BIGINT       NOT NULL COMMENT '患者ID',
  checkup_no   VARCHAR(30)  DEFAULT NULL COMMENT '体检单号 TJ%05d',
  checkup_date DATE         DEFAULT NULL COMMENT '体检日期',
  checkup_type VARCHAR(50)  DEFAULT NULL COMMENT '体检类型（入职/常规/老年）',
  exam_items   VARCHAR(500) DEFAULT NULL COMMENT '检查项目（逗号分隔）',
  exam_result  VARCHAR(500) DEFAULT NULL COMMENT '检查结果摘要',
  doctor_id    BIGINT       DEFAULT NULL COMMENT '检查医生',
  conclusion   VARCHAR(255) DEFAULT NULL COMMENT '体检结论',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_checkup_no (checkup_no),
  KEY idx_patient (patient_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '体检记录表';

-- 菜单：公共卫生（parent_id=11）：列表 58 + 按钮五件套 63-67（文档 §10.2⑥）
INSERT INTO sys_menu (id, menu_name, parent_id, path, perms, icon, menu_type, sort, status) VALUES
(58, '健康档案', 11, 'records', 'health:list',              'FirstAidKit', 2, 1, 1),
(63, '建档',     58, '#',       'health:record:add',        NULL, 3, 1, 1),
(64, '档案编辑', 58, '#',       'health:record:edit',       NULL, 3, 2, 1),
(65, '接种登记', 58, '#',       'health:vaccination:add',   NULL, 3, 3, 1),
(66, '传染病上报', 58, '#',     'health:infectious:report', NULL, 3, 4, 1),
(67, '体检登记', 58, '#',       'health:checkup:add',       NULL, 3, 5, 1);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (58, 63, 64, 65, 66, 67)
WHERE r.role_code IN ('admin', 'doctor', 'nurse');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 58 FROM sys_role r WHERE r.role_code = 'dean';
