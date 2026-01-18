package com.bookkeeping.repository;

import com.bookkeeping.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    /**
     * 查询用户的所有账户
     */
    List<Account> findByUserId(Long userId);
    
    /**
     * 查询用户的所有账户，按创建时间倒序
     */
    List<Account> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 查询用户启用的账户
     */
    List<Account> findByUserIdAndStatus(Long userId, Account.AccountStatus status);
    
    /**
     * 根据ID和用户ID查询账户（防止跨用户访问）
     */
    Optional<Account> findByIdAndUserId(Long id, Long userId);
    
    /**
     * 检查账户是否属于用户
     */
    boolean existsByIdAndUserId(Long id, Long userId);
    
    /**
     * 检查用户下是否存在相同名称的账户
     */
    boolean existsByUserIdAndName(Long userId, String name);
    
    /**
     * 检查用户下是否存在相同名称的账户（排除指定ID）
     */
    boolean existsByUserIdAndNameAndIdNot(Long userId, String name, Long id);
    
    /**
     * 根据用户ID和账户ID列表查询账户（用于历史快照显示）
     */
    List<Account> findByUserIdAndIdIn(Long userId, List<Long> accountIds);
}
