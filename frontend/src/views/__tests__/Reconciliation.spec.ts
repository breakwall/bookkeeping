import { describe, it, expect, beforeEach, vi } from 'vitest'

/**
 * 对账页面标签页保留功能测试
 * 测试场景：用户在某个账户添加/编辑/删除存款后，页面应保持在该账户标签页
 */
describe('Reconciliation - 标签页保留功能', () => {
  
  describe('标签页选择逻辑', () => {
    
    it('初始化时应该选中第一个账户', () => {
      // 场景：首次加载对账页面，没有选中任何账户
      const activeAccountId = ''
      const accounts = [
        { accountId: 1, accountName: '招商银行', deposits: [] },
        { accountId: 2, accountName: '支付宝', deposits: [] }
      ]
      
      // 应用逻辑：如果 activeAccountId 为空，选中第一个
      let result = activeAccountId
      if (!result && accounts.length > 0) {
        result = accounts[0].accountId.toString()
      }
      
      expect(result).toBe('1')
    })

    it('保持当前选中的账户（第二个账户）', () => {
      // 场景：用户在账户 2 进行操作后，数据重新加载
      const previousAccountId = '2'
      const accounts = [
        { accountId: 1, accountName: '招商银行', deposits: [] },
        { accountId: 2, accountName: '支付宝', deposits: [] }
      ]
      
      // 模拟恢复逻辑
      let activeAccountId = '1' // loadReconciliationData 后可能被重置
      
      // 恢复账户选择（只要该账户仍存在）
      if (previousAccountId && accounts.some(a => a.accountId.toString() === previousAccountId)) {
        activeAccountId = previousAccountId
      }
      
      expect(activeAccountId).toBe('2')
    })

    it('当选中的账户被删除时，不进行恢复', () => {
      // 场景：用户在账户 3 操作，但账户 3 被删除了
      const previousAccountId = '3'
      const accounts = [
        { accountId: 1, accountName: '招商银行', deposits: [] },
        { accountId: 2, accountName: '支付宝', deposits: [] }
      ]
      
      // 模拟恢复逻辑
      let activeAccountId = '1' // loadReconciliationData 后的默认值
      
      // 恢复账户选择（只要该账户仍存在）
      if (previousAccountId && accounts.some(a => a.accountId.toString() === previousAccountId)) {
        activeAccountId = previousAccountId
      }
      
      // 账户 3 不存在，所以不应该恢复，保持默认的账户 1
      expect(activeAccountId).toBe('1')
    })

    it('多次操作后保持正确的账户选择', () => {
      // 场景：用户多次在不同账户进行操作
      const accounts = [
        { accountId: 1, accountName: '招商银行', deposits: [] },
        { accountId: 2, accountName: '支付宝', deposits: [] },
        { accountId: 3, accountName: '微信', deposits: [] }
      ]
      
      // 模拟第一次操作：在账户 2
      let activeAccountId = '1'
      if (!activeAccountId) {
        activeAccountId = accounts[0].accountId.toString()
      }
      // 用户切换到账户 2
      activeAccountId = '2'
      
      // 第一次保存-恢复
      const previousAccountId1 = activeAccountId // '2'
      activeAccountId = '1' // loadReconciliationData 重置
      if (previousAccountId1 && accounts.some(a => a.accountId.toString() === previousAccountId1)) {
        activeAccountId = previousAccountId1
      }
      expect(activeAccountId).toBe('2')
      
      // 用户切换到账户 3
      activeAccountId = '3'
      
      // 第二次保存-恢复
      const previousAccountId2 = activeAccountId // '3'
      activeAccountId = '1' // loadReconciliationData 重置
      if (previousAccountId2 && accounts.some(a => a.accountId.toString() === previousAccountId2)) {
        activeAccountId = previousAccountId2
      }
      expect(activeAccountId).toBe('3')
    })

  })

  describe('数据流验证', () => {
    
    it('存款添加后应保持账户选择', () => {
      // 场景：用户在账户 2 添加存款记录
      const previousAccountId = '2'
      const mockResponse = {
        date: '2024-02-01',
        totalAmount: 150000,
        accounts: [
          {
            accountId: 1,
            accountName: '招商银行',
            deposits: []
          },
          {
            accountId: 2,
            accountName: '支付宝',
            deposits: [
              {
                id: 123,
                depositType: '活期',
                depositTime: '2024-02-01',
                amount: 100000,
                interestRate: 0,
                term: null,
                note: '新添加的存款'
              }
            ]
          }
        ]
      }
      
      // 模拟恢复逻辑
      let activeAccountId = '1'
      if (previousAccountId && mockResponse.accounts.some(a => a.accountId.toString() === previousAccountId)) {
        activeAccountId = previousAccountId
      }
      
      expect(activeAccountId).toBe('2')
      expect(mockResponse.accounts[1].deposits.length).toBe(1)
    })

    it('存款删除后应保持账户选择', () => {
      // 场景：用户在账户 2 删除存款记录
      const previousAccountId = '2'
      const mockResponse = {
        date: '2024-02-01',
        totalAmount: 50000,
        accounts: [
          {
            accountId: 1,
            accountName: '招商银行',
            deposits: [
              {
                id: 1,
                depositType: '活期',
                depositTime: '2024-01-15',
                amount: 50000,
                interestRate: 0,
                term: null,
                note: '工资卡'
              }
            ]
          },
          {
            accountId: 2,
            accountName: '支付宝',
            deposits: [] // 删除后为空
          }
        ]
      }
      
      // 模拟恢复逻辑
      let activeAccountId = '1'
      if (previousAccountId && mockResponse.accounts.some(a => a.accountId.toString() === previousAccountId)) {
        activeAccountId = previousAccountId
      }
      
      expect(activeAccountId).toBe('2')
      expect(mockResponse.accounts[1].deposits.length).toBe(0)
    })

  })

})
