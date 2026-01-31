# 记账管理系统需求说明书（MVP）

## 1. 项目概述

- **技术栈**：
  - 前端：Vue 3 + Vite + TypeScript
  - 后端：Java 17+ Spring Boot
  - 数据库：SQLite（单机部署）
- **使用场景**：
  - 个人用户在 PC 浏览器上进行日常记账和查看统计。
- **首版目标（MVP）**：
  - 多用户、本地部署，无云同步。
  - 支持基本的账户管理、记账以及统计。
  - 支持按月和按年的统计。

## 2. 角色与使用场景

- **场景示例**：
  - 每月的某几天，一般是发工资后，用浏览器打开页面，用手机将所有的银行app，支付宝，微信app打开，核对一下账目的金额是否和之前有变化，如果有变化就更新。
  - 每月或到年底查看存款总额，存款分布图，以及存款趋势图。


    - 账户资金堆叠面积图（按账户堆叠的每月资金趋势）
### 3.1 账户管理

- **功能**：
  - 新增账户：名称，类型（银行，支付宝，微信，理财APP，股票），备注。
  - 编辑账户信息。
  - 删除账户：
    - 若已有账目引用该账户：禁止物理删除，可标记为"停用"，在新建账目时不再可选。
- **前端需求**：
  - 新增/编辑弹窗表单。
- **后端接口**：
  - `GET /api/accounts`：查询当前用户的账户列表
    - 响应字段：id, name, type, note, status（启用/停用）, createdAt, updatedAt
  - `POST /api/accounts`：创建账户
    - 请求体：{ "name": "招商银行", "type": "银行", "note": "工资卡" }
  - `PUT /api/accounts/{id}`：更新账户
    - 请求体：{ "name": "招商银行", "type": "银行", "note": "工资卡" }
  - `DELETE /api/accounts/{id}`：删除/停用账户
    - 若有关联存款记录，则标记为停用（status=DISABLED），不物理删除

### 3.2 对账管理

  - `GET /api/statistics/account-trend?period=6m|1y|3y|all`：账户资金堆叠趋势统计
    - 参数 period：6m（最近6个月）、1y（最近一年）、3y（最近3年）、all（全部）
    - 响应：
      ```json
      {
        "period": "1y",
        "months": ["2023-03", "2023-04"],
        "accounts": [
          {
            "accountId": 1,
            "accountName": "招商银行",
            "amounts": [50000.00, 52000.00]
          },
          {
            "accountId": 2,
            "accountName": "支付宝",
            "amounts": [30000.00, 32000.00]
          }
        ]
      }
      ```
    - 说明：
      - `months` 为时间轴月份列表
      - 每个月的金额按该月最后一次记录计算（逻辑同按月统计）
      - 如果某月没有记录，则使用上一个月的金额（沿用趋势统计的空月处理逻辑）
- **功能**：
  - 新建对账：系统会将最近一次的记账数据按照不同的账号显示
  - 用户可更新不同账号的记账数据。
  - 用户点击保存后，将对账数据保留在当天
  - 用户可以查看每次（不同天）的记账数据
- **前端需求**：
  - 首先用不同的标签页显示不同的账户数据，用户可以切换标签页查看数据
  - 每个账户数据是用表格显示，内容是关于存款记录，每个账户是可以有多个存款记录，包括存款类型，存款时间，金额，利率，存期，备注。支持分页
  - 新增/编辑存款弹窗：
    - 存款类型下拉选择：活期、定期、理财、其他
    - 存款时间默认为今天日期
    - 当选择"活期"时：
      - 利率自动设置为0%（因为活期利息不确定）
      - 利率输入框自动禁用
      - 存款时间输入框自动禁用，自动设置为当前日期
    - 当选择"定期"时：
      - 存期字段启用（必填）
      - 存期支持0.5年（6个月）、1年、2年、3年、5年
    - 当选择"理财"或"其他"时：
      - 存期字段禁用（不需要存期）
  - 存款列表表格：
    - 活期存款的存款时间列显示为 `-`（不显示具体日期）
