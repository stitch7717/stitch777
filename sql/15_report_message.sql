-- ============================================================
-- 医疗后台管理系统 · 消息通知模块（阶段4，接口文档 §十二）
-- MQ 消费链后续接入（本机无 RabbitMQ），收费成功由应用内同步写站内信
-- ============================================================
USE medical;

CREATE TABLE IF NOT EXISTS sys_message (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  receiver_id BIGINT       NOT NULL COMMENT '接收人ID（本人恒等）',
  title       VARCHAR(100) NOT NULL COMMENT '标题',
  content     TEXT         DEFAULT NULL COMMENT '内容',
  msg_type    TINYINT      DEFAULT 1 COMMENT '1系统2待办3预警(预留)',
  is_read     TINYINT      DEFAULT 0 COMMENT '0未读1已读',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  KEY idx_receiver (receiver_id, is_read)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站内消息表';

CREATE TABLE IF NOT EXISTS sys_notice (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  title        VARCHAR(100) NOT NULL COMMENT '公告标题',
  content      TEXT         DEFAULT NULL COMMENT '公告内容',
  notice_type  TINYINT      DEFAULT 1 COMMENT '1院内2科室',
  dept_id      BIGINT       DEFAULT NULL COMMENT '科室ID（可空）',
  publisher_id BIGINT       DEFAULT NULL COMMENT '发布人',
  publish_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  status       TINYINT      DEFAULT 1 COMMENT '0草稿1已发布',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '公告表';

-- 菜单：报表中心 60（report:view）/ 报表导出 61（report:export）/ 消息中心 62（message:view）
-- 编号沿用现有方案（44-47 空闲，60-62 挂到对应父级）
INSERT INTO sys_menu (id, menu_name, parent_id, path, perms, icon, menu_type, sort, status) VALUES
(60, '报表中心', 7, 'reports', 'report:view',   'TrendCharts', 2, 1, 1),
(61, '报表导出', 60, '#',       'report:export', NULL, 3, 1, 1),
(62, '消息中心', 12, 'center',  'message:view',  'Bell',       2, 1, 1);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (60, 61)
WHERE r.role_code IN ('admin', 'dean');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 62 FROM sys_role r;
