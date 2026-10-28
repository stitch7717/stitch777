# 医疗后台管理系统 — 字段设计、接口文档与原型图

> **说明**
>
> - 接口遵循 RESTful 风格，统一返回结构：`{ "code": 200, "msg": "success", "data": {} }`
> - 原型图使用 ASCII 线框图表示，可用于需求评审，后续可用 Axure / Figma 落地。
> - 文档涵盖 12 个功能模块的字段设计、接口定义与页面原型。

------

## 目录

1. 通用规范
2. 系统管理模块
3. 患者管理模块
4. 挂号与预约模块
5. 门诊管理模块
6. 住院管理模块
7. 药品与药房管理模块
8. 检查检验管理模块
9. 收费与财务管理模块
10. 公共卫生与健康管理模块
11. 统计报表模块
12. 消息与通知模块
13. 数据安全与审计模块

------

## 一、通用规范

### 1.1 分页请求参数

| 参数      | 类型   | 必填 | 说明                |
| :-------- | :----- | :--- | :------------------ |
| pageNum   | int    | 是   | 页码，从1开始       |
| pageSize  | int    | 是   | 每页条数，默认10    |
| keyword   | string | 否   | 模糊搜索关键字      |
| startTime | string | 否   | 开始时间 yyyy-MM-dd |
| endTime   | string | 否   | 结束时间 yyyy-MM-dd |

### 1.2 分页返回结构

json

```
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 100,
    "pageNum": 1,
    "pageSize": 10,
    "list": []
  }
}
```



### 1.3 状态码约定

| code | 说明       |
| :--- | :--------- |
| 200  | 成功       |
| 400  | 参数错误   |
| 401  | 未登录     |
| 403  | 无权限     |
| 500  | 服务器异常 |

### 1.4 文件上传（通用能力）

| 方法 | 路径              | 说明                                              |
| :--- | :---------------- | :------------------------------------------------ |
| POST | /api/file/upload  | 通用文件上传（multipart/form-data，字段名 file，≤10MB） |

响应 `data` 为文件可访问 URL（阶段2 起由对象存储 RustFS 提供）。业务侧库里只存 URL：② 患者头像、⑦ 检验报告/影像附件等复用此接口。

------

## 二、系统管理模块

### 2.1 字段设计

**用户表 sys_user**

| 字段        | 类型         | 说明            |
| :---------- | :----------- | :-------------- |
| id          | bigint       | 主键            |
| username    | varchar(50)  | 登录账号        |
| password    | varchar(100) | 密码（加密）    |
| real_name   | varchar(50)  | 真实姓名        |
| gender      | tinyint      | 性别 0女1男     |
| phone       | varchar(20)  | 手机号          |
| email       | varchar(50)  | 邮箱            |
| dept_id     | bigint       | 科室ID          |
| role_id     | bigint       | 角色ID          |
| avatar      | varchar(255) | 头像            |
| status      | tinyint      | 状态 0禁用1启用 |
| create_time | datetime     | 创建时间        |
| update_time | datetime     | 更新时间        |
| deleted     | tinyint      | 逻辑删除        |

**角色表 sys_role**

| 字段        | 类型         | 说明     |
| :---------- | :----------- | :------- |
| id          | bigint       | 主键     |
| role_name   | varchar(50)  | 角色名称 |
| role_code   | varchar(50)  | 角色编码 |
| remark      | varchar(255) | 备注     |
| status      | tinyint      | 状态     |
| create_time | datetime     | 创建时间 |

**科室表 sys_dept**

| 字段      | 类型        | 说明                 |
| :-------- | :---------- | :------------------- |
| id        | bigint      | 主键                 |
| parent_id | bigint      | 父级ID               |
| dept_name | varchar(50) | 科室名称             |
| dept_code | varchar(50) | 科室编码             |
| dept_type | tinyint     | 类型 1门诊2住院3医技4行政后勤（阶段3①扩4） |
| sort      | int         | 排序                 |
| status    | tinyint     | 状态                 |

**菜单表 sys_menu**（阶段3①施工补建，见计划书第八章）

| 字段      | 类型        | 说明                 |
| :-------- | :---------- | :------------------- |
| id        | bigint      | 主键                 |
| menu_name | varchar(50) | 菜单名称             |
| parent_id | bigint      | 父级ID，0为顶级      |
| path      | varchar(200) | 路由地址            |
| perms     | varchar(100) | 权限标识（预留）    |
| icon      | varchar(100) | 图标（预留）        |
| menu_type | tinyint     | 类型 1目录2菜单3按钮 |
| sort      | int         | 排序                 |
| status    | tinyint     | 状态 0禁用1启用      |

**角色菜单关联表 sys_role_menu**（阶段3①施工补建）

| 字段    | 类型   | 说明   |
| :------ | :----- | :----- |
| id      | bigint | 主键   |
| role_id | bigint | 角色ID |
| menu_id | bigint | 菜单ID |

**字典类型表 sys_dict_type**（阶段3 施工补建，计划书第八章；首个场景：患者标签）

| 字段      | 类型         | 说明     |
| :-------- | :----------- | :------- |
| id        | bigint       | 主键     |
| dict_name | varchar(100) | 字典名称 |
| dict_type | varchar(100) | 字典类型 |
| remark    | varchar(255) | 备注     |
| status    | tinyint      | 状态     |

**字典数据表 sys_dict_data**（阶段3 施工补建）

| 字段       | 类型         | 说明                    |
| :--------- | :----------- | :---------------------- |
| id         | bigint       | 主键                    |
| dict_type  | varchar(100) | 字典类型                |
| dict_label | varchar(100) | 字典标签                |
| dict_value | varchar(100) | 字典键值                |
| color      | varchar(10)  | 标签颜色 red/yellow/green（三色分级，空为中性描边；阶段3 追加） |
| sort       | int          | 排序（预留，当前按 id） |
| status     | tinyint      | 状态                    |

（以上各表均含公共字段 create_time / update_time / deleted，见通用数据约定）

> 字典语义注明：患者标签场景 `dict_value` 与 `dict_label` 同值（患者 tags 直接存标签文本，筛选走 FIND_IN_SET 精确匹配，写入端已归一化分隔符）；删除标签后允许重建同名（与用户名的含已删唯一口径不同）；`biz_patient.tags` 存量字符串不做白名单校验。
>
> 标签三色分级（阶段3 追加）：`color` 取值 red 高风险 / yellow 关注 / green 常规，用于患者列表与详情标签 chip 的视觉分级；创建时可选（缺省中性），编辑仅支持改色（不支持恢复中性，需中性可删除后重建）；种子标签默认分级：高风险=红、慢病=黄、VIP=绿。

### 2.2 接口文档

| 方法   | 路径                             | 说明      |
| :----- | :------------------------------- | :-------- |
| POST   | /api/system/user/login           | 登录      |
| POST   | /api/system/user/logout          | 登出      |
| GET    | /api/system/user/page            | 用户分页（支持 keyword/deptId/roleId/status 筛选） |
| POST   | /api/system/user                 | 新增用户  |
| PUT    | /api/system/user/{id}            | 修改用户  |
| DELETE | /api/system/user/{id}            | 删除用户  |
| PUT    | /api/system/user/{id}/status     | 启用/禁用 |
| PUT    | /api/system/user/{id}/password   | 重置密码  |
| GET    | /api/system/role/list            | 角色列表（含 userCount 用户数） |
| POST   | /api/system/role                 | 新增角色  |
| PUT    | /api/system/role/{id}/permission | 分配权限  |
| GET    | /api/system/dept/tree            | 科室树    |
| GET    | /api/system/log/page             | 日志分页  |
| GET    | /api/system/dict/data            | 按类型查询字典数据（dictType 必传，未知类型返回空数组） |
| POST   | /api/system/dict/data            | 新增字典数据（同类型未删行标签唯一，可删除后重建同名；可带 color） |
| PUT    | /api/system/dict/data/{id}       | 修改字典数据（阶段3 追加：当前仅改色 {color: red/yellow/green}，权限 system:dict:edit） |
| DELETE | /api/system/dict/data/{id}       | 删除字典数据（逻辑删除） |
| GET    | /api/system/menu/current         | 当前用户可见菜单（含按钮权限，不挂权限，无角色返回空数组） |
| GET    | /api/system/menu/tree            | 全量菜单树（供角色权限分配） |
| GET    | /api/system/role/{id}            | 角色详情（含已分配 menuIds 与用户数） |
| PUT    | /api/system/role/{id}            | 修改角色（roleName/remark/status；roleCode 不可改） |
| DELETE | /api/system/role/{id}            | 删除角色（硬删除，内置角色保护，有用户引用拒绝） |

**权限说明（阶段3 追加）**：sys_menu 的 perms 字段为权限标识（菜单 view / 按钮操作），接口经 `@PreAuthorize` 校验（命名规范与角色矩阵见《项目搭建计划书》第九章）；登录返回 `userInfo` 增加 `roleName`（角色中文名）。

**登录接口示例**

json

```
// 请求 POST /api/system/user/login
{ "username": "admin", "password": "123456" }

// 响应
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1...",
    "userInfo": { "id": 1, "realName": "管理员", "roleCode": "admin" }
  }
}
```



### 2.3 原型图

text

```
┌──────────────────────────────────────────────────────────┐
│ 医疗后台管理系统          [搜索框]        [管理员▼]      │
├────────┬─────────────────────────────────────────────────┤
│ 系统管理│  用户管理                                        │
│ 患者管理│  ┌──────────────────────────────────────────┐  │
│ 挂号预约│  │ [+新增] [批量删除] 姓名:[   ] 科室:[▼]   │  │
│ 门诊管理│  ├──────────────────────────────────────────┤  │
│ 住院管理│  │ □ 账号   姓名  科室  角色   状态  操作   │  │
│ 药房管理│  │ □ admin  管理员 内科  超管   启用 编辑删 │  │
│ 收费财务│  │ □ doc01  张三   内科  医生   启用 编辑删 │  │
│ 统计报表│  └──────────────────────────────────────────┘  │
│         │            [上一页] 1 2 3 [下一页]             │
└────────┴─────────────────────────────────────────────────┘
```



------

## 三、患者管理模块

### 3.1 字段设计

**患者表 biz_patient**