- **后端接口**：
  - `GET /api/reconciliation?date=YYYY-MM-DD`：获取指定日期的对账数据
    - 参数 date 可选，如果不传则返回最近一次快照的日期（如果从未对账，使用今天的日期作为占位符）
    - 逻辑：
      - **判断是否有快照：只基于快照表（`reconciliation_snapshots`）中的记录判断，不依赖存款表**
      - 如果指定日期在快照表中有记录 → 返回该日期的快照数据（可编辑），包括快照备注、总金额和该日期的存款记录
      - 如果指定日期在快照表中没有记录 → 返回空数据（显示启用的账户，但无存款记录）
      - 说明：所有返回的对账数据都可以直接编辑，无需额外的保存步骤。存款记录的增删改会直接更新到数据库，快照总金额会自动同步
    - 响应结构：
      ```json
      {
        "date": "2024-02-01",
        "note": "快照备注（可选）",
        "totalAmount": 150000.00,
        "accounts": [
          {
            "accountId": 1,
            "accountName": "招商银行",
            "deposits": [
              {
                "id": 1,
                "depositType": "活期",
                "depositTime": "2024-01-15",
                "amount": 50000.00,
                "interestRate": 0.00,
                "term": null,
                "note": "工资卡余额"
              }
            ]
          }
        ]
      }
      ```
      - 说明：
        - `date`: 对账日期
        - `note`: 快照备注（可选）
        - `totalAmount`: 快照总金额
        - `accounts`: 账户列表及其存款记录
  
  - `GET /api/accounts/{accountId}/deposits?date=YYYY-MM-DD`：获取指定账户在指定日期的存款记录
    - 支持分页：`?page=1&size=20`
    - 说明：用于对账页面中单个账户标签页的数据展示
    - 响应：存款记录列表（带分页信息）
  
  - `POST /api/deposits`：创建存款记录（在对账页面新增一条存款记录）
    - 请求体：{ "accountId": 1, "depositType": "活期", "depositTime": "2024-02-15", "amount": 50000.00, "interestRate": 0.00, "term": null, "note": "工资卡余额", "reconciliationDate": "2024-02-01" }
    - 说明：
      - reconciliationDate 字段表示这条记录属于哪个日期的对账快照
      - term 字段为存期（年数），仅定期存款必填，取值范围1-10年
      - 当 depositType 为"活期"时，interestRate 应自动设为0
  
  - `PUT /api/deposits/{id}`：更新存款记录
    - 请求体：{ "depositType": "定期", "depositTime": "2024-02-15", "amount": 50000.00, "interestRate": 2.5, "term": 1, "note": "一年定期" }
    - 说明：term 字段仅定期存款必填，取值范围1-10年
  
  - `DELETE /api/deposits/{id}`：删除存款记录
  
  - `POST /api/reconciliation/create-new?date=YYYY-MM-DD`：新建对账
    - 参数 date：目标日期（必填）
    - 说明：将选中日期之前最近一次快照复制到选中日期并保存到数据库
    - 逻辑：
      - **检查目标日期是否已有快照：只基于快照表（`reconciliation_snapshots`）中的记录判断，不依赖存款表**。如果快照表中已有该日期的记录，则抛出异常（一个日期只能有一个快照）
      - 查找目标日期之前最近的一次快照日期（只基于快照表）
      - 如果有前一次快照：复制其所有存款记录到目标日期，计算总金额，创建快照记录（备注清空）
      - 如果没有前一次快照：创建空快照（总金额为0，没有存款记录，备注为空）
    - 响应：{ "message": "新建对账成功" }
  
  - `PUT /api/reconciliation/note?date=YYYY-MM-DD`：更新快照备注
    - 参数 date：快照日期（必填）
    - 请求体：{ "note": "快照备注内容" }
    - 说明：更新指定日期快照的备注
    - 响应：{ "message": "更新备注成功" }
  
  - `POST /api/reconciliation/save?date=YYYY-MM-DD`：保存对账快照（已废弃，前端不再使用）
    - 说明：此接口已废弃，现在所有对账数据的增删改都直接通过存款记录的增删改接口完成，无需单独保存快照
    - 保留此接口仅为了向后兼容
  
  - `GET /api/reconciliation/history`：查看历史对账记录
    - **只基于快照表（`reconciliation_snapshots`）返回已保存快照的日期列表**，不依赖存款表
    - 返回快照表中所有快照日期的列表，并统计每个日期的存款记录数
    - 响应：
      ```json
      {
        "dates": [
          { "date": "2024-02-01", "recordCount": 15 },
          { "date": "2024-01-01", "recordCount": 12 }
        ]
      }
      ```
      - `recordCount`：该日期在存款表中的存款记录数
  
  - `GET /api/reconciliation/latest`：获取最近一次对账日期
    - **只基于快照表（`reconciliation_snapshots`）获取最近一次对账日期**，不依赖存款表
    - 用于前端判断"最近一次对账"是哪一天
    - 响应：{ "date": "2024-01-01" } 或 { "date": null }（如果从未对账，即快照表中没有记录）

  - `GET /api/reconciliation/previous?date=YYYY-MM-DD`：获取上一个快照日期
    - 用于导航到上一个对账快照
    - **只基于快照表（`reconciliation_snapshots`）获取快照日期列表**，不依赖存款表
    - 参数 date：当前日期
    - 响应：上一个快照日期（字符串格式 "YYYY-MM-DD"）或 null（如果没有更早的快照）
    - 逻辑：
      - 从快照表中获取所有快照日期（按日期倒序）
      - 如果当前日期不在快照列表中，返回最近的小于当前日期的快照日期
      - 如果当前日期是最早的快照，返回 null

  - `GET /api/reconciliation/next?date=YYYY-MM-DD`：获取下一个快照日期
    - 用于导航到下一个对账快照
    - **只基于快照表（`reconciliation_snapshots`）获取快照日期列表**，不依赖存款表
    - 参数 date：当前日期
    - 响应：下一个快照日期（字符串格式 "YYYY-MM-DD"）或 null（如果没有更新的快照）
    - 逻辑：
      - 从快照表中获取所有快照日期（按日期倒序）
      - 如果当前日期不在快照列表中，返回最近的大于当前日期的快照日期
      - 如果当前日期是最新的快照，返回 null

