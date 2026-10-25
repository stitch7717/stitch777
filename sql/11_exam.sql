-- ============================================================
-- 医疗后台管理系统 · 检查检验模块（阶段4，接口文档 §八）
-- 状态机：0待缴费 --收费--> 1待采样 --采样--> 2检验中 --发布--> 3已出报告
-- 报告：0草稿 --发布--> 1已发布（审核写 auditor_id 状态仍 0）
-- ============================================================
USE medical;

-- ------------------------------------------------------------
-- 检查申请表 biz_exam_apply
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_exam_apply (
  id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  apply_no           VARCHAR(30)  DEFAULT NULL COMMENT '申请单号 EXAM+日期+序号',
  registration_id    BIGINT       DEFAULT NULL COMMENT '就诊ID（挂号记录ID）',
  patient_id         BIGINT       NOT NULL COMMENT '患者ID',
  doctor_id          BIGINT       NOT NULL COMMENT '申请医生',
  exam_type          TINYINT      DEFAULT 1 COMMENT '1检验2检查(影像)',
  exam_item          VARCHAR(100) NOT NULL COMMENT '项目名称',
  body_part          VARCHAR(50)  DEFAULT NULL COMMENT '检查部位',
  clinical_diagnosis VARCHAR(500) DEFAULT NULL COMMENT '临床诊断',
  fee                DECIMAL(10,2) DEFAULT 0.00 COMMENT '检查定价',
  status             TINYINT      DEFAULT 0 COMMENT '0待缴费1待采样2检验中3已出报告',
  apply_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted            TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_apply_no (apply_no),
  KEY idx_patient (patient_id),
  KEY idx_status (status)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '检查申请表';

-- ------------------------------------------------------------
-- 检验报告 biz_exam_report（一申请一报告 uk_apply）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_exam_report (
  id                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  apply_id               BIGINT       NOT NULL COMMENT '申请ID',
  report_no              VARCHAR(30)  DEFAULT NULL COMMENT '报告号 RP+日期+序号',
  result                 TEXT         DEFAULT NULL COMMENT 'JSON明细数组 [{name,value,ref,unit,abnormal}]',
  conclusion             VARCHAR(500) DEFAULT NULL COMMENT '结论',
  is_critical            TINYINT      DEFAULT 0 COMMENT '危急值 0否1是',
  reporter_id            BIGINT       DEFAULT NULL COMMENT '报告人',
  auditor_id             BIGINT       DEFAULT NULL COMMENT '审核人（非空即已审核）',
  report_time            DATETIME     DEFAULT NULL COMMENT '发布时间',
  status                 TINYINT      DEFAULT 0 COMMENT '0草稿1已发布',
  attachment_url         VARCHAR(500) DEFAULT NULL COMMENT '附件URL（S3，扩展）',
  critical_handled       TINYINT      DEFAULT 0 COMMENT '危急值是否已处理',
  critical_handle_remark VARCHAR(500) DEFAULT NULL COMMENT '危急值处理备注',
  critical_handle_time   DATETIME     DEFAULT NULL COMMENT '危急值处理时间',
  critical_handler_id    BIGINT       DEFAULT NULL COMMENT '危急值处理人',
  create_time            DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time            DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted                TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_apply (apply_id),
  UNIQUE KEY uk_report_no (report_no)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '检验报告表';

-- ------------------------------------------------------------
-- 菜单：检查检验（parent_id=10）：列表 48 + 按钮三码 56-58（文档 §8.2⑤）
-- ------------------------------------------------------------
INSERT INTO sys_menu (id, menu_name, parent_id, path, perms, icon, menu_type, sort, status) VALUES
(48, '检验工作台', 10, 'lab', 'exam:list',          'DocumentChecked', 2, 1, 1),
(55, '检查开单',   48, '#',    'exam:apply:add',     NULL, 3, 1, 1),
(56, '采样录入',   48, '#',    'exam:report:add',    NULL, 3, 2, 1),
(57, '报告审核',   48, '#',    'exam:report:audit',  NULL, 3, 3, 1);

-- 角色分配：admin 全量；doctor 开单+审核；nurse 采样+录入；dean 只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (48, 55, 56, 57)
WHERE r.role_code = 'admin';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (48, 55, 57)
WHERE r.role_code = 'doctor';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (48, 56)
WHERE r.role_code = 'nurse';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 48 FROM sys_role r WHERE r.role_code = 'dean';
