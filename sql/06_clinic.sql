-- ============================================================
-- 医疗后台管理系统 · 门诊管理模块（阶段3）
-- 药品目录（模块六提前建表供开处方）+ 门诊病历 + 处方
-- 金额由服务端按药品当刻售价快照计算（防篡改）
-- ============================================================
USE medical;

-- ------------------------------------------------------------
-- 药品表 biz_drug（接口文档 §7.1）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_drug (
  id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  drug_code      VARCHAR(30)   DEFAULT NULL COMMENT '药品编码',
  drug_name      VARCHAR(100)  NOT NULL COMMENT '药品名称',
  generic_name   VARCHAR(100)  DEFAULT NULL COMMENT '通用名',
  spec           VARCHAR(50)   DEFAULT NULL COMMENT '规格',
  dosage_form    VARCHAR(30)   DEFAULT NULL COMMENT '剂型',
  unit           VARCHAR(20)   DEFAULT NULL COMMENT '单位',
  manufacturer   VARCHAR(100)  DEFAULT NULL COMMENT '生产厂家',
  approval_no    VARCHAR(50)   DEFAULT NULL COMMENT '批准文号',
  purchase_price DECIMAL(10,2) DEFAULT 0.00 COMMENT '进价',
  sale_price     DECIMAL(10,2) NOT NULL COMMENT '售价',
  category       VARCHAR(50)   DEFAULT NULL COMMENT '分类',
  is_special     TINYINT       DEFAULT 0 COMMENT '是否特殊药品 0否1是',
  status         TINYINT       DEFAULT 1 COMMENT '状态 0停用1启用',
  create_time    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted        TINYINT       DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_drug_code (drug_code)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品表';

-- ------------------------------------------------------------
-- 病历表 biz_medical_record（接口文档 §3.1；visit_no=挂号单号，upsert）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_medical_record (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  patient_id      BIGINT       NOT NULL COMMENT '患者ID',
  visit_no        VARCHAR(30)  NOT NULL COMMENT '就诊流水号（=挂号单号）',
  visit_type      TINYINT      DEFAULT 1 COMMENT '1门诊2住院',
  chief_complaint VARCHAR(500) DEFAULT NULL COMMENT '主诉',
  present_illness TEXT         DEFAULT NULL COMMENT '现病史',
  physical_exam   TEXT         DEFAULT NULL COMMENT '体格检查',
  diagnosis       VARCHAR(500) DEFAULT NULL COMMENT '诊断',
  icd_code        VARCHAR(20)  DEFAULT NULL COMMENT 'ICD编码',
  doctor_id       BIGINT       NOT NULL COMMENT '医生ID',
  visit_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '就诊时间',
  status          TINYINT      DEFAULT 0 COMMENT '0草稿1已提交',
  create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted         TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_visit_no (visit_no)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '病历表';

-- ------------------------------------------------------------
-- 处方表 + 明细表（接口文档 §5.1；visit_id=挂号记录ID）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_prescription (
  id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  presc_no     VARCHAR(30)   DEFAULT NULL COMMENT '处方号 PR+日期+序号',
  patient_id   BIGINT        NOT NULL COMMENT '患者ID',
  doctor_id    BIGINT        NOT NULL COMMENT '医生ID',
  visit_id     BIGINT        NOT NULL COMMENT '就诊ID（挂号记录ID）',
  presc_type   TINYINT       DEFAULT 1 COMMENT '1西药2中药3中成药',
  diagnosis    VARCHAR(500)  DEFAULT NULL COMMENT '诊断',
  total_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '总金额（服务端按售价快照计算）',
  status       TINYINT       DEFAULT 0 COMMENT '0待审核1已审核2已收费3已发药4已退',
  create_time  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '开方时间',
  update_time  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      TINYINT       DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_presc_no (presc_no),
  KEY idx_visit (visit_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '处方表';

CREATE TABLE IF NOT EXISTS biz_prescription_item (
  id        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  presc_id  BIGINT        NOT NULL COMMENT '处方ID',
  drug_id   BIGINT        NOT NULL COMMENT '药品ID',
  drug_name VARCHAR(100)  DEFAULT NULL COMMENT '药品名称快照',
  spec      VARCHAR(50)   DEFAULT NULL COMMENT '规格快照',
  `usage`   VARCHAR(100)  DEFAULT NULL COMMENT '用法',
  dosage    VARCHAR(50)   DEFAULT NULL COMMENT '用量',
  frequency VARCHAR(20)   DEFAULT NULL COMMENT '频次',
  days      INT           DEFAULT NULL COMMENT '天数',
  quantity  INT           NOT NULL COMMENT '数量',
  price     DECIMAL(10,2) NOT NULL COMMENT '单价快照',
  amount    DECIMAL(10,2) NOT NULL COMMENT '小计',
  PRIMARY KEY (id),
  KEY idx_presc (presc_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '处方明细表';

-- 药品种子（常用口服药）
INSERT INTO biz_drug (drug_code, drug_name, generic_name, spec, dosage_form, unit, manufacturer, sale_price, purchase_price, category) VALUES
('D00001', '阿莫西林胶囊', '阿莫西林', '0.25g*24粒', '胶囊剂', '盒', '华北制药', 12.50, 8.00, '抗生素'),
('D00002', '布洛芬缓释胶囊', '布洛芬', '0.3g*20粒', '缓释胶囊', '盒', '中美史克', 18.00, 12.00, '解热镇痛'),
('D00003', '复方甘草片', '复方甘草', '100片', '片剂', '瓶', '太极集团', 6.50, 4.00, '止咳化痰'),
('D00004', '奥美拉唑肠溶胶囊', '奥美拉唑', '20mg*14粒', '肠溶胶囊', '盒', '阿斯利康', 25.00, 17.00, '消化系统'),
('D00005', '0.9%氯化钠注射液', '氯化钠', '500ml', '注射液', '瓶', '科伦药业', 3.20, 2.00, '输液')
ON DUPLICATE KEY UPDATE drug_name = VALUES(drug_name);

-- ------------------------------------------------------------
-- 菜单：门诊工作台（parent_id=4 门诊管理）+ 按钮
-- ------------------------------------------------------------
INSERT INTO sys_menu (id, menu_name, parent_id, path, perms, icon, menu_type, sort, status) VALUES
(27, '门诊工作台', 4, 'console', 'clinic:list',      'Platform', 2, 1, 1),
(28, '病历书写',   27, '#',       'clinic:record',    NULL, 3, 1, 1),
(29, '开处方',     27, '#',       'clinic:prescribe', NULL, 3, 2, 1),
(30, '处方审核',   27, '#',       'clinic:audit',     NULL, 3, 3, 1);

-- 角色分配：admin 全量；doctor 诊疗书写全量；dean 只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (27, 28, 29, 30)
WHERE r.role_code IN ('admin', 'doctor');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 27 FROM sys_role r WHERE r.role_code = 'dean';
