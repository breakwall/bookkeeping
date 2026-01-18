package com.bookkeeping.service;

import com.bookkeeping.dto.CreateDepositRequest;
import com.bookkeeping.dto.DepositResponse;
import com.bookkeeping.dto.UpdateDepositRequest;
import com.bookkeeping.entity.Deposit;
import com.bookkeeping.entity.ReconciliationSnapshot;
import com.bookkeeping.repository.AccountRepository;
import com.bookkeeping.repository.DepositRepository;
import com.bookkeeping.repository.ReconciliationSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DepositService {
    
    private static final Logger logger = LoggerFactory.getLogger(DepositService.class);
    
    @Autowired
    private DepositRepository depositRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private ReconciliationSnapshotRepository snapshotRepository;
    
    /**
     * 获取账户在指定日期的存款记录
     */
    public List<DepositResponse> getDepositsByAccount(Long accountId, Long userId, LocalDate date) {
        // 验证账户是否属于用户
        if (!accountRepository.existsByIdAndUserId(accountId, userId)) {
            throw new RuntimeException("账户不存在");
        }
        
        logger.info("[DepositService] 准备查询账户存款记录 - accountId: {}, userId: {}, date: {}", accountId, userId, date);
        try {
            List<Deposit> deposits = depositRepository.findByAccountIdAndReconciliationDate(accountId, date);
            logger.info("[DepositService] 查询账户存款记录成功 - count: {}", deposits.size());
            return deposits.stream()
                    .map(DepositResponse::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("[DepositService] 查询账户存款记录失败 - accountId: {}, userId: {}, date: {}, 错误: {}", 
                       accountId, userId, date, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 创建存款记录
     */
    @Transactional
    public DepositResponse createDeposit(CreateDepositRequest request, Long userId) {
        // 验证账户是否属于用户
        if (!accountRepository.existsByIdAndUserId(request.getAccountId(), userId)) {
            throw new RuntimeException("账户不存在");
        }
        
        Deposit deposit = new Deposit();
        deposit.setUserId(userId);
        deposit.setAccountId(request.getAccountId());
        deposit.setDepositType(request.getDepositType());
        deposit.setDepositTime(request.getDepositTime());
        deposit.setAmount(request.getAmount());
        deposit.setInterestRate(request.getInterestRate());
        deposit.setTerm(request.getTerm());
        deposit.setNote(request.getNote());
        deposit.setReconciliationDate(request.getReconciliationDate());
        
        deposit = depositRepository.save(deposit);
        
        // 如果该日期有快照，更新快照总金额
        updateSnapshotTotalAmount(userId, request.getReconciliationDate());
        
        return DepositResponse.fromEntity(deposit);
    }
    
    /**
     * 更新存款记录（允许编辑历史快照）
     */
    @Transactional
    public DepositResponse updateDeposit(Long id, UpdateDepositRequest request, Long userId) {
        Deposit deposit = depositRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("存款记录不存在"));
        
        LocalDate reconciliationDate = deposit.getReconciliationDate();
        
        // 允许编辑所有快照的存款记录（包括历史快照）
        deposit.setDepositType(request.getDepositType());
        deposit.setDepositTime(request.getDepositTime());
        deposit.setAmount(request.getAmount());
        deposit.setInterestRate(request.getInterestRate());
        deposit.setTerm(request.getTerm());
        deposit.setNote(request.getNote());
        
        deposit = depositRepository.save(deposit);
        
        // 如果该记录属于快照，更新快照总金额
        updateSnapshotTotalAmount(userId, reconciliationDate);
        
        return DepositResponse.fromEntity(deposit);
    }
    
    /**
     * 删除存款记录（允许删除历史快照的记录）
     */
    @Transactional
    public void deleteDeposit(Long id, Long userId) {
        Deposit deposit = depositRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("存款记录不存在"));
        
        LocalDate reconciliationDate = deposit.getReconciliationDate();
        
        // 允许删除所有快照的存款记录（包括历史快照）
        depositRepository.delete(deposit);
        
        // 如果该记录属于快照，更新快照总金额
        updateSnapshotTotalAmount(userId, reconciliationDate);
    }
    
    /**
     * 更新快照总金额
     */
    private void updateSnapshotTotalAmount(Long userId, LocalDate reconciliationDate) {
        Optional<ReconciliationSnapshot> snapshotOpt = snapshotRepository.findByUserIdAndReconciliationDate(userId, reconciliationDate);
        if (snapshotOpt.isPresent()) {
            ReconciliationSnapshot snapshot = snapshotOpt.get();
            // 重新计算该日期的所有存款记录总金额
            List<Deposit> deposits = depositRepository.findByUserIdAndReconciliationDate(userId, reconciliationDate);
            BigDecimal totalAmount = deposits.stream()
                    .map(Deposit::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            snapshot.setTotalAmount(totalAmount);
            snapshotRepository.save(snapshot);
        }
    }
    
    /**
     * 复制存款记录（用于初始化对账）
     */
    @Transactional
    public List<DepositResponse> copyDepositsFromDate(Long userId, LocalDate sourceDate, LocalDate targetDate) {
        List<Deposit> sourceDeposits = depositRepository.findByUserIdAndReconciliationDate(userId, sourceDate);
        
        return sourceDeposits.stream()
                .map(source -> {
                    Deposit newDeposit = new Deposit();
                    newDeposit.setUserId(userId);
                    newDeposit.setAccountId(source.getAccountId());
                    newDeposit.setDepositType(source.getDepositType());
                    newDeposit.setDepositTime(source.getDepositTime());
                    newDeposit.setAmount(source.getAmount());
                    newDeposit.setInterestRate(source.getInterestRate());
                    newDeposit.setTerm(source.getTerm());
                    newDeposit.setNote(source.getNote());
                    newDeposit.setReconciliationDate(targetDate);
                    
                    newDeposit = depositRepository.save(newDeposit);
                    return DepositResponse.fromEntity(newDeposit);
                })
                .collect(Collectors.toList());
    }
}
