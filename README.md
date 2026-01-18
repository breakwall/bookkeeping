# 记账管理系统

## 项目简介

一个基于 Vue 3 + Spring Boot + SQLite 的记账管理系统，支持多用户、账户管理、对账管理和统计报表功能。

## 技术栈

### 前端
- Vue 3 + TypeScript
- Vite
- Vue Router
- Pinia
- Element Plus
- ECharts

### 后端
- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- SQLite
- JWT

## 项目结构

```
.
├── frontend/          # 前端项目
│   ├── src/
│   │   ├── api/      # API接口
│   │   ├── views/    # 页面组件
│   │   ├── router/   # 路由配置
│   │   └── ...
│   └── package.json
├── backend/           # 后端项目
│   ├── src/main/java/com/bookkeeping/
│   │   ├── entity/   # 实体类
│   │   ├── repository/# 数据访问层
│   │   ├── service/  # 业务逻辑层
│   │   ├── controller/# 控制器
│   │   └── config/   # 配置类
│   └── pom.xml
├── docs/              # 文档和脚本
│   ├── archived/     # 已归档的文档（已解决的问题）
│   │   ├── issues/   # 问题文档
│   │   └── migration/# 迁移文档
│   ├── scripts/      # 脚本文件
│   │   ├── migration/# 迁移脚本
│   │   ├── diagnosis/# 诊断脚本
│   │   └── dev/      # 开发脚本
│   └── README.md     # 文档说明
├── REQUIREMENTS.md   # 需求文档
├── DATABASE_DESIGN.md # 数据库设计文档
└── START_GUIDE.md    # 启动说明
```

> **注意**：项目根目录下的文档和脚本已整理到 `docs/` 目录，详见 [docs/README.md](docs/README.md)

## 快速开始

### 前置要求

- Node.js 18+ 和 npm
- Java 17+
- Maven 3.6+

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端将在 http://localhost:3000 启动

### 启动后端

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端将在 http://localhost:8080 启动

数据库文件会自动创建在 `backend/data/bookkeeping.db`

## 功能特性

- ✅ 用户注册和登录（JWT认证）
- ✅ 账户管理（新增、编辑、删除/停用）
- ✅ 对账管理（基于历史数据复制初始化、快照管理、历史对账记录查看）
- ✅ 统计报表
  - 月度统计：本月存款总额和存款分布饼图
  - 趋势统计：多个月存款折线图（支持最近6个月、最近一年、最近3年、全部）
  - 年度统计：年度资产变化增值柱状图

## 开发进度

- [x] 项目初始化和基础配置
- [x] 用户认证功能
- [x] 账户管理功能
- [x] 对账管理功能
- [x] 统计报表功能（月度统计、趋势统计、年度统计）

## 文档

- [需求说明书](./REQUIREMENTS.md)
- [数据库设计](./DATABASE_DESIGN.md)

## License

MIT