| 字段            | 类型          | 说明             |
| :-------------- | :------------ | :--------------- |
| id              | bigint        | 主键             |
| patient_no      | varchar(30)   | 患者编号         |
| name            | varchar(50)   | 姓名             |
| gender          | tinyint       | 性别             |
| birth_date      | date          | 出生日期         |
| id_card         | varchar(20)   | 身份证号（加密） |
| phone           | varchar(20)   | 手机号           |
| address         | varchar(255)  | 住址             |
| blood_type      | varchar(10)   | 血型             |
| allergy_history | varchar(500)  | 过敏史           |
| medical_history | text          | 既往病史         |
| family_history  | text          | 家族病史         |
| card_no         | varchar(30)   | 就诊卡号         |
| card_status     | tinyint       | 就诊卡状态 0挂失1正常（阶段3②扩） |
| card_balance    | decimal(10,2) | 卡余额           |
| tags            | varchar(255)  | 标签（逗号分隔） |
| create_time     | datetime      | 建档时间         |

**病历表 biz_medical_record**

| 字段            | 类型         | 说明                |
| :-------------- | :----------- | :------------------ |
| id              | bigint       | 主键                |
| patient_id      | bigint       | 患者ID              |
| visit_no        | varchar(30)  | 就诊流水号          |
| visit_type      | tinyint      | 1门诊2住院          |
| chief_complaint | varchar(500) | 主诉                |
| present_illness | text         | 现病史              |
| physical_exam   | text         | 体格检查            |
| diagnosis       | varchar(500) | 诊断                |
| icd_code        | varchar(20)  | ICD编码             |
| doctor_id       | bigint       | 医生ID              |
| visit_time      | datetime     | 就诊时间            |
| status          | tinyint      | 0草稿1已提交2已归档 |

**随访表 biz_follow_up**

| 字段        | 类型         | 说明            |
| :---------- | :----------- | :-------------- |
| id          | bigint       | 主键            |
| patient_id  | bigint       | 患者ID          |
| plan_date   | date         | 计划随访日期    |
| follow_type | tinyint      | 1电话2上门3短信 |
| content     | text         | 随访内容        |
| result      | varchar(500) | 随访结果        |
| operator_id | bigint       | 随访人          |
| status      | tinyint      | 0待随访1已完成  |

### 3.2 接口文档

| 方法   | 路径                        | 说明         |
| :----- | :-------------------------- | :----------- |
| GET    | /api/patient/page           | 患者分页     |
| GET    | /api/patient/{id}           | 患者详情     |
| POST   | /api/patient                | 新增患者     |
| PUT    | /api/patient/{id}           | 修改患者     |
| DELETE | /api/patient/{id}           | 删除患者     |
| GET    | /api/patient/{id}/records   | 患者病历列表 |
| POST   | /api/patient/card/reissue   | 补办就诊卡   |
| PUT    | /api/patient/card/{id}/loss | 挂失         |
| GET    | /api/patient/followup/page  | 随访分页     |
| POST   | /api/patient/followup       | 新增随访     |

**新增患者示例**

json

```
// POST /api/patient
{
  "name": "张三",
  "gender": 1,
  "birthDate": "1990-05-01",
  "idCard": "44030019900501XXXX",
  "phone": "13800000000",
  "address": "深圳市南山区",
  "bloodType": "A",
  "allergyHistory": "青霉素过敏"
}
```



### 3.3 原型图

text

```
┌──────────────────────────────────────────────────────────┐
│ 患者管理 > 患者档案                                      │
│ ┌──────────────────────────────────────────────────────┐│
│ │ [+新建患者] [导入] 姓名:[   ] 手机:[   ] [查询]      ││
│ ├──────────────────────────────────────────────────────┤│
│ │ 编号   姓名 性别 年龄 手机      就诊卡   标签   操作 ││
│ │ P0001  张三  男  34  138***00   C10001  慢病   编辑  ││
│ │ P0002  李四  女  28  139***11   C10002  VIP    编辑  ││
│ └──────────────────────────────────────────────────────┘│
│                                                          │
│ ┌── 患者详情 ──────────────────────────────────────────┐│
│ │ 姓名:张三  性别:男  年龄:34  血型:A                  ││
│ │ 身份证:4403**********01   手机:138****0000          ││
│ │ 过敏史:青霉素过敏                                     ││
│ │ [病历记录] [随访记录] [就诊卡] [既往病史]            ││
│ └──────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────┘
```



------

## 四、挂号与预约模块

### 4.1 字段设计

**排班表 biz_schedule**

| 字段          | 类型          | 说明            |
| :------------ | :------------ | :-------------- |
| id            | bigint        | 主键            |
| doctor_id     | bigint        | 医生ID          |
| dept_id       | bigint        | 科室ID          |
| schedule_date | date          | 排班日期        |
| time_period   | tinyint       | 1上午2下午3晚上 |
| reg_type      | tinyint       | 1普通2专家3急诊 |
| total_num     | int           | 总号源          |
| remain_num    | int           | 剩余号源        |
| reg_fee       | decimal(10,2) | 挂号费          |
| status        | tinyint       | 0停诊1正常      |

**挂号记录表 biz_registration**

| 字段        | 类型          | 说明                         |
| :---------- | :------------ | :--------------------------- |
| id          | bigint        | 主键                         |
| reg_no      | varchar(30)   | 挂号单号                     |
| patient_id  | bigint        | 患者ID                       |
| doctor_id   | bigint        | 医生ID                       |
| dept_id     | bigint        | 科室ID                       |
| schedule_id | bigint        | 排班ID                       |
| reg_type    | tinyint       | 挂号类型                     |
| reg_fee     | decimal(10,2) | 挂号费                       |
| queue_no    | int           | 排队号                       |
| status      | tinyint       | 0待就诊1就诊中2已完成3已退号 |
| reg_time    | datetime      | 挂号时间                     |

### 4.2 接口文档

| 方法 | 路径                                 | 说明     |
| :--- | :----------------------------------- | :------- |
| GET  | /api/registration/schedule           | 排班查询 |
| POST | /api/registration/schedule           | 新增排班 |
| PUT  | /api/registration/schedule/{id}/stop | 停诊     |
| POST | /api/registration                    | 挂号     |
| PUT  | /api/registration/{id}/cancel        | 退号     |
| GET  | /api/registration/queue/{deptId}     | 候诊队列 |
| POST | /api/registration/call/{id}          | 叫号     |
| GET  | /api/registration/depts              | 科室下拉（阶段3 补充：挂号页专用，避免依赖 system:view） |
| GET  | /api/registration/doctors?deptId=    | 医生下拉（阶段3 补充：同上，最小字段） |

**阶段3 实现说明**：① 挂号响应含 `regNo`（REG+挂号日期+序号）；② `queueNo` 为**当日该科室**按序排队号（含退号不回收）；③ `biz_registration` 扩展 `patient_name`（姓名快照·凭证语义）与 `creator_id`（操作人）两列；④ 号源扣减采用 DB 原子 UPDATE（`remain_num>0 AND status=1` 条件内扣，防超卖，替代计划书 §4.4 的 Redis 预扣——Redis 分桶预留为扩展）；⑤ 排班/队列查询按角色数据范围过滤（SELF/DEPT/ALL），其余为操作型接口。

**挂号示例**

json

```
// POST /api/registration
{
  "patientId": 1001,
  "scheduleId": 2001,
  "regType": 2
}

// 响应
{
  "code": 200,
  "data": {
    "regNo": "REG202501010001",
    "queueNo": 15,
    "regFee": 50.00
  }
}
```



### 4.3 原型图

text

```
┌──────────────────────────────────────────────────────────┐
│ 挂号与预约 > 现场挂号                                    │
│ ┌──────────────────────────────────────────────────────┐│
│ │ 患者:[张三 ▼]  就诊卡:C10001                         ││
│ │ 科室:[内科 ▼]  医生:[李医生 ▼]  日期:[2025-01-01]   ││
│ │ 号别: ○普通 ●专家 ○急诊   挂号费:￥50               ││
│ │                          [确认挂号]                  ││
│ └──────────────────────────────────────────────────────┘│
│                                                          │
│ ┌── 候诊队列（内科）──────────────────────────────────┐│
│ │ 排队号  姓名   状态      挂号时间                    ││
│ │  13    王五   候诊中    09:00                        ││
│ │  14    赵六   就诊中    09:05                        ││
│ │  15    张三   候诊中    09:10  ← 当前叫号            ││
│ └──────────────────────────────────────────────────────┘│
│                                     [叫下一位]           │
└──────────────────────────────────────────────────────────┘
```



------

## 五、门诊管理模块

### 5.1 字段设计

**处方表 biz_prescription**

| 字段         | 类型          | 说明                              |
| :----------- | :------------ | :-------------------------------- |
| id           | bigint        | 主键                              |
| presc_no     | varchar(30)   | 处方号                            |
| patient_id   | bigint        | 患者ID                            |
| doctor_id    | bigint        | 医生ID                            |
| visit_id     | bigint        | 就诊ID                            |
| presc_type   | tinyint       | 1西药2中药3中成药                 |
| diagnosis    | varchar(500)  | 诊断                              |
| total_amount | decimal(10,2) | 总金额                            |
| status       | tinyint       | 0待审核1已审核2已收费3已发药4已退 |
| create_time  | datetime      | 开方时间                          |

**处方明细表 biz_prescription_item**

| 字段      | 类型          | 说明     |
| :-------- | :------------ | :------- |
| id        | bigint        | 主键     |
| presc_id  | bigint        | 处方ID   |
| drug_id   | bigint        | 药品ID   |
| drug_name | varchar(100)  | 药品名称 |
| spec      | varchar(50)   | 规格     |
| usage     | varchar(100)  | 用法     |
| dosage    | varchar(50)   | 用量     |
| frequency | varchar(20)   | 频次     |
| days      | int           | 天数     |
| quantity  | int           | 数量     |
| price     | decimal(10,2) | 单价     |
| amount    | decimal(10,2) | 小计     |

### 5.2 接口文档

