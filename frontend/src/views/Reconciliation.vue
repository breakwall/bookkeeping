<template>
  <div class="reconciliation-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>对账管理</span>
          <div class="header-actions">
            <div class="nav-buttons">
              <el-tooltip content="上一个快照" placement="top" :disabled="hasPreviousSnapshot">
                <el-button 
                  :icon="ArrowLeft"
                  size="small"
                  :disabled="!hasPreviousSnapshot"
                  @click="handlePreviousSnapshot"
                />
              </el-tooltip>
              <el-tooltip content="下一个快照" placement="top" :disabled="hasNextSnapshot">
                <el-button 
                  :icon="ArrowRight"
                  size="small"
                  :disabled="!hasNextSnapshot"
                  @click="handleNextSnapshot"
                />
              </el-tooltip>
              <el-tooltip content="跳转到今日" placement="top">
                <el-button 
                  :icon="Calendar"
                  size="small"
                  @click="handleGoToToday"
                />
              </el-tooltip>
            </div>
            <el-date-picker
              v-model="selectedDate"
              type="date"
              placeholder="选择对账日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="loadReconciliationData"
              style="margin-left: 10px"
            />
            <el-button 
              type="primary" 
              @click="handleCreateNew" 
              :loading="creating"
              :disabled="hasSnapshotForSelectedDate"
              style="margin-left: 10px"
            >
              新建对账
            </el-button>
          </div>
        </div>
      </template>
      <!-- 快照总金额和备注（仅在有快照时显示） -->
      <div v-if="hasSnapshot" style="margin-bottom: 15px;">
        <el-form label-width="100px">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="快照总金额">
                <div style="font-size: 20px; font-weight: bold; color: #409eff;">
                  ¥{{ totalAmount.toLocaleString() }}
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="16" style="display: flex; justify-content: flex-end;">
              <el-form-item label="快照备注" class="note-form-item">
                <el-input
                  v-model="snapshotNote"
                  type="textarea"
                  :rows="1"
                  placeholder="请输入快照备注（可选）"
                  style="width: 600px;"
                  @blur="handleNoteBlur"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>
      
      <el-tabs v-model="activeAccountId" v-if="reconciliationData.accounts.length > 0">
        <el-tab-pane
          v-for="account in reconciliationData.accounts"
          :key="account.accountId"
          :label="account.accountName"
          :name="account.accountId.toString()"
        >
          <div class="account-summary">
            <div class="account-header">
              <div class="account-total">
                <span style="font-size: 14px; color: #909399; margin-right: 8px;">账户总金额：</span>
                <span style="font-size: 18px; font-weight: bold; color: #409eff;">
                  ¥{{ getAccountTotal(account).toLocaleString() }}
                </span>
              </div>
              <div class="account-actions">
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="handleAddDeposit(account.accountId)"
                >
                  新增存款记录
                </el-button>
              </div>
            </div>
          </div>
          <el-table :data="account.deposits" style="width: 100%">
            <el-table-column prop="depositType" label="存款类型" />
            <el-table-column prop="depositTime" label="存款时间" />
            <el-table-column prop="amount" label="金额">
              <template #default="{ row }">
                ¥{{ row.amount.toLocaleString() }}
              </template>
            </el-table-column>
            <el-table-column prop="interestRate" label="利率">
              <template #default="{ row }">
                {{ row.interestRate !== undefined && row.interestRate !== null ? (row.interestRate * 100).toFixed(2) + '%' : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="term" label="存期">
              <template #default="{ row }">
                {{ row.term ? row.term + '年' : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="note" label="备注" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button 
                  size="small" 
                  @click="handleEditDeposit(row)"
                >
                  编辑
                </el-button>
                <el-button 
                  size="small" 
                  type="danger" 
                  @click="handleDeleteDeposit(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <el-empty v-else description="暂无对账数据，请先创建账户" />
    </el-card>

    <!-- 新增/编辑存款记录对话框 -->
    <el-dialog
      v-model="depositDialogVisible"
      :title="depositDialogTitle"
      width="500px"
      @close="resetDepositForm"
    >
      <el-form :model="depositForm" :rules="depositRules" ref="depositFormRef" label-width="100px">
        <el-form-item label="存款类型" prop="depositType">
          <el-select v-model="depositForm.depositType" placeholder="请选择存款类型" style="width: 100%" @change="handleDepositTypeChange">
            <el-option label="活期" value="活期" />
            <el-option label="定期" value="定期" />
            <el-option label="理财" value="理财" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="存款时间" prop="depositTime">
          <el-date-picker
            v-model="depositForm.depositTime"
            type="date"
            placeholder="选择存款时间"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number
            v-model="depositForm.amount"
            :precision="2"
            :min="0"
            style="width: 100%"
            placeholder="请输入金额"
          />
        </el-form-item>
        <el-form-item label="利率" prop="interestRate">
          <el-input-number
            v-model="depositForm.interestRate"
            :precision="2"
            :min="0"
            :max="100"
            :disabled="depositForm.depositType === '活期'"
            style="width: 100%"
            placeholder="请输入利率（百分比）"
          />
        </el-form-item>
        <el-form-item label="存期" prop="term">
          <el-input-number
            v-model="depositForm.term"
            :precision="0"
            :min="1"
            :max="10"
            :disabled="depositForm.depositType !== '定期'"
            style="width: 100%"
            placeholder="请输入存期（年）"
          />
          <span v-if="depositForm.depositType === '定期'" style="margin-left: 10px; color: #909399;">年（1-10年）</span>
        </el-form-item>
        <el-form-item label="备注" prop="note">
          <el-input
            v-model="depositForm.note"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="depositDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitDeposit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowRight, Calendar } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { reconciliationApi, type ReconciliationData, type AccountDepositData } from '@/api/reconciliation'
import { depositApi, type Deposit, type CreateDepositRequest, type UpdateDepositRequest } from '@/api/deposit'

// 使用从API导入的类型

const selectedDate = ref<string>('')
const reconciliationData = ref<ReconciliationData>({
  date: '',
  totalAmount: 0,
  accounts: []
})
const snapshotNote = ref<string>('') // 快照备注（用于编辑）
const activeAccountId = ref<string>('')
const creating = ref(false)
const depositDialogVisible = ref(false)
const depositDialogTitle = ref('新增存款记录')
const depositFormRef = ref<FormInstance>()
const editingDepositId = ref<number | null>(null)
const currentAccountId = ref<number | null>(null)
const hasPreviousSnapshot = ref(false)
const hasNextSnapshot = ref(false)
const checkingNavigation = ref(false)
const loading = ref(false)

const depositForm = reactive<Deposit>({
  depositType: '',
  depositTime: '',
  amount: 0,
  interestRate: undefined,
  term: undefined,
  note: ''
})

const depositRules: FormRules = {
  depositType: [{ required: true, message: '请输入存款类型', trigger: 'blur' }],
  depositTime: [{ required: true, message: '请选择存款时间', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  term: [
    {
      validator: (rule: any, value: any, callback: any) => {
        if (depositForm.depositType === '定期') {
          if (!value) {
            callback(new Error('定期存款必须填写存期'))
          } else if (value < 1 || value > 10) {
            callback(new Error('存期必须在1-10年之间'))
          } else {
            callback()
          }
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

onMounted(async () => {
  // 初始化时先获取最近一次快照日期
  try {
    const latestDate = await reconciliationApi.getLatestReconciliationDate()
    if (latestDate) {
      selectedDate.value = latestDate
    } else {
      // 如果从未对账，使用今天的日期
      selectedDate.value = new Date().toISOString().split('T')[0]
    }
    await loadReconciliationData()
  } catch (error: any) {
    const errorMessage = error?.response?.data?.message || error?.message || '加载对账数据失败'
    ElMessage.error(errorMessage)
    // 如果获取失败，使用今天的日期
    selectedDate.value = new Date().toISOString().split('T')[0]
    await loadReconciliationData()
  }
})

// 监听日期变化，检查导航按钮状态
watch(selectedDate, () => {
  checkNavigationButtons()
}, { immediate: false })

const loadReconciliationData = async () => {
  if (!selectedDate.value) {
    return
  }
  loading.value = true
  try {
    const data = await reconciliationApi.getReconciliation(selectedDate.value)
    reconciliationData.value = data
    // 更新选中的日期为实际返回的日期（确保日期选择器显示正确）
    if (data.date) {
      selectedDate.value = data.date
    }
    // 设置备注
    snapshotNote.value = data.note || ''
    if (data.accounts.length > 0) {
      // 检查当前选中的账户是否仍存在于新数据中
      const currentActiveId = activeAccountId.value
      const accountExists = data.accounts.some(a => a.accountId.toString() === currentActiveId)
      // 只有当没有选中账户或选中的账户已不存在时，才切换到第一个账户
      if (!currentActiveId || !accountExists) {
        activeAccountId.value = data.accounts[0].accountId.toString()
      }
    }
    
    // 检查导航按钮状态
    await checkNavigationButtons()
  } catch (error: any) {
    const errorMessage = error?.response?.data?.message || error?.message || '加载对账数据失败'
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}

// 检查导航按钮状态
const checkNavigationButtons = async () => {
  if (checkingNavigation.value) return
  checkingNavigation.value = true
  
  try {
    const [previousDate, nextDate] = await Promise.all([
      reconciliationApi.getPreviousSnapshotDate(selectedDate.value),
      reconciliationApi.getNextSnapshotDate(selectedDate.value)
    ])
    
    hasPreviousSnapshot.value = previousDate !== null && previousDate !== undefined && previousDate !== ''
    hasNextSnapshot.value = nextDate !== null && nextDate !== undefined && nextDate !== ''
  } catch (error: any) {
    console.error('检查导航按钮状态失败:', error)
    hasPreviousSnapshot.value = false
    hasNextSnapshot.value = false
  } finally {
    checkingNavigation.value = false
  }
}

// 跳转到上一个快照
const handlePreviousSnapshot = async () => {
  try {
    const previousDate = await reconciliationApi.getPreviousSnapshotDate(selectedDate.value)
    if (previousDate) {
      selectedDate.value = previousDate
      await loadReconciliationData()
    } else {
      ElMessage.info('这是最早的对账记录')
      hasPreviousSnapshot.value = false
    }
  } catch (error: any) {
    const errorMessage = error?.response?.data?.message || error?.message || '获取上一个快照失败'
    ElMessage.error(errorMessage)
  }
}

// 跳转到下一个快照
const handleNextSnapshot = async () => {
  try {
    const nextDate = await reconciliationApi.getNextSnapshotDate(selectedDate.value)
    if (nextDate) {
      selectedDate.value = nextDate
      await loadReconciliationData()
    } else {
      ElMessage.info('这是最新的对账记录')
      hasNextSnapshot.value = false
    }
  } catch (error: any) {
    const errorMessage = error?.response?.data?.message || error?.message || '获取下一个快照失败'
    ElMessage.error(errorMessage)
  }
}

// 跳转到今日
const handleGoToToday = () => {
  const today = new Date().toISOString().split('T')[0]
  selectedDate.value = today
  loadReconciliationData()
}

// 判断当前日期是否有快照（用于控制快照总金额和备注的显示）
const hasSnapshot = computed(() => {
  // 如果总金额大于0，说明有快照
  if (reconciliationData.value.totalAmount > 0) {
    return true
  }
  // 或者有任何存款记录，也说明有快照
  const hasDeposits = reconciliationData.value.accounts.some(
    account => account.deposits && account.deposits.length > 0
  )
  return hasDeposits
})

// 判断选中的日期是否已有快照（用于控制"新建对账"按钮的显示）
const hasSnapshotForSelectedDate = computed(() => {
  if (!selectedDate.value) {
    return true // 没有选择日期，禁用按钮
  }
  // 如果选中的日期已有快照（总金额大于0 或 有任何存款记录），禁用按钮
  // 注意：即使没有快照，accounts 也可能不为空（显示启用的账户），但 deposits 应该为空
  const hasDeposits = reconciliationData.value.accounts.some(
    account => account.deposits && account.deposits.length > 0
  )
  return hasDeposits || reconciliationData.value.totalAmount > 0
})

// 新建对账
const handleCreateNew = async () => {
  if (!selectedDate.value) {
    ElMessage.warning('请先选择日期')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要在 ${selectedDate.value} 创建新对账快照吗？系统将复制该日期之前最近一次的快照数据。`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return // 用户取消
  }
  
  creating.value = true
  try {
    await reconciliationApi.createNewReconciliation(selectedDate.value)
    ElMessage.success('新建对账成功')
    // 重新加载数据（使用选中的日期）
    await loadReconciliationData()
  } catch (error: any) {
    const errorMessage = error?.response?.data?.message || error?.message || '新建对账失败'
    ElMessage.error(errorMessage)
  } finally {
    creating.value = false
  }
}

// 快照备注失去焦点时保存
const handleNoteBlur = async () => {
  try {
    // 使用选中的日期
    await reconciliationApi.updateSnapshotNote(selectedDate.value, snapshotNote.value || '')
    // 不显示成功消息，避免频繁提示
  } catch (error: any) {
    const errorMessage = error?.response?.data?.message || error?.message || '更新备注失败'
    ElMessage.error(errorMessage)
  }
}

const handleAddDeposit = (accountId: number) => {
  depositDialogTitle.value = '新增存款记录'
  editingDepositId.value = null
  currentAccountId.value = accountId
  resetDepositForm()
  depositDialogVisible.value = true
}

const handleEditDeposit = (row: Deposit) => {
  depositDialogTitle.value = '编辑存款记录'
  editingDepositId.value = row.id || null
  // 找到该存款记录所属的账户
  const account = reconciliationData.value.accounts.find(a => 
    a.deposits.some(d => d.id === row.id || d === row)
  )
  if (account) {
    currentAccountId.value = account.accountId
  }
  depositForm.depositType = row.depositType
  depositForm.depositTime = row.depositTime
  depositForm.amount = row.amount
  // 确保活期存款的利率为0，否则将数据库中的小数形式转换为百分比（乘以100）
  depositForm.interestRate = row.depositType === '活期' ? 0 : (row.interestRate ? row.interestRate * 100 : undefined)
  depositForm.term = row.term
  depositForm.note = row.note || ''
  depositDialogVisible.value = true
}

const handleDeleteDeposit = async (deposit: Deposit) => {
  if (!deposit.id) {
    ElMessage.error('该记录尚未保存，无法删除')
    return
  }

  try {
    await ElMessageBox.confirm('确定要删除该存款记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 调用API删除
    await depositApi.deleteDeposit(deposit.id)
    ElMessage.success('删除成功')
    
    // 重新加载数据
    await loadReconciliationData()
  } catch (error: any) {
    if (error !== 'cancel') {
      const errorMessage = error?.response?.data?.message || error?.message || '删除失败'
      ElMessage.error(errorMessage)
    }
  }
}

const handleSubmitDeposit = async () => {
  if (!depositFormRef.value) return
  await depositFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // 使用选中的日期作为对账日期
        const reconciliationDate = selectedDate.value
        
        // 确保活期存款的利率为0，否则将用户输入的百分比转换为小数形式（除以100）保存到数据库
        const finalInterestRate = depositForm.depositType === '活期' 
          ? 0 
          : (depositForm.interestRate !== undefined && depositForm.interestRate !== null 
              ? depositForm.interestRate / 100 
              : undefined)

        if (editingDepositId.value) {
          // 更新现有记录
          const updateData: UpdateDepositRequest = {
            depositType: depositForm.depositType,
            depositTime: depositForm.depositTime,
            amount: depositForm.amount,
            interestRate: finalInterestRate,
            term: depositForm.term,
            note: depositForm.note
          }
          await depositApi.updateDeposit(editingDepositId.value, updateData)
          ElMessage.success('更新成功')
        } else {
          // 创建新记录（使用选中的日期）
          const createData: CreateDepositRequest = {
            accountId: currentAccountId.value!,
            depositType: depositForm.depositType,
            depositTime: depositForm.depositTime,
            amount: depositForm.amount,
            interestRate: finalInterestRate,
            term: depositForm.term,
            note: depositForm.note,
            reconciliationDate: reconciliationDate
          }
          await depositApi.createDeposit(createData)
          ElMessage.success('添加成功')
        }

        depositDialogVisible.value = false
        // 重新加载数据
        await loadReconciliationData()
      } catch (error: any) {
        const errorMessage = error?.response?.data?.message || error?.message || '操作失败'
        ElMessage.error(errorMessage)
      }
    }
  })
}

const resetDepositForm = () => {
  depositForm.depositType = ''
  depositForm.depositTime = ''
  depositForm.amount = 0
  depositForm.interestRate = undefined
  depositForm.term = undefined
  depositForm.note = ''
  depositFormRef.value?.resetFields()
}

// 处理存款类型变化
const handleDepositTypeChange = (value: string) => {
  if (value === '活期') {
    // 活期时，利率自动设为0%
    depositForm.interestRate = 0
    // 存期清空
    depositForm.term = undefined
  } else if (value !== '定期') {
    // 理财或其他时，存期清空
    depositForm.term = undefined
  }
  // 验证表单，触发存期字段的验证
  depositFormRef.value?.validateField('term')
}

// 计算账户总金额
const getAccountTotal = (account: AccountDepositData): number => {
  return account.deposits.reduce((sum, deposit) => {
    return sum + (typeof deposit.amount === 'number' ? deposit.amount : parseFloat(String(deposit.amount)) || 0)
  }, 0)
}

// 计算快照总金额（优先使用后端返回的值，如果没有则实时计算所有账户的总金额）
const totalAmount = computed(() => {
  if (reconciliationData.value.totalAmount > 0) {
    // 使用后端返回的总金额
    return reconciliationData.value.totalAmount
  } else {
    // 如果没有总金额，实时计算所有账户的总金额
    return reconciliationData.value.accounts.reduce((sum, account) => {
      return sum + getAccountTotal(account)
    }, 0)
  }
})
</script>

<style scoped>
.reconciliation-container {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
}

.nav-buttons {
  display: flex;
  gap: 5px;
}
</style>

<style>
.account-summary {
  margin-bottom: 20px;
}

.account-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.account-total {
  display: flex;
  align-items: center;
}

.account-actions {
  display: flex;
  align-items: center;
}

</style>
