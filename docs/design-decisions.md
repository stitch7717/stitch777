# 设计取舍说明

本文记录项目中几个关键工程问题的方案选择与原因，便于代码评审和面试复盘。

## 1. 挂号并发：为什么不用 Redis 分布式锁

单机部署、数据库是唯一共享状态，直接把正确性下沉到数据库，避免为一个演示项目引入额外中间件。

### 1.1 号源扣减

```sql
UPDATE biz_schedule
SET remain_num = remain_num - 1
WHERE id = ? AND status = 1 AND remain_num > 0
```

一条 SQL 完成「判断 + 扣减」，影响行数为 0 即号源已满或停诊。并发下由 InnoDB 行锁串行化，不会超卖。

### 1.2 排队号：从 `MAX(queue_no)+1` 改为计数器表

原实现先查当日该科室最大排队号再加 1，两个并发事务会读到同一个值。现改为：

```sql
INSERT INTO biz_queue_seq (dept_id, seq_date, current_no)
VALUES (?, ?, 1)
ON DUPLICATE KEY UPDATE current_no = current_no + 1;
SELECT current_no FROM biz_queue_seq WHERE dept_id = ? AND seq_date = ?;
```

- 计数器按「科室 + 日期」维度，符合「当日科室排队号」的业务语义；
- `INSERT ... ON DUPLICATE KEY UPDATE` 持有行锁到事务提交，同一科室/日期的取号串行化；
- 为什么不放在 `biz_schedule.queue_seq`：同一天同一科室可能有上午/下午两个排班，号池必须共享，否则两个排班都会从 1 开始。

### 1.3 同一患者重复挂号：生成列 + 唯一约束

```sql
active_flag TINYINT GENERATED ALWAYS AS (
  CASE WHEN deleted = 0 AND status IN (0, 1, 2) THEN 1 ELSE NULL END
) STORED,
UNIQUE KEY uk_patient_schedule_active (patient_id, schedule_id, active_flag)
```

- 待就诊/就诊中/已完成视为「有效」，退号/逻辑删除生成 `NULL`；
- MySQL 唯一索引允许多个 `NULL`，因此退号后可以再次挂同一排班；
- 服务层捕获 `DuplicateKeyException`，转换为 HTTP 409；事务回滚会同时归还已扣减的号源。

另有 `uk_dept_date_queue (dept_id, reg_date, queue_no)` 作为排队号唯一的兜底。

## 2. 收费幂等：行锁 + 状态条件更新

### 2.1 并发重复收费

`ChargeService.charge` 事务首行执行：

```sql
SELECT * FROM biz_registration WHERE id = ? AND deleted = 0 FOR UPDATE
```

锁住就诊行后再检查「是否已有已支付收费单」。同一就诊的并发收费在此串行化，第二个请求看到已收费后返回 409。

为什么锁就诊行而不是收费单：收费单此时还不存在，没有可锁的目标；就诊行是同一业务聚合的稳定锚点。

### 2.2 状态迁移

```sql
UPDATE biz_charge SET pay_status = 2
WHERE id = ? AND pay_status = 1
```

影响行数为 0 说明状态已被其他请求改变，返回 409。处方收费 `1已审核 → 2已收费`、发药 `1/2 → 3已发药`、退药 `3 → 4` 均采用同一模式。

### 2.3 HTTP 状态码

`BusinessException` 的 code 与 HTTP 状态保持一致：400/401/403/409/500 都返回真实 HTTP 状态，不再统一 200。前端 `request.js` 同时识别 HTTP 状态与 body code。

## 3. JWT 改密失效：版本号方案

### 3.1 实现

- `sys_user` 增加 `token_version INT DEFAULT 0`；
- 签发 token 时写入 `ver` 声明；
- `JwtAuthenticationFilter` 每次请求比对 `token.ver` 与数据库 `token_version`，不一致则不建立认证上下文，最终返回 401；
- 管理员重置密码时 `token_version + 1`，该账号所有已签发 token 立即失效。

### 3.2 为什么用 token 版本号，而不是 Redis 黑名单

| 方案 | 优点 | 代价 |
|---|---|---|
| token 版本号（本项目） | 无额外存储、实现简单、改密即全端下线 | 每次请求需读一次用户表（本过滤器本来就要读用户/角色/权限） |
| Redis 黑名单 | 可精确撤销单个 token | 需要额外存储和过期清理；黑名单 key 量大 |
| 短 TTL + refresh token | 泄露窗口小 | 需要 refresh 流程，复杂度更高 |