| 方法   | 路径                                | 说明     |
| :----- | :---------------------------------- | :------- |
| GET    | /api/clinic/visit/page              | 就诊列表 |
| POST   | /api/clinic/visit/{id}/receive      | 接诊     |
| POST   | /api/clinic/record                  | 保存病历 |
| GET    | /api/clinic/prescription/page       | 处方分页 |
| POST   | /api/clinic/prescription            | 开处方   |
| PUT    | /api/clinic/prescription/{id}/audit | 处方审核 |
| DELETE | /api/clinic/prescription/{id}       | 作废处方 |
| POST   | /api/clinic/exam                    | 开检查单 |
| GET    | /api/clinic/exam?registrationId=    | 检查单列表（阶段3 补充：按就诊查询，⑦模块整合到 /api/exam） |
| GET    | /api/clinic/drugs                   | 开方选品下拉（阶段3 补充：最小字段，避免依赖药房模块接口） |

**阶段3 实现说明**：① 就诊列表（visit/page）默认当日 0待就诊/1就诊中/2已完成（提交病历后仍可回看与补开），按角色数据范围过滤（SELF/DEPT/ALL）；② receive 对「已叫号/已接诊」幂等返回；③ `biz_prescription.visit_id` = 挂号记录ID（无独立就诊表）；④ 处方列表随页返回 items（一次批量填充）；⑤ 处方金额由服务端按药品当刻售价快照计算（前端仅展示）；⑥ 病历保存按就诊流水号（visit_no=挂号单号）upsert，提交时联动就诊完成；⑦ 开检查单与病历同属诊疗书写权限；⑧ 患者过敏史仅前端红字提示、不做开方阻断（文本比对不可靠）。

### 5.3 原型图

text

```
┌──────────────────────────────────────────────────────────┐
│ 门诊管理 > 医生工作台                                    │
│ ┌───就诊列表─────┬───接诊详情────────────────────────┐  │
│ │ 15 张三 候诊中 │ 患者:张三 男 34岁 过敏:青霉素      │  │
│ │ 16 李四 候诊中 │ ─────────────────────────────────  │  │
│ │ 17 王五 候诊中 │ 主诉: [发热3天            ]        │  │
│ │                │ 现病史:[畏寒、咳嗽      ]         │  │
│ │                │ 体格检查:[体温38.5℃    ]         │  │
│ │                │ 诊断:  [上呼吸道感染 ▼] ICD:J06  │  │
│ │                │ ─────────────────────────────────  │  │
│ │                │ [开处方] [开检查] [保存] [提交]    │  │
│ └────────────────┴────────────────────────────────────┘  │
│                                                          │
│ ┌── 处方弹窗 ─────────────────────────────────────────┐ │
│ │ 药品:[阿莫西林 ▼] 规格:0.25g  用法:口服              │ │
│ │ 用量:1粒 频次:每日3次 天数:5天 数量:15 单价:2.5      │ │
│ │ [+添加]                                              │ │
│ │ ┌──────────────────────────────────────────────┐    │ │
│ │ │ 药品   规格  用法   频次   天数 数量 金额     │    │ │
│ │ │ 阿莫西林 0.25g 口服 3次/日 5   15  37.5      │    │ │
│ │ └──────────────────────────────────────────────┘    │ │
│ │ 合计:￥37.50           [取消] [提交处方]             │ │
│ └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```



------

## 六、住院管理模块

### 6.1 字段设计

**床位表 biz_bed**

| 字段     | 类型          | 说明                          |
| :------- | :------------ | :---------------------------- |
| id       | bigint        | 主键                          |
| ward_id  | bigint        | 病区ID（复用 sys_dept 科室ID） |
| bed_no   | varchar(20)   | 床位号                        |
| bed_type | tinyint       | 1普通2重症3VIP                |
| price    | decimal(10,2) | 床位费/天                     |
| status   | tinyint       | 0空闲1占用2维修               |

**住院登记表 biz_admission**

| 字段           | 类型          | 说明                            |
| :------------- | :------------ | :------------------------------ |
| id             | bigint        | 主键                            |
| admission_no   | varchar(30)   | 住院号                          |
| patient_id     | bigint        | 患者ID                          |
| patient_name   | varchar(50)   | 患者姓名快照（扩展列）          |
| dept_id        | bigint        | 科室ID                          |
| bed_id         | bigint        | 床位ID                          |
| doctor_id      | bigint        | 主治医生                        |
| nurse_id       | bigint        | 责任护士                        |
| admission_time | datetime      | 入院时间                        |
| discharge_time | datetime      | 出院时间（转科时=转出时刻）     |
| diagnosis      | varchar(500)  | 入院诊断                        |
| deposit        | decimal(10,2) | 押金                            |
| status         | tinyint       | 0在院1出院2转科（终结态）       |

**医嘱表 biz_medical_order**

| 字段         | 类型         | 说明                            |
| :----------- | :----------- | :------------------------------ |
| id           | bigint       | 主键                            |
| admission_id | bigint       | 住院ID                          |
| order_type   | tinyint      | 1长期2临时                      |
| content      | varchar(500) | 医嘱内容                        |
| drug_id      | bigint       | 药品ID（可选）                  |
| dosage       | varchar(50)  | 用量                            |
| usage_method | varchar(20)  | 用法：口服/静滴等（扩展列）     |
| frequency    | varchar(20)  | 频次                            |
| start_time   | datetime     | 开始时间                        |
| stop_time    | datetime     | 停止时间                        |
| doctor_id    | bigint       | 开嘱医生                        |
| status       | tinyint      | 0未执行1已执行2已停止           |

**护理记录表 biz_nursing_record**（计划书第八章点名补建）

| 字段           | 类型         | 说明              |
| :------------- | :----------- | :---------------- |
| id             | bigint       | 主键              |
| admission_id   | bigint       | 住院ID            |
| record_time    | datetime     | 记录时间          |
| temperature    | decimal(4,1) | 体温（℃）         |
| pulse          | int          | 脉搏（次/分）     |
| breath         | int          | 呼吸（次/分）     |
| blood_pressure | varchar(20)  | 血压（如 120/80） |
| content        | varchar(500) | 护理内容          |
| nurse_id       | bigint       | 记录护士          |

### 6.2 接口文档

| 方法 | 路径                                    | 说明                                       |
| :--- | :-------------------------------------- | :----------------------------------------- |
| GET  | /api/inpatient/bed/list                 | 床位列表（wardId/status 可选，聚合在院患者） |
| POST | /api/inpatient/admission                | 入院登记                                   |
| PUT  | /api/inpatient/admission/{id}/discharge | 出院办理                                   |
| POST | /api/inpatient/admission/{id}/transfer  | 转科                                       |
| GET  | /api/inpatient/admission/{id}           | 住院详情（含费用清单预演，补充接口）       |
| GET  | /api/inpatient/order/page               | 医嘱分页（admissionId 必填、orderType 可选） |
| POST | /api/inpatient/order                    | 开医嘱                                     |
| PUT  | /api/inpatient/order/{id}/execute       | 执行医嘱                                   |
| PUT  | /api/inpatient/order/{id}/stop          | 停止医嘱                                   |
| POST | /api/inpatient/nursing                  | 护理记录（新增）                           |
| GET  | /api/inpatient/nursing/page             | 护理记录分页（补充接口）                   |
| GET  | /api/inpatient/depts                    | 科室下拉（补充接口）                       |
| GET  | /api/inpatient/doctors                  | 医生下拉（补充接口，deptId 可选）          |
| GET  | /api/inpatient/nurses                   | 护士下拉（补充接口，deptId 可选）          |

**阶段4 实现说明**：① 床位号唯一键为**病区作用域**（`uk_ward_bed_no (ward_id, bed_no)`，支持多病区同号床）；`ward_id` **复用 sys_dept 科室ID**（科室即病区，不建 biz_ward，"内科病区"=内科）；② **表扩展列**（文档原文之外，均已落库）：`biz_admission` 加 `patient_name`（入院时姓名快照，床位一览零跨模块聚合）、`biz_medical_order` 加 `usage_method`（原型医嘱行「用法」列：口服/静滴等）；`biz_nursing_record` 为计划书第八章点名**补建表**（字段见 6.1）；③ **状态流转**：医嘱 `0未执行→1已执行→2已停止` 单调（execute 仅 0→1；stop 仅 1→2，未执行不可停止；长期医嘱 UI 文案「执行中」）；**转科 = 原记录终结**（`0→2`，`discharge_time` 记转出时刻）+ **新建在院记录**（新住院号，诊断/押金延续，旧床释放/新床占用）；出院 `0→1` 并释放床位；④ 住院号 `ZY+yyyyMMdd+%04d`（先插后回补，同收费单先例）；⑤ **并发防护**：床位占用/释放/换床为条件 UPDATE 仲裁（命中 0 行→400）；入院查重用在院记录锁查（空集亦持间隙锁，防同一患者并发双登）；开医嘱/护理记录锁住院记录行校验在院（防并发出院窗口）；⑥ **费用清单页签为费用预演**（住院详情内嵌 `feeSummary`：押金 + 床位费估算[床价×在院天数，向上取整≥1] + 药品医嘱清单，不做金额结算）——**住院结算归后续阶段**（charge_type=3 预留）；⑦ **补充接口 5 条**（第 10-14 条）：住院详情（床位卡点击弹窗）、护理记录分页、科室/医生/护士下拉（仿挂号模块先例仅返回最小字段，避免依赖 system/registration 权限；DirectoryProvider 增 listEnabledNurses/findEnabledNurse/userNames）；⑧ **权限四件套**：菜单 **51-55**（`inpatient:admission:add` / `inpatient:admission:edit`（出院+转科）/ `inpatient:order:add` / `inpatient:order:operate`（执行+停止）/ `inpatient:nursing:add`，parent_id=9 住院管理；计划书 §9.5 简写 `inpatient:bed` 取消、admission/order 各拆两码）；role_menu 段从 **215** 起（库中因角色管理 UI 保存重建已占用至 214，避让防 INSERT IGNORE 冲突）；角色分配：admin 全、doctor（admission 两码 + order 两码）、nurse（order:operate + nursing:add）、dean 只读、cashier/pharmacist 无权限；⑨ 床位维修态数据层支持（`status=2`，原型图例保留），维修维护 UI 暂缺（无对应接口，后续按需补）；⑩ 入院选患者复用患者分页接口（`/api/patient/page` 本阶段补全通用 `keyword` 对姓名/手机号 OR 模糊）。

### 6.3 原型图

text

