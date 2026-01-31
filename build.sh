#!/bin/bash

# 构建脚本 - 一次性构建前端和后端
# 项目：记账管理系统
# 功能：按照流程构建前端和后端代码

set -e  # 任何命令失败都立即退出

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 脚本所在的根目录
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
BACKEND_DIR="$ROOT_DIR/backend"

# 获取开始时间
START_TIME=$(date +%s)

# 函数：打印步骤信息
print_step() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

# 函数：打印成功信息
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# 函数：打印错误信息
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# 函数：打印警告信息
print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# 清理函数 - 异常退出时执行
cleanup() {
    if [ $? -ne 0 ]; then
        print_error "构建过程中出现错误！"
        END_TIME=$(date +%s)
        DURATION=$((END_TIME - START_TIME))
        echo "耗时: ${DURATION}秒"
        exit 1
    fi
}

trap cleanup EXIT

# 验证前置条件
print_step "第一步：验证构建环境"

# 检查 Node.js
if ! command -v node &> /dev/null; then
    print_error "Node.js 未安装，请先安装 Node.js"
    exit 1
fi
NODE_VERSION=$(node -v)
print_success "Node.js 已安装: $NODE_VERSION"

# 检查 npm
if ! command -v npm &> /dev/null; then
    print_error "npm 未安装"
    exit 1
fi
NPM_VERSION=$(npm -v)
print_success "npm 已安装: $NPM_VERSION"

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    print_error "Maven 未安装，请先安装 Maven"
    exit 1
fi
MVN_VERSION=$(mvn -v | head -n 1)
print_success "Maven 已安装: $MVN_VERSION"

# 检查 Java
if ! command -v java &> /dev/null; then
    print_error "Java 未安装"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -n 1)
print_success "Java 已安装: $JAVA_VERSION"

# 检查目录
if [ ! -d "$FRONTEND_DIR" ]; then
    print_error "前端目录不存在: $FRONTEND_DIR"
    exit 1
fi
print_success "前端目录存在: $FRONTEND_DIR"

if [ ! -d "$BACKEND_DIR" ]; then
    print_error "后端目录不存在: $BACKEND_DIR"
    exit 1
fi
print_success "后端目录存在: $BACKEND_DIR"

# 构建前端
print_step "第二步：构建前端代码"
cd "$FRONTEND_DIR"

if [ ! -f "package.json" ]; then
    print_error "package.json 不存在"
    exit 1
fi

print_warning "安装前端依赖..."
npm install

print_warning "编译和构建前端..."
npm run build

if [ $? -eq 0 ]; then
    print_success "前端构建完成"
else
    print_error "前端构建失败"
    exit 1
fi

# 构建后端
print_step "第三步：构建后端代码"
cd "$BACKEND_DIR"

if [ ! -f "pom.xml" ]; then
    print_error "pom.xml 不存在"
    exit 1
fi

print_warning "清理并构建后端..."
mvn clean package

if [ $? -eq 0 ]; then
    print_success "后端构建完成"
    
    # 检查生成的 JAR 文件
    if [ -f "target/bookkeeping-backend-1.0.0.jar" ]; then
        print_success "JAR 文件已生成: target/bookkeeping-backend-1.0.0.jar"
    else
        print_warning "JAR 文件未找到（可能未配置生成）"
    fi
else
    print_error "后端构建失败"
    exit 1
fi

# 总结
print_step "构建完成总结"
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

print_success "所有构建任务已完成"
print_success "前端输出目录: $FRONTEND_DIR/dist"
print_success "后端输出目录: $BACKEND_DIR/target"
echo -e "${GREEN}总耗时: ${DURATION}秒${NC}"

# 返回根目录
cd "$ROOT_DIR"
