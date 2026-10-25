-- ============================================================
-- 医疗后台管理系统 · 数据安全与审计模块（阶段5，接口文档 §十三）
-- 日志查询从系统管理迁入 /api/audit/*；备份为 Java 原生导出
-- 登录失败锁定：security.fail-count 次锁定 10 分钟（课程级内存实现）
-- ============================================================
USE medical;

CREATE TABLE IF NOT EXISTS sys_operation_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT       DEFAULT NULL COMMENT '操作用户',
  username    VARCHAR(50)  DEFAULT NULL COMMENT '用户名',
  module      VARCHAR(50)  DEFAULT NULL COMMENT '模块',
  operation   VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
  method      VARCHAR(200) DEFAULT NULL COMMENT '请求方法 路径',
  params      TEXT         DEFAULT NULL COMMENT '请求参数',
  ip          VARCHAR(50)  DEFAULT NULL COMMENT 'IP地址',
  status      TINYINT      DEFAULT 1 COMMENT '0失败1成功',
  cost_time   INT          DEFAULT NULL COMMENT '耗时ms',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  KEY idx_module (module),
  KEY idx_user (user_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志表';

CREATE TABLE IF NOT EXISTS sys_login_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username    VARCHAR(50)  DEFAULT NULL COMMENT '用户名',
  ip          VARCHAR(50)  DEFAULT NULL COMMENT 'IP地址',
  status      TINYINT      DEFAULT 1 COMMENT '0失败1成功',
  msg         VARCHAR(255) DEFAULT NULL COMMENT '提示信息',
  login_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '登录日志表';

CREATE TABLE IF NOT EXISTS sys_backup (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  backup_name VARCHAR(100) DEFAULT NULL COMMENT '备份名称',
  backup_path VARCHAR(255) DEFAULT NULL COMMENT '备份路径',
  backup_size BIGINT       DEFAULT NULL COMMENT '文件大小(字节)',
  backup_type TINYINT      DEFAULT 1 COMMENT '1手动2自动',
  status      TINYINT      DEFAULT 1 COMMENT '0失败1成功',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '备份时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据备份表';

CREATE TABLE IF NOT EXISTS sys_config (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  config_key   VARCHAR(100) NOT NULL COMMENT '配置键',
  config_value VARCHAR(255) DEFAULT NULL COMMENT '配置值',
  config_name  VARCHAR(100) DEFAULT NULL COMMENT '配置名称',
  remark       VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统参数表';

-- 安全配置种子五键（文档 §13.1）
INSERT INTO sys_config (config_key, config_value, config_name, remark) VALUES
('security.fail-count', '5', '登录失败锁定次数', '达到次数锁定10分钟'),
('security.session-timeout', '120', '会话超时时间(分钟)', '作用于新签发令牌'),
('security.ip-whitelist', '', 'IP白名单', '空不限制，逗号分隔'),
('security.two-factor', '0', '双因素认证', '演示配置暂不接入'),
('security.data-mask', '1', '敏感数据脱敏', '患者列表手机号打码')
ON DUPLICATE KEY UPDATE config_name = VALUES(config_name);

-- 菜单：数据安全（parent_id=13）按钮 68-70（文档 §13.2⑦）
INSERT INTO sys_menu (id, menu_name, parent_id, path, perms, icon, menu_type, sort, status) VALUES
(59, '审计日志', 13, 'logs', 'audit:log:view', 'Document', 2, 1, 1),
(68, '日志查询', 59, '#', 'audit:log:view',  NULL, 3, 1, 1),
(69, '备份管理', 59, '#', 'backup:add',      NULL, 3, 2, 1),
(70, '安全配置', 59, '#', 'security:config', NULL, 3, 3, 1);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (59, 68, 69, 70)
WHERE r.role_code = 'admin';