```
┌──────────────────────────────────────────────────────────┐
│ 住院管理 > 床位一览（内科病区）                          │
│ ┌────┬────┬────┬────┬────┬────┬────┬────┐              │
│ │A01 │A02 │A03 │A04 │A05 │A06 │A07 │A08 │              │
│ │空闲│张三│李四│空闲│王五│空闲│赵六│空闲│              │
│ │    │●占 │●占 │    │●占 │    │●占 │    │              │
│ └────┴────┴────┴────┴────┴────┴────┴────┘              │
│  图例: ●占用  ○空闲  ▲维修                              │
│                                                          │
│ ┌── 张三 住院详情 ────────────────────────────────────┐ │
│ │ 住院号:ZY20250101001  床位:A02  主治:李医生         │ │
│ │ 入院时间:2025-01-01  押金:￥5000  状态:在院          │ │
│ │ [长期医嘱] [临时医嘱] [护理记录] [费用清单]         │ │
│ │ ────────────────────────────────────────────────    │ │
│ │ 长期医嘱:                                            │ │
│ │  阿莫西林 0.25g 口服 3次/日  执行中                  │ │
│ │  0.9%氯化钠 500ml 静滴 1次/日 执行中                │ │
│ │ [+开医嘱]                                            │ │
│ └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```



------

## 七、药品与药房管理模块

### 7.1 字段设计

**药品表 biz_drug**

| 字段           | 类型          | 说明         |
| :------------- | :------------ | :----------- |
| id             | bigint        | 主键         |
| drug_code      | varchar(30)   | 药品编码     |
| drug_name      | varchar(100)  | 药品名称     |
| generic_name   | varchar(100)  | 通用名       |
| spec           | varchar(50)   | 规格         |
| dosage_form    | varchar(30)   | 剂型         |
| unit           | varchar(20)   | 单位         |
| manufacturer   | varchar(100)  | 生产厂家     |
| approval_no    | varchar(50)   | 批准文号     |
| purchase_price | decimal(10,2) | 进价         |
| sale_price     | decimal(10,2) | 售价         |
| category       | varchar(50)   | 分类         |
| is_special     | tinyint       | 是否特殊药品 |
| status         | tinyint       | 状态         |

**库存表 biz_stock**

| 字段         | 类型        | 说明     |
| :----------- | :---------- | :------- |
| id           | bigint      | 主键     |
| drug_id      | bigint      | 药品ID   |
| batch_no     | varchar(50) | 批号     |
| quantity     | int         | 库存数量 |
| warn_num     | int         | 预警数量 |
| produce_date | date        | 生产日期 |
| expire_date  | date        | 有效期   |
| warehouse_id | bigint      | 库房ID   |

**出入库记录 biz_stock_record**

| 字段        | 类型         | 说明            |
| :---------- | :----------- | :-------------- |
| id          | bigint       | 主键            |
| drug_id     | bigint       | 药品ID          |
| record_type | tinyint      | 1入库2出库3盘点 |
| quantity    | int          | 数量            |
| batch_no    | varchar(50)  | 批号            |
| operator_id | bigint       | 操作人          |
| remark      | varchar(255) | 备注            |
| create_time | datetime     | 操作时间        |

### 7.2 接口文档

| 方法   | 路径                   | 说明         |
| :----- | :--------------------- | :----------- |
| GET    | /api/drug/page         | 药品分页     |
| POST   | /api/drug              | 新增药品     |
| PUT    | /api/drug/{id}         | 修改药品     |
| DELETE | /api/drug/{id}         | 删除药品     |
| GET    | /api/drug/stock/page   | 库存分页     |
| POST   | /api/drug/stock/in     | 入库         |
| POST   | /api/drug/stock/out    | 出库         |
| GET    | /api/drug/stock/warn   | 库存预警列表 |
| GET    | /api/drug/stock/expire | 效期预警列表 |
| POST   | /api/pharmacy/dispense | 发药         |
| POST   | /api/pharmacy/return   | 退药         |
| GET    | /api/pharmacy/prescription/list?statuses= | 处方列表（阶段3 补充：发药页 tabs 数据源，默认 1,2 待发药） |

**阶段3 实现说明**：① 库存模型为**一药一行**（`biz_stock` 唯一键 `uk_drug_id`，多批次拆分为扩展）：入库 upsert 原子累加数量，批号/生产日期/有效期非空才覆盖（空值保留旧值）；新增药品自动建库存行（数量 0、预警默认 50）；② 药品编码删除后**不可重建同码**（`uk_drug_code` 不含逻辑删除行，与 username 同族语义）；有库存不可删除（库存 0 删除时库存行同步逻辑删除）；③ 库存扣减用 DB 原子 UPDATE（`quantity >= n` 条件内扣，防负库存）；发药为**整单发放**，同药多明细行按 drugId 汇总一批扣减，任一药品不足则整单失败回滚（含已扣部分）；④ 预警口径：列表状态 pill 优先级 **近效期 > 库存预警 > 正常**；近效期 = 有效期 ≤ 今日 + 90 天；「库存预警 N 项」= 低库存列表与近效期列表长度之和（同药双命中不去重）；筛选仅按**药品启停状态**下推，库存预警/近效期只做视觉呈现与预警卡（避免伪分页）；⑤ 处方状态衔接：发药 `1已审核 / 2已收费 → 3已发药`（并发下状态迁移命中 0 行则整单回滚）；退药仅 `3 → 4已退`，未发药的退药语义为作废（走门诊作废）；**退药不退款**备忘：当前退药仅回补库存，⑧收费落地时复核 3→4 与退费顺序；⑥ 发药/退药请求体为处方号 `{ "prescNo": "PR..." }`；发药写出库流水（type=2，备注关联处方号）、退药写入库流水（type=1），发药/退药按钮无独立菜单行时沿用 pharmacy:dispense:add / pharmacy:dispense:return；⑦ 库存分页（/api/drug/stock/page）与药品分页（/api/drug/page）返回**同构聚合数据**（药品全字段 + 库存字段 + stockStatus），复用同一查询。

### 7.3 原型图

text

```
┌──────────────────────────────────────────────────────────┐
│ 药房管理 > 药品库存                                      │
│ ┌──────────────────────────────────────────────────────┐│
│ │ [+新增药品] [入库] [出库] 名称:[   ] 分类:[▼] [查询] ││
│ ├──────────────────────────────────────────────────────┤│
│ │ 编码  名称   规格  库存  预警 有效期    操作         ││
│ │ D001 阿莫西林 0.25g  500  100  2026-01  编辑 入库    ││
│ │ D002 布洛芬   0.2g   80   100  2025-12  编辑 入库 ⚠ ││
│ │ D003 头孢克肟 0.1g   300  100  2026-05  编辑 入库    ││
│ └──────────────────────────────────────────────────────┘│
│  ⚠ 库存预警:布洛芬库存不足                              │
│                                                          │
│ ┌── 门诊发药 ─────────────────────────────────────────┐ │
│ │ 处方号:[PR20250101001] 患者:张三                    │ │
│ │ ┌──────────────────────────────────────────────┐    │ │
│ │ │ 药品   规格  数量 单价 金额  发药            │    │ │
│ │ │ 阿莫西林 0.25g 15  2.5  37.5  [✓]           │    │ │
│ │ └──────────────────────────────────────────────┘    │ │
│ │ 合计:￥37.50          [退药] [确认发药]             │ │
│ └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```



------

## 八、检查检验管理模块（LIS/PACS）

### 8.1 字段设计

**检查申请表 biz_exam_apply**

| 字段               | 类型         | 说明                           |
| :----------------- | :----------- | :----------------------------- |
| id                 | bigint       | 主键                           |
| apply_no           | varchar(30)  | 申请单号 EXAM+日期+序号        |
| registration_id    | bigint       | 就诊ID（阶段3④门诊扩展列：门诊开单关联挂号记录） |
| patient_id         | bigint       | 患者ID                         |
| doctor_id          | bigint       | 申请医生                       |
| exam_type          | tinyint      | 1检验2检查(影像)               |
| exam_item          | varchar(100) | 项目名称                       |
| body_part          | varchar(50)  | 检查部位                       |
| clinical_diagnosis | varchar(500) | 临床诊断                       |
| status             | tinyint      | 0待缴费1待采样2检验中3已出报告 |
| apply_time         | datetime     | 申请时间                       |

**检验报告 biz_exam_report**（一申请一报告，`uk_apply` 唯一）

| 字段                   | 类型         | 说明                                                   |
| :--------------------- | :----------- | :----------------------------------------------------- |
| id                     | bigint       | 主键                                                   |
| apply_id               | bigint       | 申请ID                                                 |
| report_no              | varchar(30)  | 报告号 RP+日期+序号                                    |
| result                 | text         | 检验结果（JSON 明细数组 [{"name","value","ref","unit","abnormal"}]） |
| conclusion             | varchar(500) | 结论                                                   |
| is_critical            | tinyint      | 是否危急值 0否1是                                      |
| reporter_id            | bigint       | 报告人                                                 |
| auditor_id             | bigint       | 审核人（非空即已审核）                                 |
| report_time            | datetime     | 报告时间（发布时刻）                                   |
| status                 | tinyint      | 0草稿1已发布                                           |
| attachment_url         | varchar(500) | 报告附件URL（S3，扩展列）                              |
| critical_handled       | tinyint      | 危急值是否已处理（扩展列）                             |
| critical_handle_remark | varchar(500) | 危急值处理备注（扩展列）                               |
| critical_handle_time   | datetime     | 危急值处理时间（扩展列）                               |
| critical_handler_id    | bigint       | 危急值处理人（扩展列）                                 |

### 8.2 接口文档

| 方法 | 路径                          | 说明                                                     |
| :--- | :---------------------------- | :------------------------------------------------------- |
| POST | /api/exam/apply               | 开检查申请（自 /api/clinic/exam 迁入，门诊开单同用）     |
| GET  | /api/exam/apply/page          | 申请分页（registrationId/status/examItem/keyword 可选）  |
| POST | /api/exam/sample              | 样本采集（body `{applyId}`，1待采样→2检验中）            |
| POST | /api/exam/report              | 录入结果（body 含 applyId/items/conclusion/isCritical/attachmentUrl；草稿 upsert） |
| PUT  | /api/exam/report/{id}/audit   | 报告审核（写 auditor_id，状态仍 0草稿）                  |
| PUT  | /api/exam/report/{id}/publish | 报告发布（0→1 并联动检查单 2→3）                         |
| GET  | /api/exam/report/{id}         | 报告详情（明细反序列化 + 姓名/患者聚合）                 |
| POST | /api/exam/critical/handle     | 危急值处理（body `{reportId, remark}`）                  |
| GET  | /api/exam/items               | 项目下拉（补充接口，distinct 项目名）                    |

