package com.bookkeeping.service;

import com.bookkeeping.dto.AccountResponse;
import com.bookkeeping.dto.CreateAccountRequest;
import com.bookkeeping.dto.UpdateAccountRequest;
import com.bookkeeping.entity.Account;
import com.bookkeeping.repository.AccountRepository;
import com.bookkeeping.repository.DepositRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private DepositRepository depositRepository;
    
    /**
     * 获取用户的所有账户列表
     * 先显示启用的账户，后显示未启用的账户，同一状态下按创建时间倒序
     */
    public List<AccountResponse> getAccounts(Long userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);
        return accounts.stream()
                .sorted(Comparator
                        // 先按状态排序：ACTIVE在前，DISABLED在后
                        .comparing((Account a) -> a.getStatus() == Account.AccountStatus.ACTIVE ? 0 : 1)
                        // 同一状态下按创建时间倒序
                        .thenComparing(Account::getCreatedAt, Comparator.reverseOrder()))
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取用户启用的账户列表（用于对账管理等场景）
     */
    public List<AccountResponse> getActiveAccounts(Long userId) {
        List<Account> accounts = accountRepository.findByUserIdAndStatus(userId, Account.AccountStatus.ACTIVE);
        return accounts.stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取单个账户详情
     */
    public AccountResponse getAccount(Long id, Long userId) {
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("账户不存在"));
        return AccountResponse.fromEntity(account);
    }
    
    /**
     * 创建账户
     */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, Long userId) {
        // 检查账户名称是否已存在
        if (accountRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new RuntimeException("账户名称已存在");
        }
        
        Account account = new Account();
        account.setUserId(userId);
        account.setName(request.getName());
        account.setType(request.getType());
        account.setNote(request.getNote());
        account.setStatus(Account.AccountStatus.ACTIVE);
        
        account = accountRepository.save(account);
        return AccountResponse.fromEntity(account);
    }
    
    /**
     * 更新账户
     */
    @Transactional
    public AccountResponse updateAccount(Long id, UpdateAccountRequest request, Long userId) {
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("账户不存在"));
        
        // 检查账户名称是否与其他账户重复（排除当前账户）
        if (accountRepository.existsByUserIdAndNameAndIdNot(userId, request.getName(), id)) {
            throw new RuntimeException("账户名称已存在");
        }
        
        account.setName(request.getName());
        account.setType(request.getType());
        account.setNote(request.getNote());
        
        account = accountRepository.save(account);
        return AccountResponse.fromEntity(account);
    }
    
    /**
     * 删除账户
     * 如果有存款记录，标记为停用；如果没有，物理删除
     */
    @Transactional
    public void deleteAccount(Long id, Long userId) {
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("账户不存在"));
        
        // 检查是否有存款记录
        if (depositRepository.existsByAccountId(id)) {
            // 有记录，标记为停用
            account.setStatus(Account.AccountStatus.DISABLED);
            accountRepository.save(account);
        } else {
            // 无记录，物理删除
            accountRepository.delete(account);
        }
    }
    
    /**
     * 启用账户
     */
    @Transactional
    public AccountResponse enableAccount(Long id, Long userId) {
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("账户不存在"));
        
        account.setStatus(Account.AccountStatus.ACTIVE);
        account = accountRepository.save(account);
        return AccountResponse.fromEntity(account);
    }
    
    /**
     * 禁用账户
     */
    @Transactional
    public AccountResponse disableAccount(Long id, Long userId) {
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("账户不存在"));
        
        account.setStatus(Account.AccountStatus.DISABLED);
        account = accountRepository.save(account);
        return AccountResponse.fromEntity(account);
    }
}
