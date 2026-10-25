-- ============================================================
-- 医疗后台管理系统 · 表结构初始化
-- 依据《接口,字段,原型图文档》§2.1 系统管理模块
-- MySQL 8.0 / utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS medical DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE medical;

-- ------------------------------------------------------------
-- 用户表 sys_user
-- username 唯一口径：含已删除行（删除后不可重建同名账号）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username    VARCHAR(50)  NOT NULL COMMENT '登录账号',
  password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
  real_name   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
  gender      TINYINT      DEFAULT 0 COMMENT '性别 0女1男',
  phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  email       VARCHAR(50)  DEFAULT NULL COMMENT '邮箱',
  dept_id     BIGINT       DEFAULT NULL COMMENT '科室ID',
  role_id     BIGINT       DEFAULT NULL COMMENT '角色ID',
  avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像',
  status      TINYINT      DEFAULT 1 COMMENT '状态 0禁用1启用',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表';

-- ------------------------------------------------------------
-- 角色表 sys_role
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_name   VARCHAR(50) NOT NULL COMMENT '角色名称',
  role_code   VARCHAR(50) NOT NULL COMMENT '角色编码',
  remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  status      TINYINT     DEFAULT 1 COMMENT '状态 0禁用1启用',
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT     DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表';

-- ------------------------------------------------------------
-- 科室表 sys_dept（parent_id=0 为顶级；dept_type 1门诊2住院3医技4行政后勤）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id   BIGINT      DEFAULT 0 COMMENT '父级ID，0为顶级',
  dept_name   VARCHAR(50) NOT NULL COMMENT '科室名称',
  dept_code   VARCHAR(50) DEFAULT NULL COMMENT '科室编码',
  dept_type   TINYINT     DEFAULT 1 COMMENT '类型 1门诊2住院3医技4行政后勤',
  sort        INT         DEFAULT 0 COMMENT '排序',
  status      TINYINT     DEFAULT 1 COMMENT '状态 0禁用1启用',
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT     DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '科室表';

-- ------------------------------------------------------------
-- 菜单表 sys_menu（menu_type 1目录2菜单3按钮）
-- 顶级菜单 id 1-13 与接口文档阶段编号对齐：
-- 9=住院管理 10=检查检验 11=公共卫生 12=消息中心 13=数据安全
-- 后续阶段按钮菜单自 51 起编号
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  menu_name   VARCHAR(50)  NOT NULL COMMENT '菜单名称',
  parent_id   BIGINT       DEFAULT 0 COMMENT '父级ID，0为顶级',
  path        VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
  perms       VARCHAR(100) DEFAULT NULL COMMENT '权限标识（菜单 view / 按钮操作）',
  icon        VARCHAR(100) DEFAULT NULL COMMENT '图标（预留）',
  menu_type   TINYINT      DEFAULT 2 COMMENT '类型 1目录2菜单3按钮',
  sort        INT          DEFAULT 0 COMMENT '排序',
  status      TINYINT      DEFAULT 1 COMMENT '状态 0禁用1启用',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜单表';

-- ------------------------------------------------------------
-- 角色菜单关联表 sys_role_menu
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
  id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_id     BIGINT   NOT NULL COMMENT '角色ID',
  menu_id     BIGINT   NOT NULL COMMENT '菜单ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT  DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  KEY idx_role_id (role_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色菜单关联表';

-- ------------------------------------------------------------
-- 字典类型表 sys_dict_type
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_dict_type;
CREATE TABLE sys_dict_type (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  dict_name   VARCHAR(100) NOT NULL COMMENT '字典名称',
  dict_type   VARCHAR(100) NOT NULL COMMENT '字典类型',
  remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  status      TINYINT      DEFAULT 1 COMMENT '状态 0禁用1启用',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dict_type (dict_type)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典类型表';

-- ------------------------------------------------------------
-- 字典数据表 sys_dict_data
-- 患者标签场景：dict_value 与 dict_label 同值，color 三色分级
-- red 高风险 / yellow 关注 / green 常规，空为中性
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_dict_data;
CREATE TABLE sys_dict_data (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  dict_type   VARCHAR(100) NOT NULL COMMENT '字典类型',
  dict_label  VARCHAR(100) NOT NULL COMMENT '字典标签',
  dict_value  VARCHAR(100) NOT NULL COMMENT '字典键值',
  color       VARCHAR(10)  DEFAULT NULL COMMENT '标签颜色 red/yellow/green（空为中性描边）',
  sort        INT          DEFAULT 0 COMMENT '排序',
  status      TINYINT      DEFAULT 1 COMMENT '状态 0禁用1启用',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  KEY idx_dict_type (dict_type)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典数据表';
