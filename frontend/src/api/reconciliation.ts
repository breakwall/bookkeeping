import request from './request'

export interface ReconciliationData {
  date: string
  note?: string // 快照备注
  totalAmount: number // 快照总金额
  accounts: AccountDepositData[]
}

export interface AccountDepositData {
  accountId: number
  accountName: string
  deposits: Deposit[]
}

export interface Deposit {
  id?: number
  depositType: string
  depositTime: string
  amount: number
  interestRate?: number
  term?: number
  note?: string
  reconciliationDate?: string
}

export interface SaveReconciliationRequest {
  date: string
  note?: string // 快照备注
  accounts: {
    accountId: number
    deposits: {
      id?: number
      depositType: string
      depositTime: string
      amount: number
      interestRate?: number
      term?: number
      note?: string
    }[]
  }[]
}

export interface ReconciliationHistory {
  dates: {
    date: string
    recordCount: number
  }[]
}

export const reconciliationApi = {
  // 获取对账数据
  getReconciliation(date?: string) {
    const params = date ? { date } : {}
    return request.get<ReconciliationData>('/reconciliation', { params })
  },

  // 保存对账快照
  saveReconciliation(date: string, data: SaveReconciliationRequest) {
    return request.post(`/reconciliation/save?date=${date}`, data)
  },

  // 获取最近一次对账日期
  getLatestReconciliationDate() {
    return request.get<string>('/reconciliation/latest')
  },

  // 获取历史对账记录
  getReconciliationHistory() {
    return request.get<ReconciliationHistory>('/reconciliation/history')
  },

  // 获取上一个快照日期
  getPreviousSnapshotDate(date: string) {
    return request.get<string | null>('/reconciliation/previous', {
      params: { date }
    })
  },

  // 获取下一个快照日期
  getNextSnapshotDate(date: string) {
    return request.get<string | null>('/reconciliation/next', {
      params: { date }
    })
  },

  // 新建对账：将选中日期之前最近一次快照复制到选中日期并保存到数据库
  createNewReconciliation(date: string) {
    return request.post(`/reconciliation/create-new?date=${date}`)
  },

  // 更新快照备注
  updateSnapshotNote(date: string, note: string) {
    return request.put(`/reconciliation/note?date=${date}`, { note })
  }
}