单机项目选择版本号；如果未来需要「单设备登出」，可以复用已有 Redis 做黑名单。

### 3.3 登出的边界

当前登出是前端清除 token，服务端不保存会话。改密会强制所有端下线；普通登出不能撤销已泄露的 token，这一点在 README 中如实说明，不夸大为「服务端登出」。

## 4. 敏感字段：AES-256-GCM + HMAC 检索列

### 4.1 为什么是 GCM

- ECB 是确定性加密：相同明文产生相同密文，攻击者不用解密就能判断两个患者身份证是否相同；
- GCM 是 AEAD，密文被篡改时解密直接失败，而不是返回乱码；
- 每次加密使用 12 字节随机 IV，存储格式 `ENC:Base64(IV + 密文 + 16字节认证标签)`；
- 原实现密文约 48 字符，但列是 `VARCHAR(20)`，严格模式下写入会直接失败；迁移脚本已将列扩为 `VARCHAR(255)`。

### 4.2 加密后如何按身份证查询

GCM 的随机 IV 导致同一明文每次密文不同，无法用 `WHERE id_card = ?` 查询。方案是额外存一列确定性哈希：

```
id_card_hash = HMAC-SHA256(pepper, 规范化身份证号)
```

- 为什么用 HMAC 而不是裸 SHA-256：身份证号空间有限，裸哈希可被彩虹表反推；HMAC 需要服务端密钥；
- 规范化：去除空白、字母统一大写；
- 查询/查重只走 `id_card_hash`，加密列仅用于展示时解密。

### 4.3 密钥管理

- `AES_KEY`、`ID_CARD_PEPPER`、`JWT_SECRET` 通过环境变量注入；
- 本地开发放在 `application-local.yml`（已 gitignore），仓库只提交 `.example`；
- 启动时校验密钥长度，缺失直接失败，避免运行到写入身份证才报错；
- 历史明文数据通过 `--app.backfill=true` 一次性回填（`IdCardBackfillRunner`），可重复执行。

## 5. 权限模型

- RBAC：`sys_user.role_id → sys_role → sys_role_menu → sys_menu.perms`；
- 权限码同时驱动前端菜单与后端 `@PreAuthorize`；
- 修复了 4 个仅登录即可访问的控制器（用户分页、部门树、菜单树、公告发布/删除）；
- `/me`、`/logout`、`/menu/current` 保持仅需登录，避免把侧边栏渲染也锁死。

## 6. Redis 与消息队列的取舍

- Redis 只做三个场景：登录失败计数、角色权限缓存、患者基础信息缓存；都是缓存/计数，MySQL 仍是唯一数据源；
- 登录计数用 Lua 把 `INCR` 和 `EXPIRE` 做成原子操作，避免「有计数没 TTL」；权限缓存 5 分钟 TTL，角色/菜单变更时删除；患者基础信息 2 分钟 TTL，只缓存 id/patientNo/name，不缓存过敏史等临床数据；
- 三个场景都 fail-open：Redis 失败时 30 秒熔断，登录计数回退内存，权限回源数据库，患者缓存跳过；不会因为 Redis 挂了导致登录/鉴权不可用；
- 不用 Redis 分布式锁：挂号/收费的正确性由数据库唯一索引、行锁、条件更新保证，Redis 锁是冗余；
- 没有引入 MQ：项目没有异步削峰、延迟任务等真实需求，引入 MQ 只会增加部署与排障成本。

## 7. 测试策略

- 测试库 `medical_test` 由 `scripts/init-test-db.sh` 从 `sql/01~17` 初始化，与开发库隔离；
- 30 个集成测试，覆盖：挂号并发/重复预约、排队号唯一、收费与退费并发、处方审核、并发发药、库存扣减、身份证加解密与按证查询、JWT 改密失效、权限拒绝、登录成功/失败、Redis 登录计数与权限缓存；
- 并发测试使用 `CountDownLatch` 同时放行，断言真实数据库中的行数与库存，而不是 mock；
- JaCoCo 整体行覆盖率约 22%，核心改动类 41%–68%。存量代码量大，未追求整体 80%，README 中如实展示，不虚报。

## 8. 已知限制

- 单机部署；登录失败计数已迁到 Redis，Redis 不可用时回退内存；JWT 登出未做多节点共享；
- 普通登出不能撤销已泄露 token（改密可以）；
- 测试覆盖集中在新改动的核心链路，报表/公卫/住院等模块尚未补测试；
- Redis 只做缓存/计数，不做分布式锁、不存就诊数据。