**阶段4 实现说明**：① **状态机**：检查单 `0待缴费 --收费--> 1待采样 --采样--> 2检验中 --报告发布--> 3已出报告`（退费 `1→0` 不变；已采样(2)/已出报告(3)锁死整单退费，退费文案区分「检查单已采样/已出报告，不可退费」）；报告 `0草稿 --审核(写 auditor_id，状态仍 0)--> --发布--> 1已发布`；② **报告录入为草稿 upsert**（一申请一报告 `uk_apply`，重复保存覆盖；已审核/已发布 400 不可改；保存事务锁申请行、锁序与发布一致防死锁）；`result` 存 **JSON 明细数组**（序列化落库/反序列化返回，`[{"name","value","ref","unit","abnormal"}]`）；报告号 `RP+yyyyMMdd+%04d` 先插后回补；③ **报告附件走 S3（RustFS）**：复用 `POST /api/file/upload`（白名单图片/PDF），URL 存 `attachment_url`（后端白名单 http/https 防注入）；④ **危急值闭环**：录入时标记 `is_critical`，报告发布后由审核权限角色「处理」（写处理人/时间/备注四列，重复处理 400）；⑤ **权限四件套**：菜单 **56-58**（`exam:apply:add` 开单=doctor / `exam:report:add` 采样+录入=nurse / `exam:report:audit` 审核+发布+危急值处理=doctor），parent_id=10；role_menu 段从 **226** 起（库中因角色管理 UI 保存重建已占用至 225）；计划书 §9.5 简写（exam:apply:add / report:add）定稿为三码，docs 声明；dean 只读、cashier/pharmacist 无；⑥ **数据权限**：apply/page 经 DataScopeProvider（SELF→doctor_id、DEPT→科室挂号子查询、ALL 放行；按就诊查询时逐就诊访问断言）；⑦ **接口迁移**：`/api/clinic/exam`×2 删除，门诊开检查单/列表改用本模块接口（门诊页开单按钮权限码改 exam:apply:add）；⑧ 检查单状态推进收口本模块（采样 1→2、发布 2→3 原子迁移命中 0 行回滚）。

### 8.3 原型图

text

```
┌──────────────────────────────────────────────────────────┐
│ 检查检验 > 检验工作台                                    │
│ ┌──────────────────────────────────────────────────────┐│
│ │ 状态:[待检验▼] 项目:[血常规▼] [查询]                 ││
│ ├──────────────────────────────────────────────────────┤│
│ │ 申请号  患者 项目   部位 状态   时间     操作        ││
│ │ E001   张三 血常规 -    检验中 09:00   录入结果      ││
│ │ E002   李四 胸片  胸部 待采样 09:05   采样          ││
│ └──────────────────────────────────────────────────────┘│
│                                                          │
│ ┌── 结果录入 (E001 张三 血常规)───────────────────────┐ │
│ │ ┌──────────────────────────────────────────────┐    │ │
│ │ │ 项目      结果   参考值     单位   异常       │    │ │
│ │ │ WBC       12.5   4-10      10^9/L  ↑          │    │ │
│ │ │ RBC       4.5    3.5-5.5   10^12/L            │    │ │
│ │ │ HGB       130    110-160   g/L                │    │ │
│ │ └──────────────────────────────────────────────┘    │ │
│ │ 结论:[白细胞偏高，提示感染          ]                │ │
│ │ 危急值: ○是 ●否                                      │ │
│ │                    [保存草稿] [提交审核]             │ │
│ └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```



------

## 九、收费与财务管理模块

### 9.1 字段设计

**收费单 biz_charge**

| 字段            | 类型          | 说明                          |
| :-------------- | :------------ | :---------------------------- |
| id              | bigint        | 主键                          |
| charge_no       | varchar(30)   | 收费单号                      |
| patient_id      | bigint        | 患者ID                        |
| visit_id        | bigint        | 就诊ID                        |
| charge_type     | tinyint       | 1挂号2门诊3住院               |
| total_amount    | decimal(10,2) | 总金额                        |
| discount_amount | decimal(10,2) | 优惠金额                      |
| actual_amount   | decimal(10,2) | 实收金额                      |
| pay_type        | tinyint       | 1现金2微信3支付宝4医保5银行卡 |
| pay_status      | tinyint       | 0待支付1已支付2已退费         |
| operator_id     | bigint        | 收费员                        |
| charge_time     | datetime      | 收费时间                      |

**收费明细 biz_charge_item**

| 字段      | 类型          | 说明                 |
| :-------- | :------------ | :------------------- |
| id        | bigint        | 主键                 |
| charge_id | bigint        | 收费单ID             |
| item_type | tinyint       | 1药品2诊疗3检查4床位 |
| item_name | varchar(100)  | 项目名称             |
| quantity  | int           | 数量                 |
| price     | decimal(10,2) | 单价                 |
| amount    | decimal(10,2) | 金额                 |

**发票表 biz_invoice**

| 字段         | 类型          | 说明       |
| :----------- | :------------ | :--------- |
| id           | bigint        | 主键       |
| invoice_no   | varchar(50)   | 发票号     |
| charge_id    | bigint        | 收费单ID   |
| invoice_type | tinyint       | 1电子2纸质 |
| amount       | decimal(10,2) | 金额       |
| status       | tinyint       | 0正常1作废 |
| print_time   | datetime      | 打印时间   |

### 9.2 接口文档

| 方法 | 路径                    | 说明         |
| :--- | :---------------------- | :----------- |
| POST | /api/charge             | 收费         |
| GET  | /api/charge/page        | 收费记录分页 |
| POST | /api/charge/{id}/refund | 退费         |
| GET  | /api/charge/{id}/detail | 收费明细     |
| POST | /api/invoice/print      | 发票打印     |
| PUT  | /api/invoice/{id}/void  | 发票作废     |
| GET  | /api/finance/daily      | 日结         |
| GET  | /api/finance/monthly    | 月结         |
| POST | /api/insurance/settle   | 医保结算     |
| GET  | /api/charge/pending     | 待收费列表（阶段3 补充：收费台数据源） |
| GET  | /api/charge/pending/{visitId} | 待收费明细（阶段3 补充：收费确认弹窗，含挂号费首项） |

**阶段3 实现说明**：① **整单收费模型**：收费台列当日未退号且有待收项的就诊，一键收该就诊全部待收项（**挂号费（首单含）+ 已审核处方明细 + 待缴费检查单**），一次收费一笔 biz_charge（charge_type=2 门诊）；金额由服务端按处方快照价/检查单 fee/挂号费 reg_fee 重算，不收客户端 items（防篡改）；② **挂号费首单规则**：该就诊尚无 `pay_status=1` 收费单时待收项含挂号费（item_type=2、source_id 置空）；已退费（pay_status=2）不算已收，可重收；仅挂号费就诊亦可独立收费；③ **处方状态衔接**：收费 `1已审核→2已收费`；退费 `2→4`（**终态**，防回退 1 后重回待发药/待收费形成循环）、`3已发药→400「先到药房退药」`（与⑥退药 3→4 链咬合，退药后 4 状态处方退费仅退钱不动状态）；检查单收费 `0→1`、退费 `1→0`（已进入采样流程则不可退）；挂号状态不随退费变动（退号需手工走退号）；④ **表扩展列**（文档原文之外，均已落库）：biz_charge 加 `patient_name`（凭证快照）、`refund_time`、`refund_operator_id`；biz_charge_item 加 `source_id`（处方/检查单 ID，退费精确回退）；`biz_exam_apply` 加 `fee`（检查定价，开检查单时录入，08_charge.sql 幂等加列）；⑤ **发票一单一有效**：重复开票 400、作废后可重开；退费时有效发票自动作废；⑥ **并发防护**：收费事务首步行锁就诊（防同一就诊并发收费），处方/检查状态原子迁移命中 0 行即整单回滚；退费以 `pay_status 1→2` 条件更新仲裁；⑦ **MQ**：收费成功于事务提交后发 `charge.paid`（exchange `medical.topic`，⑪消息模块落地后接消费方；发送失败仅告警不阻断）；⑧ **医保结算为课程级试算**（仅医保支付且已支付单据，按 70% 模拟返回报销/自付，不落库）；⑨ **日结口径**：收款 = charge_time 在周期内且 `pay_status∈{1,2}`（当日收当日退不退单，退款另计，防双重扣减）；退款 = refund_time 在周期内；⑩ `pay_status=0 待支付` 为扩展预留（当前直收即 1）；⑪ **D 级数据权限**：charge/page 已接 DataScopeProvider（SELF→仅本人操作单），当前 cashier 保持 ALL（③约定：收费员需全院排班，勿改 SELF，否则排班页失效），机制保留可手配；⑫ ⑦检查检验落地后，已采样/出报告的检查单将锁死整单退费（先声明）。

### 9.3 原型图

text

```
┌──────────────────────────────────────────────────────────┐
│ 收费管理 > 门诊收费                                      │
│ ┌──────────────────────────────────────────────────────┐│
│ │ 患者:张三  就诊号:V001  科室:内科                    ││
│ │ ┌──────────────────────────────────────────────┐    │ │
│ │ │ 项目        类型  数量 单价  金额             │    │ │
│ │ │ 阿莫西林    药品  15   2.5   37.5            │    │ │
│ │ │ 血常规      检查  1    30    30.0            │    │ │
│ │ └──────────────────────────────────────────────┘    │ │
│ │ 合计:￥67.50  优惠:￥0.00  实收:￥67.50            │ │
│ │ 支付方式: ○现金 ●微信 ○支付宝 ○医保 ○银行卡       │ │
│ │              [退费] [确认收费]                       │ │
│ └──────────────────────────────────────────────────────┘│
│                                                          │
│ ┌── 日结汇总 (2025-01-01)─────────────────────────────┐ │
│ │ 收费笔数:120  总收入:￥8,650.00                      │ │
│ │ 现金:￥1,200  微信:￥3,500  支付宝:￥
```

------

## 十、公共卫生与健康管理模块

### 10.1 字段设计

**健康档案 biz_health_record**

