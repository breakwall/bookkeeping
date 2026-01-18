# 项目启动指南

## 前置要求

### 前端环境
- **Node.js**: 18+ 版本
- **npm**: 随 Node.js 一起安装

### 后端环境
- **Java**: JDK 17+
- **Maven**: 3.6+

## 检查环境

### 检查 Node.js 和 npm
```bash
node --version
npm --version
```

如果未安装，请访问：https://nodejs.org/

### 检查 Java 和 Maven
```bash
java -version
mvn -version
```

如果未安装：
- Java: https://www.oracle.com/java/technologies/downloads/#java17
- Maven: https://maven.apache.org/download.cgi

## 启动步骤

### 方式一：一键启动（推荐）

项目提供了便捷的一键启动脚本，可以同时启动前端和后端：

**Windows PowerShell：**
```powershell
.\start-dev.ps1
```

**Windows 批处理：**
```cmd
start-dev.bat
```

**Linux/Mac/Git Bash：**
```bash
chmod +x start-dev.sh
./start-dev.sh
```

**停止服务（仅 Shell 脚本）：**
```bash
./stop-dev.sh
```

脚本会自动：
- ✅ 检查环境（Node.js、Java、Maven）
- ✅ 安装前端依赖（如果未安装）
- ✅ 在新窗口中启动后端服务（端口 8080）
- ✅ 在新窗口中启动前端服务（端口 3000）

### 方式二：手动启动

#### 1. 启动前端（端口：3000）

```bash
# 进入前端目录
cd frontend

# 安装依赖（首次运行需要）
npm install

# 启动开发服务器
npm run dev
```

启动成功后，访问：http://localhost:3000

**注意**：前端会显示登录页面，需要先注册账号才能登录使用。

#### 2. 启动后端（端口：8080）

```bash
# 进入后端目录
cd backend

# 编译项目（首次运行需要）
mvn clean install

# 启动Spring Boot应用
mvn spring-boot:run
```

或者使用IDE（如IntelliJ IDEA）直接运行 `BookkeepingApplication.java`

启动成功后，后端API地址：http://localhost:8080

**注意**：
- 数据库文件会自动创建在 `backend/data/bookkeeping.db`
- 首次启动会自动创建数据表（通过JPA的ddl-auto: update）

## 验证运行状态

### 前端验证
1. 打开浏览器访问 http://localhost:3000
2. 应该能看到登录页面（即使功能未实现）

### 后端验证
1. 访问 http://localhost:8080/api/health（应该返回健康检查状态）
2. 检查控制台是否有启动成功的日志（包含 "Started BookkeepingApplication"）
3. 检查 `backend/data/` 目录下是否生成了 `bookkeeping.db` 文件
4. 检查 `backend/logs/` 目录下是否生成了 `bookkeeping-backend.log` 日志文件

## 当前状态

### ✅ 已完成
- 项目结构搭建
- 前端页面框架（登录、首页、账户管理、对账管理、统计报表）
- 后端基础配置（Spring Boot、SQLite、JPA）
- 数据库实体类（User、Account、Deposit、ReconciliationSnapshot）
- 用户认证功能（注册、登录、JWT认证）
- 账户管理功能（新增、编辑、删除/停用）
- 对账管理功能（快照创建、历史记录查看、存款记录增删改）
- 统计报表功能
  - 月度统计：本月存款总额和存款分布饼图
  - 趋势统计：多个月存款折线图（支持最近6个月、最近一年、最近3年、全部）
  - 年度统计：年度资产变化增值柱状图

## 常见问题

### 1. npm install 失败
- 检查网络连接
- 尝试使用国内镜像：`npm config set registry https://registry.npmmirror.com`
- 清除缓存：`npm cache clean --force`

### 2. Maven 下载依赖慢
- 配置国内镜像（编辑 `~/.m2/settings.xml`）
- 或使用阿里云镜像

### 3. 端口被占用
- 前端：修改 `frontend/vite.config.ts` 中的 `server.port`
- 后端：修改 `backend/src/main/resources/application.yml` 中的 `server.port`

### 4. SQLite数据库文件未创建
- 确保 `backend/data/` 目录存在（如果不存在会自动创建）
- 检查应用启动日志是否有错误

### 5. 前端页面空白或报错
- 打开浏览器开发者工具（F12）查看控制台错误
- 检查后端是否正常启动
- 确认前端代理配置正确（`vite.config.ts` 中的 proxy 配置）

## 使用指南

### 首次使用
1. **注册账号**：访问 http://localhost:3000，点击注册，创建新账号
2. **登录系统**：使用注册的账号密码登录
3. **添加账户**：在"账户管理"页面添加银行账户、支付宝、微信等账户
4. **创建对账快照**：在"对账管理"页面选择日期，点击"新建对账"，复制历史数据或创建空快照
5. **录入存款记录**：在对账快照中为每个账户添加存款记录
6. **查看统计报表**：在"统计报表"页面查看月度统计、趋势统计和年度统计

### 统计报表说明
- **月度统计**：选择月份，查看该月的存款总额和账户分布饼图
- **趋势统计**：选择时间范围（最近6个月、最近一年、最近3年、全部），查看存款趋势折线图
- **年度统计**：查看每年的资产变化增值柱状图（自动显示全部年份）

## 开发建议

1. **先启动后端**，确保数据库和基础服务正常
2. **再启动前端**，查看页面效果
3. **逐个实现功能**，每完成一个模块就测试一下
4. **使用浏览器开发者工具**调试前端问题
5. **查看后端控制台日志**调试后端问题
