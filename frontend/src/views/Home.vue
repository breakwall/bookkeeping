<template>
  <el-container class="home-container">
    <el-header class="header">
      <div class="header-left">
        <h2>记账管理系统</h2>
      </div>
      <div class="header-right">
        <span>欢迎，{{ userInfo?.username }}</span>
        <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="aside">
        <el-menu
          :default-active="activeMenu"
          router
          class="menu"
        >
          <el-menu-item index="/home/accounts">
            <el-icon><Wallet /></el-icon>
            <span>账户管理</span>
          </el-menu-item>
          <el-menu-item index="/home/reconciliation">
            <el-icon><Document /></el-icon>
            <span>对账管理</span>
          </el-menu-item>
          <el-menu-item index="/home/statistics">
            <el-icon><DataAnalysis /></el-icon>
            <span>统计报表</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Wallet, Document, DataAnalysis } from '@element-plus/icons-vue'
import { authApi } from '@/api/auth'
import type { UserInfo } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userInfo = ref<UserInfo | null>(null)

const activeMenu = computed(() => route.path)

onMounted(async () => {
  try {
    userInfo.value = await authApi.getCurrentUser()
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
})

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await authApi.logout()
    localStorage.removeItem('token')
    router.push('/login')
  } catch (error) {
    // 用户取消
  }
}
</script>

<style scoped>
.home-container {
  height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #409eff;
  color: white;
  padding: 0 20px;
}

.header-left h2 {
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.aside {
  background-color: #f5f5f5;
}

.menu {
  border-right: none;
}

.main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