| 字段            | 类型         | 说明             |
| :-------------- | :----------- | :--------------- |
| id              | bigint       | 主键             |
| patient_id      | bigint       | 患者ID           |
| record_no       | varchar(30)  | 档案号           |
| chronic_disease | varchar(255) | 慢病（逗号分隔） |
| smoking         | tinyint      | 吸烟 0否1是      |
| drinking        | tinyint      | 饮酒 0否1是      |
| exercise        | varchar(50)  | 运动频率         |
| create_time     | datetime     | 建档时间         |

**疫苗接种表 biz_vaccination**

| 字段           | 类型         | 说明         |
| :------------- | :----------- | :----------- |
| id             | bigint       | 主键         |
| patient_id     | bigint       | 患者ID       |
| vaccine_name   | varchar(100) | 疫苗名称     |
| batch_no       | varchar(50)  | 批号         |
| dose_no        | int          | 剂次         |
| vaccinate_date | date         | 接种日期     |
| operator_id    | bigint       | 接种人       |
| next_date      | date         | 下次接种日期 |

**传染病上报表 biz_infectious_report**

| 字段          | 类型         | 说明           |
| :------------ | :----------- | :------------- |
| id            | bigint       | 主键           |
| patient_id    | bigint       | 患者ID         |
| disease_name  | varchar(100) | 传染病名称     |
| icd_code      | varchar(20)  | ICD编码        |
| report_date   | date         | 报告日期       |
| reporter_id   | bigint       | 报告人         |
| report_status | tinyint      | 0待上报1已上报 |
| cdc_response  | varchar(255) | 疾控反馈       |

**体检记录表 biz_health_checkup**（阶段5 补建——文档有 checkup 接口无表设计，字段为施工自拟）

| 字段         | 类型         | 说明                       |
| :----------- | :----------- | :------------------------- |
| id           | bigint       | 主键                       |
| patient_id   | bigint       | 患者ID                     |
| checkup_no   | varchar(30)  | 体检单号 TJ+序号           |
| checkup_date | date         | 体检日期                   |
| checkup_type | varchar(50)  | 体检类型（入职/常规/老年） |
| exam_items   | varchar(500) | 检查项目（逗号分隔）       |
| exam_result  | varchar(500) | 检查结果摘要               |
| doctor_id    | bigint       | 检查医生                   |
| conclusion   | varchar(255) | 体检结论                   |

> 公共字段说明：以上四表除文档原列外，均按项目规范补齐 `update_time` / `deleted`（`create_time` 即健康档案的建档时间）。

### 10.2 接口文档

| 方法 | 路径                           | 说明           |
| :--- | :----------------------------- | :------------- |
| GET  | /api/health/record/page        | 健康档案分页   |
| POST | /api/health/record             | 新建档案       |
| PUT  | /api/health/record/{id}        | 更新档案       |
| GET  | /api/health/vaccination/page   | 接种记录分页   |
| POST | /api/health/vaccination        | 新增接种记录   |
| GET  | /api/health/vaccination/remind | 接种提醒列表   |
| POST | /api/health/infectious/report  | 传染病上报     |
| GET  | /api/health/infectious/page    | 传染病记录分页 |
| GET  | /api/health/checkup/page       | 体检记录分页   |
| POST | /api/health/checkup            | 新增体检记录   |

### 10.3 原型图

text

