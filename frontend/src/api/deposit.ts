import request from './request'

export interface Deposit {
  id: number
  accountId: number
  depositType: string
  depositTime: string
  amount: number
  interestRate?: number
  term?: number
  note?: string
  reconciliationDate: string
}

export interface CreateDepositRequest {
  accountId: number
  depositType: string
  depositTime: string
  amount: number
  interestRate?: number
  term?: number
  note?: string
  reconciliationDate: string
}

export interface UpdateDepositRequest {
  depositType: string
  depositTime: string
  amount: number
  interestRate?: number
  term?: number
  note?: string
}

export const depositApi = {
  // 获取账户的存款记录
  getDepositsByAccount(accountId: number, date?: string) {
    const params: any = {}
    if (date) {
      params.date = date
    }
    return request.get<Deposit[]>(`/accounts/${accountId}/deposits`, { params })
  },

  // 创建存款记录
  createDeposit(data: CreateDepositRequest) {
    return request.post<Deposit>('/deposits', data)
  },

  // 更新存款记录
  updateDeposit(id: number, data: UpdateDepositRequest) {
    return request.put<Deposit>(`/deposits/${id}`, data)
  },

  // 删除存款记录
  deleteDeposit(id: number) {
    return request.delete(`/deposits/${id}`)
  }
}
