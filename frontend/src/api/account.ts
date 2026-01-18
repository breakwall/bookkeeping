import request from './request'

export interface Account {
  id: number
  name: string
  type: string
  note?: string
  status: 'ACTIVE' | 'DISABLED'
  createdAt: string
  updatedAt: string
}

export interface CreateAccountRequest {
  name: string
  type: string
  note?: string
}

export interface UpdateAccountRequest {
  name: string
  type: string
  note?: string
}

export const accountApi = {
  // 获取账户列表
  getAccounts() {
    return request.get<Account[]>('/accounts')
  },

  // 获取启用的账户列表
  getActiveAccounts() {
    return request.get<Account[]>('/accounts/active')
  },

  // 获取账户详情
  getAccount(id: number) {
    return request.get<Account>(`/accounts/${id}`)
  },

  // 创建账户
  createAccount(data: CreateAccountRequest) {
    return request.post<Account>('/accounts', data)
  },

  // 更新账户
  updateAccount(id: number, data: UpdateAccountRequest) {
    return request.put<Account>(`/accounts/${id}`, data)
  },

  // 删除账户
  deleteAccount(id: number) {
    return request.delete(`/accounts/${id}`)
  },

  // 启用账户
  enableAccount(id: number) {
    return request.post<Account>(`/accounts/${id}/enable`)
  },

  // 禁用账户
  disableAccount(id: number) {
    return request.post<Account>(`/accounts/${id}/disable`)
  }
}