- **前端需求**：
  - **快照总金额和备注的显示逻辑**：
    - 当选择有快照的日期时：显示快照总金额和快照备注（可编辑）
    - 当选择没有快照的日期时：不显示快照总金额和快照备注，界面更简洁
  - 对账页面顶部添加三个导航按钮（日期选择器左侧）：
    - "上一个快照"按钮（`<`图标）：跳转到上一个已保存的对账快照
    - "下一个快照"按钮（`>`图标）：跳转到下一个已保存的对账快照
    - "跳转到今日"按钮（日历图标）：快速跳转到今天的日期
  - 导航按钮状态：
    - 如果是最早的快照，"上一个"按钮禁用并显示Tooltip"这是最早的对账记录"
    - 如果是最新的快照，"下一个"按钮禁用并显示Tooltip"这是最新的对账记录"
    - 如果当前日期不在快照列表中，自动找到最近的快照进行导航
  - 点击导航按钮后，自动加载对应日期的对账数据

### 3.3 统计报表

- **功能（MVP）**：
  - 按月统计：
    - 本月存款总额（按照当月最后一次记录为准，比如2月20日是2月最后一次记录，那么就按照2月20日的存款作为2月的总额，如果2月没有记录那么就找前面的月份直到找到最近的记录为止）。
    - 本月存款分布饼图
  - 多个月统计：
    - 每个月的存款折线图（最近6个月，最近一年，最近3年，全部）
    - 账户资金堆叠面积图（按账户堆叠的每月资金趋势）
  - 年度统计：
    - 每年的资产变化增值柱状图（默认显示全部年份）
    - 增值计算逻辑：
      - 第一年（快照表里时间最早的记录的那一年）：该年最后一次快照的totalAmount - 该年第一次快照的totalAmount
      - 特殊情况：如果第一年只有一次快照，增值 = 该快照的totalAmount（视为从0开始）
      - 第二年开始：该年最后一次快照的totalAmount - 上一年最后一次快照的totalAmount
      - 特殊情况：如果某一年只有一次快照，且有前一年快照，则：该次快照的totalAmount - 前一年最后一次快照的totalAmount
      - 如果某一年没有任何快照记录，增值显示为0
