import request from './request'

export interface MonthlyStatistics {
  month: string
  totalAmount: number
  distribution: AccountDistribution[]
}

export interface AccountDistribution {
  accountId: number
  accountName: string
  amount: number
  percentage: number
}

export interface TrendStatistics {
  period: string
  data: TrendDataItem[]
}

export interface TrendDataItem {
  month: string
  totalAmount: number
  notes?: string[] // 该月所有有备注的快照备注列表，格式：["2024-01-15: 备注1", "2024-01-20: 备注2"]
}

export interface YearlyStatistics {
  data: YearlyDataItem[]
}

export interface YearlyDataItem {
  year: string // 年份，格式：YYYY
  increase: number // 该年的资产变化增值
}

export interface MaturityDataItem {
  accountName: string;
  depositAmount: number;
  depositTime: string;
  maturityDate: string;
  remainingDays: number;
}

export interface MaturityStatistics {
  data: MaturityDataItem[];
}

export const statisticsApi = {
  // 按月统计
  getMonthlyStatistics(month: string) {
    return request.get<MonthlyStatistics>('/statistics/monthly', {
      params: { month }
    })
  },

  // 趋势统计
  getTrendStatistics(period: string) {
    return request.get<TrendStatistics>('/statistics/trend', {
      params: { period }
    })
  },

  // 年度统计
  getYearlyStatistics() {
    return request.get<YearlyStatistics>('/statistics/yearly')
  },

  // 到期统计
  getMaturityStatistics() {
    return request.get<MaturityStatistics>('/statistics/maturity')
  }
}