```
┌──────────────────────────────────────────────────────────┐
│ 公共卫生 > 健康档案                                      │
│ ┌──────────────────────────────────────────────────────┐│
│ │ 姓名:[   ] 档案号:[   ] 慢病:[▼] [查询] [+新建档案]  ││
│ ├──────────────────────────────────────────────────────┤│
│ │ 档案号  姓名 性别 年龄 慢病     吸烟 饮酒  操作      ││
│ │ H0001  张三  男  34  高血压    是   否   编辑 详情  ││
│ │ H0002  李四  女  28  糖尿病    否   否   编辑 详情  ││
│ └──────────────────────────────────────────────────────┘│
│                                                          │
│ ┌── 疫苗接种记录 (张三)──────────────────────────────┐  │
│ │ 疫苗名称    批号    剂次 接种日期   下次日期  操作 │  │
│ │ 新冠疫苗    B2024   1    2024-01-01 2024-02-01 详情│  │
│ │ 流感疫苗    F2024   1    2024-10-01 2025-10-01 详情│  │
│ │ [+新增接种]                                        │  │
│ └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

**阶段5 实现说明**：① **体检表为缺口补建**（文档有 `/api/health/checkup` 接口无表设计，`biz_health_checkup` 字段为施工自拟，见 10.1）；② **一人一档**在 Service 层校验（insert 前查患者是否已有档案 → 400「该患者已建立健康档案」；不加唯一键，逻辑删除后可重建）；档案号 `H%05d`、体检单号 `TJ%05d`（先插后回补，同患者编号先例）；③ **传染病上报 POST 直写 `report_status=1`**（上报即建档；文档 `0待上报` 为预留态仅种子演示）；④ **接种提醒口径**：`next_date ∈ [今日, 今日+7 天]` 升序返回（窗口常量 REMIND_DAYS=7）；⑤ **档案分页筛选**：姓名经患者表下推 id 集（无匹配直接返回空页，避免空 in 集退化全量）、档案号 like、慢病 `FIND_IN_SET` 精确匹配（多选固定选项集写入，见⑦）；⑥ **权限四件套**：按钮菜单 **63-67**（`health:record:add / health:record:edit / health:vaccination:add / health:infectious:report / health:checkup:add`，parent_id=11，计划书 §9.5 补录），role_menu 从 **252** 起：admin/doctor/nurse 全给、dean 只读、cashier/pharmacist 无；⑦ **前端差异（docs 声明）**：原型为「健康档案」单页，传染病上报/体检记录为页内 Tab 补充（接口齐备而原型未画）；档案[详情]复用列表行数据（无独立详情接口）；疫苗接种子表随档案行选中联动（patientId 必传）；慢病为固定选项集多选（与 FIND_IN_SET 配套）；接种提醒条为原型外补充（承接 remind 接口）；`/api/health/vaccination/page` 患者ID必传；列表增运动频率/建档时间列（信息价值补充）。



------

## 十一、统计报表模块

### 11.1 字段设计

**报表配置表 sys_report_config**（阶段5 承载自定义报表设计器配置）

| 字段        | 类型          | 说明                                          |
| :---------- | :------------ | :-------------------------------------------- |
| id          | bigint        | 主键                                          |
| report_name | varchar(100)  | 报表名称                                      |
| report_type | varchar(50)   | 报表类型（自定义）                            |
| query_sql   | text          | 查询SQL（展示用，服务端由白名单构建生成）     |
| params      | varchar(2000) | 参数配置（设计器配置 JSON，阶段5 自 500 扩容） |
| creator_id  | bigint        | 创建人                                        |
| create_time | datetime      | 创建时间                                      |
| update_time | datetime      | 更新时间（公共字段补充）                      |
| deleted     | tinyint       | 逻辑删除（公共字段补充）                      |

**报表数据缓存表 sys_report_data**（预留：三期自定义报表数据缓存，本期接口未用）

| 字段        | 类型     | 说明                     |
| :---------- | :------- | :----------------------- |
| id          | bigint   | 主键                     |
| report_id   | bigint   | 报表ID                   |
| stat_date   | date     | 统计日期                 |
| data_json   | text     | 数据JSON                 |
| create_time | datetime | 生成时间                 |
| update_time | datetime | 更新时间（公共字段补充） |
| deleted     | tinyint  | 逻辑删除（公共字段补充） |

### 11.2 接口文档

| 方法 | 路径                   | 说明                                                         |
| :--- | :--------------------- | :----------------------------------------------------------- |
| GET  | /api/report/outpatient | 门诊统计（total/trend/deptDist，支持日期区间/科室/粒度）     |
| GET  | /api/report/inpatient  | 住院统计（在院/床位使用率/出院人次/平均住院日）              |
| GET  | /api/report/income     | 收入统计（总额/门诊/住院/药品/检查/退费/构成/科室/趋势）     |
| GET  | /api/report/drug       | 药品统计（销售 TOP10 + 药品收入占比，随科室筛选）            |
| GET  | /api/report/patient    | 患者分析（年龄分段/性别/地域/病种分布）                      |
| GET  | /api/report/custom/datasets | 数据集元数据（白名单字段清单，阶段5 补充接口）         |
| POST | /api/report/custom/execute  | 执行自定义查询（选字段+条件+分组聚合+排序，阶段5 补充接口） |
| POST | /api/report/custom     | 保存自定义报表（{reportName, config}，config 白名单校验后存 params/query_sql） |
| GET  | /api/report/custom/page | 已保存配置分页（补充接口，仅自定义类型）                    |
| GET  | /api/report/custom/{id} | 已保存配置详情（含 config 解析回填设计器，补充接口）        |
| DELETE | /api/report/custom/{id} | 删除已保存配置（逻辑删，补充接口）                       |
| POST | /api/report/custom/export | 自定义报表导出（**CSV**，body=查询配置，阶段5 补充接口） |
| GET  | /api/report/export     | 导出报表（**CSV**，type=outpatient/inpatient/income/drug/patient） |
| GET  | /api/report/overview   | 数据总览聚合（补充接口，**仅要求登录态**——矩阵首行六角色全开） |
| GET  | /api/report/depts      | 科室筛选下拉（补充接口）                                     |

**阶段4 实现说明**：① **筛选参数**：`startDate/endDate`（yyyy-MM-dd，缺省本月 1 日至今日；end<start → 400）、`deptId`（可选；门诊直接过滤挂号、收入/药品经挂号→科室映射过滤）、`granularity`（day/week/month，缺省 day；**周为周一起始日**，非法值 → 400）；② **统计口径**：门诊量 = `biz_registration` reg_time∈区间且 status≠3（退号不计）；收入收款口径同 ⑧ 日结（`charge_time`∈区间且 pay_status∈{1,2}，退款按 `refund_time` 单独计 refundAmount）；构成按 `charge_item.item_type`（药品/诊疗/检查/床位，**BigDecimal 保留两位**）；住院住院收入（charge_type=3）当前恒 0（住院结算后续阶段）；药品 TOP10 = 处方明细（处方 create_time∈区间且 status∈{2,3}）按 drug_id 聚合金额降序，分类经药品目录映射；患者年龄段边界 ≤18/≤40/≤60/>60（未来生日按脏数据归「未知」），地域取 address 前 3 字（文本近似），病种取病历诊断文本 TOP10；住院平均住院日按区间内终结记录（status∈{1,2}，均含 discharge_time）均值；③ **导出为 CSV**（BOM + Excel 中文兼容；PDF 不做——docs 声明；文件名 ASCII）；④ **跨模块取数全部经桥**：新增 `ReportSourceProvider`（clinic：门诊聚合/药品销售 SQL 下推/挂号→科室映射/候诊数）、`InpatientStatsProvider`（inpatient），`PatientProvider` 增 stats、`DrugProvider` 增 drugCategories/stockWarnCount；⑤ 权限：读接口 + custom 挂 `report:view`（矩阵：统计报表仅 admin+dean），export 挂 **`report:export`**（菜单 59，role_menu 232-233 从 231 续排），**overview 仅登录态**；⑥ **数据总览页**（矩阵独立行，六角色全开）：KPI 四卡（今日门诊量/在院人数/今日收入/库存预警）＋近 7 日门诊量趋势（ECharts，缺失日补 0）＋今日收款构成＋今日科室人次分布＋待办与预警（待收费/候诊/库存预警，均按权限显示）＋快捷入口（按 view 权限过滤）；⑦ **前端图表全用 ECharts**（计划书要求；`utils/echarts.js` 按需注册，颜色从 design-system 令牌解析联动；排名条为原型同款 CSS hbar）；⑧ **原型差异（docs 声明）**：报表页省「报表类型」下拉（综合页全量展示）、无环比 delta、不做[打印]、[生成报表]=保存筛选配置、[导出 Excel] 为 CSV 下载；原型页头图标元素以圆点/文本符替代；⑨ 报表页与数据总览页于阶段4 一并交付（原型的 reports.html/index.html 为布局依据）。

**阶段5 实现说明（自定义报表设计器，计划书 :426「选字段+条件→出图/表+导出」）**：① **数据集白名单方案（绝不执行用户 SQL）**：3 个数据集读本模块本地表——`charge` 收费单（chargeNo/patientName/chargeType/payType/payStatus/金额三列/chargeTime/refundTime）、`chargeItem` 收费明细（chargeId/itemType/itemName/quantity/price/amount）、`invoice` 发票（invoiceNo/chargeId/invoiceType/amount/status/printTime）；列名/表名/运算符/聚合函数全部取自服务端注册表，查询经 QueryWrapper 参数化构建，`LIMIT 1000` 常量兜底；② **配置 JSON 键表**：`{dataset, fields[], conditions[{field,op,value,value2}], groupBy[], aggregations[{field,func}], orderBy:{field,dir}, chartType}`——op 白名单 `= != like > < >= <= between`（between 用 value+value2）；func 白名单 `count/sum/avg/max/min`（sum/avg 限数值字段）；dir 仅 asc/desc；③ **分组统计时查询字段须全部为分组字段**（防 ONLY_FULL_GROUP_BY，违反 400 明确提示；前端预览前同规则预检）；④ `query_sql` 存**展示用** SQL（select 白名单片段+参数占位符替换为字面量，不用于执行）；params 列 500→2000 幂等扩容；⑤ **权限零新增种子**：datasets/execute/page/{id}/DELETE/保存 挂 `report:view`、导出挂 `report:export`（:533 定稿）；⑥ **前端差异（docs 声明）**：报表页改双 Tab（预置报表｜自定义报表）；**阶段4「生成报表」弹窗（保存筛选快照）已移除**，职责由设计器「保存配置」承接（原保存接口请求体由扁平筛选 JSON 改为 `{reportName, config}` 结构化，旧格式不再兼容）；设计器支持 表格/折线/柱状/饼图 四种呈现与 CSV 导出；已保存配置可 加载（回填设计器，编辑=加载后另存）/删除；数据集元数据接口不暴露表名与列名。

### 11.3 原型图

text

```
┌──────────────────────────────────────────────────────────┐
│ 统计报表 > 收入统计                                      │
│ ┌──────────────────────────────────────────────────────┐│
│ │ 时间:[2025-01-01 ~ 2025-01-31] 科室:[全部▼] [查询]  ││
│ ├──────────────────────────────────────────────────────┤│
│ │ 门诊收入:￥120,000  住院收入:￥350,000              ││
│ │ 药品收入:￥180,000  检查收入:￥90,000               ││
│ │ 总收入:  ￥470,000                                   ││
│ ├──────────────────────────────────────────────────────┤│
│ │      收入趋势图（折线）                              ││
│ │  ￥ │        ╱╲                                       ││
│ │     │    ╱╲╱  ╲╱╲                                   ││
│ │     │╱╲╱         ╲                                   ││
│ │     └───────────────────► 日期                       ││
│ ├──────────────────────────────────────────────────────┤│
│ │ 科室    门诊收入  住院收入  合计    占比             ││
│ │ 内科    ￥30,000  ￥80,000  ￥110,000 23.4%         ││
│ │ 外科    ￥25,000  ￥120,000 ￥145,000 30.9%         ││
│ │ 儿科    ￥18,000  ￥50,000  ￥68,000  14.5%         ││
│ └──────────────────────────────────────────────────────┘│
│                             [导出Excel] [导出PDF]        │
└──────────────────────────────────────────────────────────┘
```



------

## 十二、消息与通知模块

### 12.1 字段设计

**站内消息表 sys_message**

| 字段        | 类型         | 说明                      |
| :---------- | :----------- | :------------------------ |
| id          | bigint       | 主键                      |
| receiver_id | bigint       | 接收人ID                  |
| title       | varchar(100) | 标题                      |
| content     | text         | 内容                      |
| msg_type    | tinyint      | 1系统2待办3预警（3 为预留）|
| is_read     | tinyint      | 0未读1已读                |
| create_time | datetime     | 创建时间                  |
| update_time | datetime     | 更新时间（公共字段补充）  |
| deleted     | tinyint      | 逻辑删除（公共字段补充）  |

**短信记录表 sys_sms_record**

| 字段          | 类型         | 说明                     |
| :------------ | :----------- | :----------------------- |
| id            | bigint       | 主键                     |
| phone         | varchar(20)  | 手机号                   |
| template_code | varchar(50)  | 模板编码                 |
| content       | varchar(500) | 短信内容                 |
| send_status   | tinyint      | 0失败1成功               |
| send_time     | datetime     | 发送时间                 |
| create_time   | datetime     | 创建时间（公共字段补充） |
| update_time   | datetime     | 更新时间（公共字段补充） |
| deleted       | tinyint      | 逻辑删除（公共字段补充） |

**公告表 sys_notice**

| 字段         | 类型         | 说明                     |
| :----------- | :----------- | :----------------------- |
| id           | bigint       | 主键                     |
| title        | varchar(100) | 公告标题                 |
| content      | text         | 公告内容                 |
| notice_type  | tinyint      | 1院内2科室               |
| dept_id      | bigint       | 科室ID（可空）           |
| publisher_id | bigint       | 发布人                   |
| publish_time | datetime     | 发布时间                 |
| status       | tinyint      | 0草稿1已发布             |
| create_time  | datetime     | 创建时间（公共字段补充） |
| update_time  | datetime     | 更新时间（公共字段补充） |
| deleted      | tinyint      | 逻辑删除（公共字段补充） |

### 12.2 接口文档

| 方法   | 路径                       | 说明                                                     |
| :----- | :------------------------- | :------------------------------------------------------- |
| GET    | /api/message/page          | 消息分页（本人恒等收件人；msgType/isRead 可选筛选）       |
| PUT    | /api/message/{id}/read     | 标记已读（仅本人消息；已读重复点击幂等）                  |
| PUT    | /api/message/read-all      | 全部已读（本人）                                         |
| GET    | /api/message/unread-count  | 未读数（补充接口，顶栏铃铛红点）                          |
| GET    | /api/message/depts         | 科室下拉（补充接口，公告弹窗）                            |
| POST   | /api/sms/send              | 发送短信（模拟网关，写记录直记成功）                      |
| GET    | /api/sms/page              | 短信记录分页（phone/keyword 可选）                        |
| POST   | /api/notice                | 发布公告（status 缺省 1 发布；科室公告必选科室并校验存在）|
| GET    | /api/notice/page           | 公告分页（noticeType/deptId/keyword；含草稿带状态标记）   |
| DELETE | /api/notice/{id}           | 删除公告（逻辑删）                                        |
| GET    | /api/warning/list          | 预警列表（实时聚合：低库存/近效期/未处理危急值）           |

**阶段4 实现说明**：① **MQ 消费链（阶段3 埋点落地）**：RabbitMQConfig 新增常量队列 `medical.message.queue`（durable）+ 绑定 `medical.topic` / `charge.paid`；`ChargePaidListener` 消费（payload 含 chargeId/chargeNo/patientId/patientName/visitId/actualAmount/payTime/**operatorId（本阶段补入**，patientPhone 由消费侧经桥取）→ ① 站内信给收费操作员（msg_type=1、内容含单号/患者/实收金额）② 短信记录给患者（含单号；无手机号跳过；template_code=`CHARGE_PAID`、**模拟发送直记 send_status=1**）；**消费开关 `app.message.consumer.enabled`**（生产 application.yml 开；测试经 surefire systemProperty 全局关，消费闭环用例手动启停容器）；listener 业务异常 try-catch 记日志（**失败静默丢弃、非关键链路**，`default-requeue-rejected=false` 防重投循环；重复消费仅崩溃窗口、不做幂等）；② **预警列表为实时聚合**（不落 sys_message）：低库存 + 近效期（经药品桥复用药房预警口径）+ 未处理危急值（经检查桥：is_critical=1 且已发布且未处理）统一返回 `{type: low/expire/critical, title, detail, time}`；**msg_type=3 预警为文档预留类型**（当前预警=实时聚合）；③ 站内信分页/已读全部按 **receiver_id=当前用户恒等**（防越权读/改他人消息）；④ **权限四件套**：菜单 12（message:view）六角色全开（矩阵）；按钮菜单 **60**（`message:send`）、**61**（`notice:add`）、**62**（`notice:delete`——审查修订，发布与删除分码），role_menu 从 234 起（六角色全分配）；⑤ **原型差异（docs 声明）**：公告[编辑]无对应接口不做；**短信记录卡为原型外补充**（需求文档短信通知场景）；预警 Tab 为实时聚合列表（注记"实时聚合"）；公告表增范围/状态列；铃铛未读数仅页面加载时拉取（不做轮询），消息页已读动作经前端 store 同页同步红点。

### 12.3 原型图

text

```
┌──────────────────────────────────────────────────────────┐
│ 消息中心                                                 │
│ ┌──────────────────────────────────────────────────────┐│
│ │ [全部] [系统] [待办] [预警]        [全部已读]        ││
│ ├──────────────────────────────────────────────────────┤│
│ │ ● 系统通知:系统将于今晚22:00维护      2分钟前        ││
│ │ ● 待办提醒:您有3份病历待审核          10分钟前       ││
│ │ ○ 预警提醒:布洛芬库存不足             1小时前        ││
│ │ ○ 危急值:患者张三血钾异常             2小时前        ││
│ └──────────────────────────────────────────────────────┘│
│                                                          │
│ ┌── 公告管理 ─────────────────────────────────────────┐ │
│ │ [+发布公告]                                         │ │
│ │ 标题            类型  发布人  发布时间   操作       │ │
│ │ 春节放假通知    院内  管理员  2025-01-20 编辑 删除  │ │
│ │ 内科排班调整    科室  李主任  2025-01-18 编辑 删除  │ │
│ └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```



------

## 十三、数据安全与审计模块

### 13.1 字段设计

**操作日志表 sys_operation_log**

| 字段        | 类型         | 说明       |
| :---------- | :----------- | :--------- |
| id          | bigint       | 主键       |
| user_id     | bigint       | 操作用户   |
| username    | varchar(50)  | 用户名     |
| module      | varchar(50)  | 模块       |
| operation   | varchar(100) | 操作描述   |
| method      | varchar(200) | 请求方法   |
| params      | text         | 请求参数   |
| ip          | varchar(50)  | IP地址     |
| user_agent  | varchar(255) | 浏览器UA   |
| status      | tinyint      | 0失败1成功 |
| cost_time   | int          | 耗时(ms)   |
| create_time | datetime     | 操作时间   |

**登录日志表 sys_login_log**

| 字段       | 类型         | 说明       |
| :--------- | :----------- | :--------- |
| id         | bigint       | 主键       |
| username   | varchar(50)  | 用户名     |
| ip         | varchar(50)  | IP地址     |
| location   | varchar(100) | 登录地点   |
| browser    | varchar(100) | 浏览器     |
| os         | varchar(50)  | 操作系统   |
| status     | tinyint      | 0失败1成功 |
| msg        | varchar(255) | 提示信息   |
| login_time | datetime     | 登录时间   |

**数据备份表 sys_backup**

| 字段        | 类型         | 说明           |
| :---------- | :----------- | :------------- |
| id          | bigint       | 主键           |
| backup_name | varchar(100) | 备份名称       |
| backup_path | varchar(255) | 备份路径       |
| backup_size | bigint       | 文件大小(字节) |
| backup_type | tinyint      | 1手动2自动     |
| status      | tinyint      | 0失败1成功     |
| create_time | datetime     | 备份时间       |

**系统参数表 sys_config**（阶段5 补建，计划书第八章 :471 定稿；key-value 通用表）

| 字段         | 类型         | 说明                     |
| :----------- | :----------- | :----------------------- |
| id           | bigint       | 主键                     |
| config_key   | varchar(100) | 配置键（唯一）           |
| config_value | varchar(255) | 配置值                   |
| config_name  | varchar(100) | 配置名称                 |
| remark       | varchar(255) | 备注                     |

>> 三表（sys_operation_log / sys_login_log / sys_backup）均含公共字段 `update_time` / `deleted`（项目规范补齐）；安全配置种子五键：`security.fail-count=5`、`security.session-timeout=30`（分）、`security.ip-whitelist=''`、`security.two-factor=0`、`security.data-mask=1`。

### 13.2 接口文档

| 方法   | 路径                          | 说明                 |
| :----- | :---------------------------- | :------------------- |
| GET    | /api/audit/operation/page     | 操作日志分页         |
| GET    | /api/audit/operation/modules  | 操作日志模块下拉     |
| GET    | /api/audit/operation/export   | 操作日志导出         |
| GET    | /api/audit/login/page         | 登录日志分页         |
| GET    | /api/audit/login/export       | 登录日志导出         |
| POST   | /api/backup                   | 手动备份             |
| GET    | /api/backup/page              | 备份记录分页         |
| POST   | /api/backup/{id}/restore      | 数据恢复             |
| DELETE | /api/backup/{id}              | 删除备份             |
| GET    | /api/security/config          | 安全配置查询         |
| PUT    | /api/security/config          | 更新安全配置         |

### 13.3 原型图

text

```
┌──────────────────────────────────────────────────────────┐
│ 数据安全 > 操作日志                                      │
│ ┌──────────────────────────────────────────────────────┐│
│ │ 用户:[   ] 模块:[▼] 时间:[2025-01-01 ~ 2025-01-31]  ││
│ │                                     [查询] [导出]    ││
│ ├──────────────────────────────────────────────────────┤│
│ │ 用户   模块    操作         IP         状态  耗时   ││
│ │ admin  用户管理 新增用户    192.168.1.1 成功  120ms ││
│ │ doc01  门诊管理 开处方      192.168.1.5 成功  80ms  ││
│ │ admin  药房管理 删除药品    192.168.1.1 失败  50ms  ││
│ └──────────────────────────────────────────────────────┘│
│                                                          │
│ ┌── 数据备份 ─────────────────────────────────────────┐ │
│ │ [+手动备份]                                         │ │
│ │ 备份名称          类型  大小    时间        操作    │ │
│ │ backup_20250101  自动  128MB  2025-01-01  恢复 删除 │ │
│ │ backup_20250102  手动  130MB  2025-01-02  恢复 删除 │ │
│ └──────────────────────────────────────────────────────┘ │
│                                                          │
│ ┌── 安全配置 ─────────────────────────────────────────┐ │
│ │ 登录失败锁定次数: [5  ] 次                           │ │
│ │ 会话超时时间:     [30 ] 分钟                         │ │
│ │ IP白名单:         [192.168.1.0/24          ]         │ │
│ │ 双因素认证:       ●开启 ○关闭                        │ │
│ │ 敏感数据脱敏:     ●开启 ○关闭                        │ │
│ │                          [保存配置]                  │ │
│ └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