- **前端需求**：
  - 使用 ECharts / AntV 等图表库（具体库可后定）。
  - 统计页面使用标签页（tab）组织：月度统计、趋势统计、年度统计、到期统计
  - 月度统计：顶部日期选择器 + 指标卡片 + 图表区域
  - 趋势统计：顶部周期选择器 + 视图切换（默认总金额，可切换账户资金堆叠） + 图表区域
  - 年度统计：柱状图区域（默认显示全部年份，不支持时间范围选择）
  - 趋势统计图的 tooltip 中显示快照备注：
    - 当鼠标悬停在趋势图的某个月份数据点上时，tooltip 显示该月的存款总额
    - 如果该月有任何快照有备注，则 tooltip 还会显示该月所有有备注的快照备注（格式：每个备注一行，例如 "2024-01-15: 备注1"）
    - 如果该月没有任何快照有备注，则 tooltip 中不显示备注信息
  - 年度统计图的柱状图：
    - 增值为正数显示绿色，负数为红色
    - tooltip 显示年份和增值金额
    - 柱状图上显示增值数值标签
- **后端接口**：
  - `GET /api/statistics/monthly?month=YYYY-MM`：按月统计
    - 响应：
      ```json
      {
        "month": "2024-02",
        "totalAmount": 150000.00,
        "distribution": [
          { "accountId": 1, "accountName": "招商银行", "amount": 50000.00, "percentage": 33.33 },
          { "accountId": 2, "accountName": "支付宝", "amount": 30000.00, "percentage": 20.00 }
        ]
      }
      ```
    - 说明：totalAmount 取该月最后一次记录（如2月20日是2月最后一次，则用2月20日的数据；如2月无记录，则向前查找最近月份）
  
  - `GET /api/statistics/trend?period=6m|1y|3y|all`：多个月趋势统计
    - 参数 period：6m（最近6个月）、1y（最近一年）、3y（最近3年）、all（全部）
    - 响应：
      ```json
      {
        "period": "1y",
        "data": [
          { 
            "month": "2023-03", 
            "totalAmount": 100000.00,
            "notes": ["2023-03-15: 备注1", "2023-03-20: 备注2"]
          },
          { 
            "month": "2023-04", 
            "totalAmount": 105000.00,
            "notes": []
          }
        ]
      }
      ```
    - 说明：
      - 每个月的 totalAmount 按该月最后一次记录计算（逻辑同按月统计）
      - `notes` 字段：该月所有有备注的快照备注列表，格式为 `["日期: 备注", ...]`
        - 如果该月有任何快照有备注：显示该月所有有备注的快照备注（按日期排序）
        - 如果该月没有任何快照有备注：notes 为空数组（包括该月没有快照，或该月有快照但都没有备注的情况）
        - 前端在趋势图的 tooltip 中显示这些备注

  - `GET /api/statistics/account-trend?period=6m|1y|3y|all`：账户资金堆叠趋势统计
    - 参数 period：6m（最近6个月）、1y（最近一年）、3y（最近3年）、all（全部）
    - 响应：
      ```json
      {
        "period": "1y",
        "months": ["2023-03", "2023-04"],
        "accounts": [
          {
            "accountId": 1,
            "accountName": "招商银行",
            "amounts": [50000.00, 52000.00]
          },
          {
            "accountId": 2,
            "accountName": "支付宝",
            "amounts": [30000.00, 32000.00]
          }
        ]
      }
      ```
    - 说明：
      - `months` 为时间轴月份列表
      - 每个月的金额按该月最后一次记录计算（逻辑同按月统计）
      - 如果某月没有记录，则使用上一个月的金额（沿用趋势统计的空月处理逻辑）
  
  - `GET /api/statistics/yearly`：年度统计
    - 说明：统计每年的资产变化增值，默认显示全部年份
    - 响应：
      ```json
      {
        "data": [
          { 
            "year": "2024", 
            "increase": 50000.00
          },
          { 
            "year": "2023", 
            "increase": 30000.00
          },
          {
            "year": "2022",
            "increase": 0
          }
        ]
      }
      ```
  
  - `GET /api/statistics/maturity`：到期统计
    - 说明：统计定期存款在一年内的到期情况，按照到期时间从近到远排序
    - 响应：
      ```json
      {
        "data": [
          {
            "accountName": "工商银行",
            "depositAmount": 100000.00,
            "depositTime": "2023-12-01",
            "maturityDate": "2024-12-01",
            "remainingDays": 30
          },
          {
            "accountName": "建设银行",
            "depositAmount": 50000.00,
            "depositTime": "2023-10-15",
            "maturityDate": "2025-10-15",
            "remainingDays": 120
          }
        ]
      }
      ```
    - 字段说明：
      - `accountName`：账户名称
      - `depositAmount`：存款金额
      - `depositTime`：存款时间，格式为 YYYY-MM-DD
      - `maturityDate`：到期时间，格式为 YYYY-MM-DD
      - `remainingDays`：剩余天数（到期日期距今天的天数）
    - 业务规则：
      - 只统计定期存款（depositType = "定期"）
      - 只显示一年内（365天内）到期的存款
      - 按到期时间从近到远排序
      - 基于最近一次对账快照的数据

