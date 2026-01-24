<template>
  <div class="statistics-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>统计报表</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 月度统计标签页 -->
        <el-tab-pane label="月度统计" name="monthly">
          <template #label>
            <span>月度统计</span>
          </template>
          <div class="monthly-stats">
            <div class="stats-header">
              <el-date-picker
                v-model="selectedMonth"
                type="month"
                placeholder="选择月份"
                format="YYYY-MM"
                value-format="YYYY-MM"
                @change="loadMonthlyStatistics"
              />
            </div>
            <el-row :gutter="20" style="margin-top: 20px">
              <el-col :span="8">
                <el-statistic title="存款总额" :value="monthlyStats.totalAmount">
                  <template #prefix>¥</template>
                </el-statistic>
              </el-col>
            </el-row>
            <div class="chart-container">
              <h4>存款分布</h4>
              <div ref="distributionChartRef" style="width: 100%; height: 400px"></div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 趋势统计标签页 -->
        <el-tab-pane label="趋势统计" name="trend">
          <template #label>
            <span>趋势统计</span>
          </template>
          <div class="trend-stats">
            <div class="stats-header">
              <el-select
                v-model="trendPeriod"
                placeholder="选择趋势周期"
                style="width: 150px"
                @change="loadTrendStatistics"
              >
                <el-option label="最近6个月" value="6m" />
                <el-option label="最近一年" value="1y" />
                <el-option label="最近3年" value="3y" />
                <el-option label="全部" value="all" />
              </el-select>
            </div>
            <div class="chart-container" style="margin-top: 20px">
              <div ref="trendChartRef" style="width: 100%; height: 400px"></div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 年度统计标签页 -->
        <el-tab-pane label="年度统计" name="yearly">
          <template #label>
            <span>年度统计</span>
          </template>
          <div class="yearly-stats">
            <div class="chart-container" style="margin-top: 20px">
              <h4>年度资产变化增值</h4>
              <div ref="yearlyChartRef" style="width: 100%; height: 400px"></div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 到期统计标签页 -->
        <el-tab-pane label="到期统计" name="maturity">
          <template #label>
            <span>到期统计</span>
          </template>
          <div class="maturity-stats">
            <div class="stats-header">
              <h4>定期存款到期时间统计（一年内）</h4>
            </div>
            <el-table 
              :data="maturityStats.data" 
              style="width: 100%; margin-top: 20px"
              v-loading="maturityLoading"
            >
              <el-table-column prop="accountName" label="账户名称" width="150" />
              <el-table-column prop="depositAmount" label="存款金额" width="120">
                <template #default="scope">
                  ¥{{ scope.row.depositAmount.toLocaleString() }}
                </template>
              </el-table-column>
              <el-table-column prop="depositTime" label="存款时间" width="120" />
              <el-table-column prop="maturityDate" label="到期时间" width="120" />
              <el-table-column prop="remainingDays" label="剩余天数" width="100">
                <template #default="scope">
                  <el-tag 
                    :type="scope.row.remainingDays <= 30 ? 'danger' : (scope.row.remainingDays <= 90 ? 'warning' : 'success')"
                  >
                    {{ scope.row.remainingDays }}天
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { statisticsApi, type MonthlyStatistics, type TrendStatistics, type YearlyStatistics, type MaturityStatistics } from '@/api/statistics'

const activeTab = ref<string>('monthly') // 默认显示月度统计
const selectedMonth = ref<string>(
  new Date().toISOString().slice(0, 7)
)
const trendPeriod = ref<string>('all')
const distributionChartRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()
const yearlyChartRef = ref<HTMLDivElement>()
let distributionChart: ECharts | null = null
let trendChart: ECharts | null = null
let yearlyChart: ECharts | null = null

const monthlyStats = reactive<{
  totalAmount: number
  distribution: MonthlyStatistics['distribution']
}>({
  totalAmount: 0,
  distribution: []
})

const trendData = reactive<{
  period: string
  data: TrendStatistics['data']
}>({
  period: '',
  data: []
})

const yearlyData = reactive<{
  data: YearlyStatistics['data']
}>({
  data: []
})

// 到期统计数据
const maturityStats = reactive<{
  data: MaturityStatistics['data']
}>({
  data: []
})

const maturityLoading = ref<boolean>(false)

