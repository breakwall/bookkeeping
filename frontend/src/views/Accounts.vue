<template>
  <div class="accounts-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>账户管理</span>
          <el-button type="primary" @click="handleAdd">新增账户</el-button>
        </div>
      </template>
      <el-table :data="accounts" style="width: 100%">
        <el-table-column prop="name" label="账户名称" />
        <el-table-column prop="type" label="账户类型" />
        <el-table-column prop="note" label="备注" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button 
              v-if="row.status === 'DISABLED'" 
              size="small" 
              type="success" 
              @click="handleEnable(row)"
            >
              启用
            </el-button>
            <el-button 
              v-if="row.status === 'ACTIVE'" 
              size="small" 
              type="warning" 
              @click="handleDisable(row)"
            >
              禁用
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="账户名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入账户名称" />
        </el-form-item>
        <el-form-item label="账户类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择账户类型" style="width: 100%">
            <el-option label="银行" value="银行" />
            <el-option label="支付宝" value="支付宝" />
            <el-option label="微信" value="微信" />
            <el-option label="理财APP" value="理财APP" />
            <el-option label="股票" value="股票" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="note">
          <el-input
            v-model="form.note"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { accountApi, type Account } from '@/api/account'

const accounts = ref<Account[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增账户')
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  type: '',
  note: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入账户名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择账户类型', trigger: 'change' }]
}

onMounted(() => {
  loadAccounts()
})

const loadAccounts = async () => {
  try {
    accounts.value = await accountApi.getAccounts()
  } catch (error) {
    ElMessage.error('加载账户列表失败')
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增账户'
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: Account) => {
  dialogTitle.value = '编辑账户'
  editingId.value = row.id
  form.name = row.name
  form.type = row.type
  form.note = row.note || ''
  dialogVisible.value = true
}

const handleDelete = async (row: Account) => {
  try {
    const message = row.status === 'DISABLED' 
      ? '确定要删除该账户吗？删除后将无法恢复。' 
      : '确定要删除该账户吗？如果账户有存款记录，将被标记为停用。'
    await ElMessageBox.confirm(message, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await accountApi.deleteAccount(row.id)
    ElMessage.success('删除成功')
    loadAccounts()
  } catch (error) {
    // 用户取消或删除失败
  }
}

const handleEnable = async (row: Account) => {
  try {
    await accountApi.enableAccount(row.id)
    ElMessage.success('启用成功')
    loadAccounts()
  } catch (error) {
    console.error('启用失败:', error)
  }
}

const handleDisable = async (row: Account) => {
  try {
    await ElMessageBox.confirm('确定要禁用该账户吗？禁用后该账户将不会在对账管理中显示。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await accountApi.disableAccount(row.id)
    ElMessage.success('禁用成功')
    loadAccounts()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('禁用失败:', error)
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (editingId.value) {
          await accountApi.updateAccount(editingId.value, form)
        } else {
          await accountApi.createAccount(form)
        }
        ElMessage.success(editingId.value ? '更新成功' : '创建成功')
        dialogVisible.value = false
        loadAccounts()
      } catch (error: any) {
        const errorMessage = error?.response?.data?.message || error?.message || '保存失败'
        ElMessage.error(errorMessage)
      }
    }
  })
}

const resetForm = () => {
  form.name = ''
  form.type = ''
  form.note = ''
  formRef.value?.resetFields()
}
</script>

<style scoped>
.accounts-container {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