### 3.4 用户认证

- **功能**：
  - 用户注册
  - 用户登录（返回 JWT token）
  - 获取当前用户信息
  - 用户登出
- **后端接口**：
  - `POST /api/auth/register`：用户注册
    - 请求体：{ "username": "user1", "password": "password123", "email": "user@example.com" }
    - 响应：{ "id": 1, "username": "user1", "token": "jwt_token_string" }
  - `POST /api/auth/login`：用户登录
    - 请求体：{ "username": "user1", "password": "password123" }
    - 响应：{ "id": 1, "username": "user1", "token": "jwt_token_string" }
  - `GET /api/auth/me`：获取当前用户信息
    - 需要 Authorization header: `Bearer {token}`
    - 响应：{ "id": 1, "username": "user1", "email": "user@example.com" }
  - `POST /api/auth/logout`：用户登出
    - 需要 Authorization header
    - 响应：{ "message": "登出成功" }

## 4. 非功能需求

- **性能**：
  - 在 1 万条账目以内，列表查询与统计接口响应时间 < 500ms（本地环境）。
- **安全**：
  - 多用户系统，所有接口需要JWT认证
  - 查询时强制过滤`user_id`，防止跨用户数据访问
  - 密码使用BCrypt加密存储
- **可维护性**：
  - 前后端分离：
    - 前端：`/frontend`（Vue + Vite 项目）。
    - 后端：`/backend`（Spring Boot 项目）。
  - 使用 RESTful 风格接口，后续若要加移动端可直接复用。

## 5. 数据模型说明

### 5.1 关键约束
- 存款记录的 `depositTime`（存款时间）与对账日期（`reconciliationDate`）是独立的
- 对账日期必须 >= 存款时间（`reconciliationDate >= depositTime`）
- 复制历史数据时只复制存款记录，不复制账户信息
- 账户可以停用，但已关联的存款记录保留

### 5.2 数据复制逻辑
- 打开对账页面时，如果当前日期没有快照数据，系统自动从最近一次对账日期复制所有存款记录
- 复制的数据仅返回给前端用于编辑，不保存到数据库
- 用户点击保存后，才将数据保存为当前日期的快照

## 6. 不在本次范围（但后续可能做）

- 云端账号与多设备登录。
- 云同步（本地 SQLite <-> 远端数据库）。
- 预算/超支提醒。
- OCR 扫描小票自动识别金额。

## 7. 移动端适配计划

### 7.1 项目现状

**技术栈：**
- Vue 3 + Vite + TypeScript
- Element Plus UI 框架
- 已有 viewport meta 标签

**当前问题：**
1. 固定宽度侧边栏（200px）不适合移动端
2. 表格在移动端显示不佳
3. 对话框宽度固定（500px、400px）
4. 表单布局未针对小屏幕优化
5. 缺少移动端导航（汉堡菜单）

### 7.2 改造难度评估

**总体难度：中等**

- Element Plus 框架本身支持响应式，但需要配置
- 项目结构清晰，代码组织良好
- 主要工作集中在布局重构和样式适配
- 预计工作量：14-20 小时

### 7.3 详细改造计划

#### 阶段一：基础响应式配置（1-2 小时）

**1.1 配置 Element Plus 响应式断点**
- 在 `main.ts` 中配置 Element Plus 的响应式断点
- 设置移动端（< 768px）、平板（768px - 1024px）、桌面（>= 1024px）断点

**1.2 添加全局响应式工具**
- 创建 `composables/useResponsive.ts` 用于检测屏幕尺寸
- 提供 `isMobile`、`isTablet`、`isDesktop` 等响应式变量