**阶段5 实现说明**：① **日志查询**：操作日志接口从系统管理内（死代码 /api/system/log/page + SysLogController）迁入本模块 `/api/audit/*`，筛选项=用户名（like）/模块（**精确**）/状态/时间区间，登录日志=用户名/状态/时间；模块下拉为 distinct 补充接口（sys_operation_log.module 已补索引）；② **导出为 CSV**（BOM + 公式注入防护，CsvUtils 自统计报表抽取至 common 复用；登录/操作日志导出含原型 [导出] 按钮对应，补充接口 docs 声明）；③ **备份为 Java 原生导出**（无外部命令依赖）：全库读 information_schema 表清单 → 每表 DROP/CREATE（DDL 换行归一化为单行）+逐行 INSERT 生成 SQL 文件（`app.backup.dir` 默认 `./backups`，文件名 backup[_auto]_yyyyMMdd_HHmmss.sql），**dump 在只读事务内执行保证一致性快照**；**文件排除 sys_backup 表**（备份元数据不入快照——恢复后备份历史保留，避免记录随恢复消失、文件成孤儿）；**恢复=逐条执行备份文件**（全库回滚语义；DDL 不可回滚，失败为半恢复态），前置校验记录与文件存在，**必带 body `{"confirm":true}` 防呆**（前端 ElMessageBox 二次确认）；删除=逻辑删记录+物理删文件（文件丢失容忍）；另加**每日自动备份**（`app.backup.auto.enabled` 开关 + `app.backup.auto.cron` 默认 `0 0 3 * * ?`，测试经 surefire 全局关）；④ **安全配置三项真生效**：登录失败锁定——Redis 计数（键 `medical:login:fail:{用户名}`），达 `security.fail-count` 次锁定**固定 10 分钟**（窗口不滑动），锁定期间正确密码亦拒绝，**成功登录清零**；密码错（含用户名不存在）计数，**密码正确但账号/角色禁用不计数**；会话超时（分钟）作用于**新登录**签发令牌（存量令牌不受影响）；数据脱敏——开启后**患者列表接口**手机号打码（138****5678，经 SecurityOptionsProvider 桥读取；详情/编辑回显保持原值），用户管理列表不做（admin 专属页且编辑直读列表行回显）；⑤ **IP 白名单**：过滤器直连取 `getRemoteAddr()`（**不取可伪造的 X-Forwarded-For**），空值不限制，支持逗号分隔精确 IP 与 IPv4 CIDR；非空且不含访问来源 IP 时**登录接口同样被拦**（自锁风险，前端保存时以 `currentIp` 提示）；⑥ **双因素认证仅存储回显**（前端标注「演示配置，暂不接入登录流程」，docs 声明）；⑦ **权限四件套**：按钮菜单 **68-70**（`audit:log:view` 日志查询+导出 / `backup:add` 备份+恢复+删除 / `security:config` 配置保存，parent_id=13），role_menu 从 **267** 起（仅 admin；矩阵「系统管理 / 安全审计」仅 admin）；backup/page 与 GET config 挂 audit:view；⑧ **原型差异（docs 声明）**：登录日志子 Tab 与操作日志段同卡切换（原型仅操作日志段）；安全配置增「当前访问 IP」提示行；恢复/删除均有二次确认弹窗（原型未画）。
注：以上实际为 12 个模块。文档中"公共卫生与健康管理"下含健康档案、疫苗接种、传染病上报、体检管理等子模块；若将 公共卫生 与 健康管理 拆分为两个独立模块，则为 13 个


------

## 附录：文档说明

| 项目     | 说明                                   |
| :------- | :------------------------------------- |
| 文档版本 | v1.0                                   |
| 编写日期 | 2025-01                                |
| 适用系统 | 医疗后台管理系统                       |
| 数据库   | MySQL 8.0                              |
| 命名规范 | 表名 `模块前缀_业务名`，字段小写下划线 |
| 接口规范 | RESTful，统一响应结构                  |
| 文档用途 | 需求评审、开发设计、测试用例编写       |

> 本文档为需求阶段设计稿，字段与接口可按实际业务进一步细化。如需某个模块的**详细ER图、时序图或完整接口参数**，可单独展开。