// 处理标签页切换
const handleTabChange = async (tabName: string) => {
  // 等待DOM更新
  await nextTick()
  // 额外等待一小段时间确保标签页内容完全渲染
  await new Promise(resolve => setTimeout(resolve, 50))
  
  if (tabName === 'monthly') {
    // 切换到月度统计标签页
    if (!distributionChart && distributionChartRef.value) {
      distributionChart = echarts.init(distributionChartRef.value)
    }
    if (!monthlyStats.distribution.length) {
      // 如果还没有加载过数据，则加载
      await loadMonthlyStatistics()
    } else {
      // 如果已加载过数据，只需要更新图表
      updateDistributionChart()
    }
    // 调整月度统计图表大小
    if (distributionChart) {
      setTimeout(() => {
        distributionChart?.resize()
      }, 100)
    }
  } else if (tabName === 'trend') {
    // 切换到趋势统计标签页
    if (!trendChart && trendChartRef.value) {
      trendChart = echarts.init(trendChartRef.value)
    }
    if (!trendData.data.length) {
      // 如果还没有加载过数据，则加载
      await loadTrendStatistics()
    } else {
      // 如果已加载过数据，只需要更新图表
      updateTrendChart()
    }
    // 调整趋势统计图表大小
    if (trendChart) {
      setTimeout(() => {
        trendChart?.resize()
      }, 100)
    }
  } else if (tabName === 'yearly') {
    // 切换到年度统计标签页
    if (!yearlyChart && yearlyChartRef.value) {
      yearlyChart = echarts.init(yearlyChartRef.value)
    }
    if (!yearlyData.data.length) {
      // 如果还没有加载过数据，则加载
      await loadYearlyStatistics()
    } else {
      // 如果已加载过数据，只需要更新图表
      updateYearlyChart()
    }
    // 调整年度统计图表大小
    if (yearlyChart) {
      setTimeout(() => {
        yearlyChart?.resize()
      }, 100)
    }
  } else if (tabName === 'maturity') {
    // 切换到到期统计标签页
    if (!maturityStats.data.length) {
      // 如果还没有加载过数据，则加载
      await loadMaturityStatistics()
    }
  }
}

onMounted(async () => {
  await nextTick()
  // 默认加载月度统计数据
  if (activeTab.value === 'monthly') {
    if (distributionChartRef.value) {
      distributionChart = echarts.init(distributionChartRef.value)
    }
    await loadMonthlyStatistics()
  }
  
  // 监听窗口大小变化，自动调整当前可见的图表
  window.addEventListener('resize', () => {
    if (activeTab.value === 'monthly' && distributionChart) {
      distributionChart.resize()
    } else if (activeTab.value === 'trend' && trendChart) {
      trendChart.resize()
    } else if (activeTab.value === 'yearly' && yearlyChart) {
      yearlyChart.resize()
    }
  })
})

const loadMonthlyStatistics = async () => {
  try {
    const data = await statisticsApi.getMonthlyStatistics(selectedMonth.value)
    monthlyStats.totalAmount = data.totalAmount
    monthlyStats.distribution = data.distribution
    await nextTick() // 等待DOM更新
    updateDistributionChart()
  } catch (error: any) {
    const errorMessage = error?.response?.data?.message || error?.message || '加载月度统计失败'
    ElMessage.error(errorMessage)
    console.error('加载月度统计失败:', error)
  }
}

const loadTrendStatistics = async () => {
  try {
    const data = await statisticsApi.getTrendStatistics(trendPeriod.value)
    trendData.period = data.period
    trendData.data = data.data
    await nextTick() // 等待DOM更新
    updateTrendChart()
  } catch (error: any) {
    const errorMessage = error?.response?.data?.message || error?.message || '加载趋势统计失败'
    ElMessage.error(errorMessage)
    console.error('加载趋势统计失败:', error)
  }
}

const loadYearlyStatistics = async () => {
  try {
    const data = await statisticsApi.getYearlyStatistics()
    yearlyData.data = data.data
    await nextTick() // 等待DOM更新
    updateYearlyChart()
  } catch (error: any) {
    const errorMessage = error?.response?.data?.message || error?.message || '加载年度统计失败'
    ElMessage.error(errorMessage)
    console.error('加载年度统计失败:', error)
  }
}

const loadMaturityStatistics = async () => {
  maturityLoading.value = true
  try {
    const data = await statisticsApi.getMaturityStatistics()
    maturityStats.data = data.data
  } catch (error: any) {
    const errorMessage = error?.response?.data?.message || error?.message || '加载到期统计失败'
    ElMessage.error(errorMessage)
    console.error('加载到期统计失败:', error)
  } finally {
    maturityLoading.value = false
  }
}