**1.3 优化全局样式**
- 在 `App.vue` 中添加移动端基础样式
- 设置触摸友好的按钮尺寸和间距

#### 阶段二：布局改造（3-4 小时）

**2.1 改造 Home.vue 布局**
- **桌面端**：保持现有侧边栏布局
- **移动端**：
  - 侧边栏改为抽屉式（使用 `el-drawer`）
  - 添加汉堡菜单按钮
  - 顶部导航栏优化（用户名和退出按钮适配）

**2.2 响应式断点处理**
- 使用 `el-container` 的响应式属性
- 移动端隐藏侧边栏，显示底部导航栏（可选）

#### 阶段三：页面组件适配（6-8 小时）

**3.1 Login.vue（登录页）**
- 移动端：卡片宽度 100%，减少 padding
- 表单字段垂直排列优化
- 按钮全宽显示

**3.2 Accounts.vue（账户管理）**
- **桌面端**：保持表格显示
- **移动端**：
  - 表格改为卡片列表
  - 操作按钮改为图标按钮或下拉菜单
  - 对话框宽度改为 90% 或全屏

**3.3 Reconciliation.vue（对账管理）**
- **移动端**：
  - 日期选择器和按钮组垂直排列
  - 标签页改为可滚动
  - 表格改为卡片式展示
  - 表单字段优化布局

**3.4 Statistics.vue（统计报表）**
- 图表响应式配置
- 移动端图表尺寸调整
- 数据卡片改为单列布局

#### 阶段四：交互优化（2-3 小时）

**4.1 触摸优化**
- 按钮最小点击区域 44x44px
- 增加触摸反馈
- 优化滚动体验

**4.2 手势支持（可选）**
- 侧边栏支持滑动打开/关闭
- 列表项支持滑动操作（可选）

#### 阶段五：测试与优化（2-3 小时）

**5.1 多设备测试**
- iPhone（Safari）
- Android（Chrome）
- iPad（Safari）
- 不同屏幕尺寸

**5.2 性能优化**
- 移动端懒加载优化
- 减少不必要的重渲染
- 优化图片和资源加载

### 7.4 工作量估算

| 阶段 | 工作量 | 优先级 |
|------|--------|--------|
| 阶段一：基础配置 | 1-2 小时 | 高 |
| 阶段二：布局改造 | 3-4 小时 | 高 |
| 阶段三：页面适配 | 6-8 小时 | 高 |
| 阶段四：交互优化 | 2-3 小时 | 中 |
| 阶段五：测试优化 | 2-3 小时 | 中 |
| **总计** | **14-20 小时** | - |

### 7.5 实施建议

1. **渐进式改造**：先完成阶段一和阶段二（基础布局），确保核心功能在移动端可用
2. **逐个页面适配**：按页面优先级逐个适配，先简单后复杂
3. **测试驱动**：每完成一个页面就在移动端进行测试
4. **保持兼容性**：确保桌面端体验不受影响

### 7.6 技术实现要点

**响应式检测 Composable：**
```typescript
// composables/useResponsive.ts
import { ref, onMounted, onUnmounted } from 'vue'

export function useResponsive() {
  const isMobile = ref(false)
  const isTablet = ref(false)
  const isDesktop = ref(true)

  const checkScreen = () => {
    const width = window.innerWidth
    isMobile.value = width < 768
    isTablet.value = width >= 768 && width < 1024
    isDesktop.value = width >= 1024
  }

  onMounted(() => {
    checkScreen()
    window.addEventListener('resize', checkScreen)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', checkScreen)
  })

  return { isMobile, isTablet, isDesktop }
}
```

**移动端表格转卡片示例：**
```vue
<!-- 桌面端显示表格，移动端显示卡片 -->
<el-table v-if="!isMobile" :data="accounts">
  <!-- 表格列 -->
</el-table>

<div v-else class="mobile-card-list">
  <el-card v-for="item in accounts" :key="item.id" class="mobile-card">
    <!-- 卡片内容 -->
  </el-card>
</div>
```

**响应式对话框：**
```vue
<el-dialog
  v-model="dialogVisible"
  :width="isMobile ? '90%' : '500px'"
  :fullscreen="isMobile"
>
  <!-- 对话框内容 -->
</el-dialog>
```