const updateDistributionChart = () => {
  if (!distributionChartRef.value) {
    console.warn('分布图容器未找到')
    return
  }
  
  // 如果图表未初始化，先初始化
  if (!distributionChart) {
    distributionChart = echarts.init(distributionChartRef.value)
  }
  
  if (monthlyStats.distribution.length === 0) {
    // 空数据，显示空状态
    distributionChart.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: {
          color: '#999',
          fontSize: 16
        }
      }
    }, true)
    return
  }
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const item = monthlyStats.distribution.find(d => d.accountName === params.name)
        const percentage = item ? item.percentage.toFixed(2) : params.percent.toFixed(2)
        return `${params.seriesName}<br/>${params.name}: ¥${params.value.toLocaleString()} (${percentage}%)`
      }
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '存款分布',
        type: 'pie',
        radius: '50%',
        data: monthlyStats.distribution.map(item => ({
          value: Number(item.amount),
          name: item.accountName
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
  distributionChart.setOption(option, true)
}

const updateTrendChart = () => {
  if (!trendChartRef.value) {
    console.warn('趋势图容器未找到')
    return
  }
  
  // 如果图表未初始化，先初始化
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  
  if (trendData.data.length === 0) {
    // 空数据，显示空状态
    trendChart.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: {
          color: '#999',
          fontSize: 16
        }
      }
    }, true)
    return
  }
  
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const param = params[0]
        const dataIndex = param.dataIndex
        const monthData = trendData.data[dataIndex]
        
        let tooltipContent = `${param.name}<br/>存款总额: ¥${param.value.toLocaleString()}`
        
        // 如果有备注，显示备注
        if (monthData && monthData.notes && monthData.notes.length > 0) {
          tooltipContent += '<br/>快照备注:'
          monthData.notes.forEach((note: string) => {
            tooltipContent += `<br/>${note}`
          })
        }
        
        return tooltipContent
      }
    },
    xAxis: {
      type: 'category',
      data: trendData.data.map(item => item.month),
      axisLabel: {
        rotate: 45 // 如果月份太多，旋转45度
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value: number) => {
          if (value >= 10000) {
            return `¥${(value / 10000).toFixed(1)}万`
          }
          return `¥${value.toLocaleString()}`
        }
      }
    },
    series: [
      {
        name: '存款总额',
        type: 'line',
        data: trendData.data.map(item => Number(item.totalAmount)),
        smooth: true,
        areaStyle: {
          opacity: 0.3
        }
      }
    ]
  }
  trendChart.setOption(option, true)
}

const updateYearlyChart = () => {
  if (!yearlyChartRef.value) {
    console.warn('年度统计图容器未找到')
    return
  }
  
  // 如果图表未初始化，先初始化
  if (!yearlyChart) {
    yearlyChart = echarts.init(yearlyChartRef.value)
  }
  
  if (yearlyData.data.length === 0) {
    // 空数据，显示空状态
    yearlyChart.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: {
          color: '#999',
          fontSize: 16
        }
      }
    }, true)
    return
  }
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const param = params[0]
        const value = param.value
        const sign = value >= 0 ? '+' : ''
        return `${param.name}<br/>资产变化增值: ${sign}¥${value.toLocaleString()}`
      }
    },
    xAxis: {
      type: 'category',
      data: yearlyData.data.map(item => item.year),
      axisLabel: {
        rotate: 0
      }
    },
    yAxis: {
      type: 'value',
      name: '增值金额（元）',
      axisLabel: {
        formatter: (value: number) => {
          if (value >= 10000) {
            return `¥${(value / 10000).toFixed(1)}万`
          }
          return `¥${value.toLocaleString()}`
        }
      }
    },
    series: [
      {
        name: '年度资产变化增值',
        type: 'bar',
        data: yearlyData.data.map(item => Number(item.increase)),
        itemStyle: {
          color: (params: any) => {
            // 增值为正数显示绿色，负数为红色
            return params.value >= 0 ? '#67c23a' : '#f56c6c'
          }
        },
        label: {
          show: true,
          position: 'top',
          formatter: (params: any) => {
            const value = params.value
            const sign = value >= 0 ? '+' : ''
            return `${sign}¥${value.toLocaleString()}`
          }
        }
      }
    ]
  }
  yearlyChart.setOption(option, true)
}
</script>

<style scoped>
.statistics-container {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-header {
  margin-bottom: 20px;
}

.monthly-stats,
.trend-stats,
.yearly-stats {
  padding: 20px 0;
}

.chart-container {
  margin-top: 20px;
}

.chart-container h4 {
  margin-bottom: 15px;
  color: #666;
  font-size: 16px;
  font-weight: 500;
}
</style>
