package com.bookkeeping.service;

import com.bookkeeping.dto.ReconciliationDataResponse;
import com.bookkeeping.dto.ReconciliationHistoryResponse;
import com.bookkeeping.dto.SaveReconciliationRequest;
import com.bookkeeping.entity.Account;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReconciliationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReconciliationService.class);
    
    @Autowired
    private DepositRepository depositRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private ReconciliationSnapshotRepository snapshotRepository;
    
    /**
     * 获取对账数据
     */
    public ReconciliationDataResponse getReconciliationData(Long userId, LocalDate date) {
        logger.debug("获取对账数据 - userId: {}, date: {}", userId, date);
        
        // 检查该日期是否有快照（只基于快照表判断）
        Optional<ReconciliationSnapshot> snapshotOpt = snapshotRepository.findByUserIdAndReconciliationDate(userId, date);
        
        List<Deposit> deposits;
        String note = null;
        BigDecimal totalAmount = BigDecimal.ZERO;
        boolean hasSnapshot = snapshotOpt.isPresent();
        
        logger.debug("快照查询结果 - hasSnapshot: {}", hasSnapshot);
        
        if (hasSnapshot) {
            // 快照表中有记录，该日期有快照
            ReconciliationSnapshot snapshot = snapshotOpt.get();
            note = snapshot.getNote();
            totalAmount = snapshot.getTotalAmount();
            // 获取该快照的存款记录
            deposits = depositRepository.findByUserIdAndReconciliationDate(userId, date);
            logger.debug("快照存在 - note: {}, totalAmount: {}, deposits count: {}", note, totalAmount, deposits.size());
        } else {
            // 快照表中没有记录，该日期没有快照，返回空数据（不返回启用的账户）
            deposits = new ArrayList<>();
            totalAmount = BigDecimal.ZERO;
            note = null;
            logger.debug("快照不存在 - 返回空数据");
        }
        
        // 按账户分组
        Map<Long, List<Deposit>> depositsByAccount = deposits.stream()
                .collect(Collectors.groupingBy(Deposit::getAccountId));
        
        // 决定显示哪些账户
        List<Account> accountsToShow = new ArrayList<>();
        if (hasSnapshot) {
            // 快照存在，只显示快照中涉及的账户（即使当前已禁用，保留历史事实）
            // 如果没有存款记录（空快照），返回空账户列表
            Set<Long> accountIdsInSnapshot = depositsByAccount.keySet();
            logger.debug("快照中的账户ID: {}", accountIdsInSnapshot);
            if (!accountIdsInSnapshot.isEmpty()) {
                accountsToShow = accountRepository.findByUserIdAndIdIn(userId, new ArrayList<>(accountIdsInSnapshot));
            }
            // 如果 accountIdsInSnapshot 为空，accountsToShow 保持为空列表（空快照）
            logger.debug("显示的账户数量: {}", accountsToShow.size());
        } else {
            // 快照不存在，也返回空账户列表（不显示启用的账户，让前端知道该日期没有快照）
            accountsToShow = new ArrayList<>();
            logger.debug("无快照，返回空账户列表");
        }
        
        // 构建响应数据
        List<ReconciliationDataResponse.AccountDepositData> accountDataList = new ArrayList<>();
        
        for (Account account : accountsToShow) {
            List<Deposit> accountDeposits = depositsByAccount.getOrDefault(account.getId(), new ArrayList<>());
            
            // 构建存款记录响应
            List<com.bookkeeping.dto.DepositResponse> depositResponses = accountDeposits.stream()
                    .map(deposit -> com.bookkeeping.dto.DepositResponse.fromEntity(deposit))
                    .collect(Collectors.toList());
            
            accountDataList.add(new ReconciliationDataResponse.AccountDepositData(
                account.getId(),
                account.getName(),
                depositResponses
            ));
        }
        
        ReconciliationDataResponse response = new ReconciliationDataResponse(date, note, totalAmount, accountDataList);
        logger.debug("返回数据 - date: {}, totalAmount: {}, accounts count: {}", response.getDate(), response.getTotalAmount(), response.getAccounts().size());
        
        return response;
    }
    
    /**
     * 保存对账快照
     */
    @Transactional
    public void saveReconciliation(Long userId, LocalDate date, SaveReconciliationRequest request) {
        // 验证所有账户是否属于用户
        for (SaveReconciliationRequest.AccountDepositData accountData : request.getAccounts()) {
            if (!accountRepository.existsByIdAndUserId(accountData.getAccountId(), userId)) {
                throw new RuntimeException("账户不存在或不属于当前用户");
            }
        }
        
        // 删除该日期的旧数据（包括存款记录和快照）
        depositRepository.deleteByUserIdAndReconciliationDate(userId, date);
        snapshotRepository.deleteByUserIdAndReconciliationDate(userId, date);
        
        // 计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        // 保存新数据
        for (SaveReconciliationRequest.AccountDepositData accountData : request.getAccounts()) {
            for (SaveReconciliationRequest.DepositData depositData : accountData.getDeposits()) {
                Deposit deposit;
                
                if (depositData.getId() != null) {
                    // 检查该ID是否属于当前日期（防止使用历史记录的ID）
                    Optional<Deposit> existingDeposit = depositRepository.findByIdAndUserId(depositData.getId(), userId);
                    if (existingDeposit.isPresent() && existingDeposit.get().getReconciliationDate().equals(date)) {
                        // 该ID属于当前日期，更新
                        deposit = existingDeposit.get();
                    } else {
                        // 该ID不属于当前日期（可能是历史记录的ID），创建新记录
                        deposit = new Deposit();
                        deposit.setUserId(userId);
                        deposit.setAccountId(accountData.getAccountId());
                    }
                } else {
                    // 创建新记录
                    deposit = new Deposit();
                    deposit.setUserId(userId);
                    deposit.setAccountId(accountData.getAccountId());
                }
                
                deposit.setDepositType(depositData.getDepositType());
                deposit.setDepositTime(depositData.getDepositTime());
                deposit.setAmount(depositData.getAmount());
                deposit.setInterestRate(depositData.getInterestRate());
                deposit.setTerm(depositData.getTerm());
                deposit.setNote(depositData.getNote());
                deposit.setReconciliationDate(date);
                
                depositRepository.save(deposit);
                
                // 累加总金额
                if (depositData.getAmount() != null) {
                    totalAmount = totalAmount.add(depositData.getAmount());
                }
            }
        }
        
        // 保存或更新快照记录
        Optional<ReconciliationSnapshot> existingSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(userId, date);
        ReconciliationSnapshot snapshot;
        if (existingSnapshot.isPresent()) {
            snapshot = existingSnapshot.get();
        } else {
            snapshot = new ReconciliationSnapshot();
            snapshot.setUserId(userId);
            snapshot.setReconciliationDate(date);
        }
        
        snapshot.setTotalAmount(totalAmount);
        snapshot.setNote(request.getNote());
        snapshotRepository.save(snapshot);
    }
    
    /**
     * 更新快照备注
     */
    @Transactional
    public void updateSnapshotNote(Long userId, LocalDate date, String note) {
        Optional<ReconciliationSnapshot> snapshotOpt = snapshotRepository.findByUserIdAndReconciliationDate(userId, date);
        if (snapshotOpt.isEmpty()) {
            throw new RuntimeException("该日期的对账快照不存在");
        }
        ReconciliationSnapshot snapshot = snapshotOpt.get();
        snapshot.setNote(note);
        snapshotRepository.save(snapshot);
    }
    
    /**
     * 获取最近一次对账日期（只基于快照表）
     */
    public LocalDate getLatestReconciliationDate(Long userId) {
        logger.debug("获取最近一次对账日期 - userId: {}", userId);
        // 只从快照表查询，判断是否有快照应该基于快照表
        LocalDate latestDate = snapshotRepository.findMaxReconciliationDateByUserId(userId).orElse(null);
        logger.debug("最近一次对账日期: {}", latestDate);
        return latestDate;
    }
    
    /**
     * 获取历史对账记录（只基于快照表，判断是否有快照应该基于快照表）
     */
    public ReconciliationHistoryResponse getReconciliationHistory(Long userId) {
        // 只从快照表获取日期，判断是否有快照应该基于快照表
        List<ReconciliationSnapshot> allSnapshots = snapshotRepository.findByUserIdOrderByReconciliationDateDesc(userId);
        
        // 对每个快照日期，统计该日期的存款记录数
        List<ReconciliationHistoryResponse.HistoryItem> historyItems = allSnapshots.stream()
                .map(snapshot -> {
                    LocalDate date = snapshot.getReconciliationDate();
                    // 统计该日期的存款记录数
                    long recordCount = depositRepository.countByUserIdAndReconciliationDate(userId, date);
                    return new ReconciliationHistoryResponse.HistoryItem(date, recordCount);
                })
                .collect(Collectors.toList());
        
        return new ReconciliationHistoryResponse(historyItems);
    }
    
    /**
     * 获取所有快照日期列表（只基于快照表，判断是否有快照应该基于快照表）
     */
    public List<LocalDate> getSnapshotDates(Long userId) {
        logger.debug("获取所有快照日期 - userId: {}", userId);
        // 只从快照表查询，判断是否有快照应该基于快照表
        List<ReconciliationSnapshot> allSnapshots = snapshotRepository.findByUserIdOrderByReconciliationDateDesc(userId);
        List<LocalDate> dates = allSnapshots.stream()
                .map(ReconciliationSnapshot::getReconciliationDate)
                .collect(Collectors.toList());
        logger.debug("快照日期列表 (倒序): {}, 总数: {}", dates, dates.size());
        return dates;
    }
    
    /**
     * 获取上一个快照日期（更早的快照）
     */
    public LocalDate getPreviousSnapshotDate(Long userId, LocalDate currentDate) {
        logger.debug("获取上一个快照日期 - userId: {}, currentDate: {}", userId, currentDate);
        List<LocalDate> snapshotDates = getSnapshotDates(userId);
        
        if (snapshotDates.isEmpty()) {
            logger.debug("没有任何快照，返回 null");
            return null; // 没有任何快照
        }
        
        // 如果当前日期不在快照列表中，找到最近的快照日期（小于当前日期的第一个）
        if (!snapshotDates.contains(currentDate)) {
            // 找到第一个小于当前日期的快照日期（最近的，因为列表是倒序的）
            Optional<LocalDate> nearestDate = snapshotDates.stream()
                    .filter(date -> date.isBefore(currentDate))
                    .findFirst();
            if (nearestDate.isPresent()) {
                // 如果找到了最近的快照，直接返回它（这就是"上一个快照"）
                logger.debug("找到最近的上一个快照日期: {}", nearestDate.get());
                return nearestDate.get();
            } else {
                // 当前日期早于所有快照，返回null（已经是最早的）
                logger.debug("当前日期早于所有快照，返回 null");
                return null;
            }
        }
        
        // 如果当前日期本身就是一个快照日期，找到列表中比它更早的快照
        int index = snapshotDates.indexOf(currentDate);
        logger.debug("当前日期在快照列表中的索引: {}", index);
        if (index < snapshotDates.size() - 1) {
            // 还有更早的快照（在列表中索引+1的位置，因为列表是倒序的）
            LocalDate previousDate = snapshotDates.get(index + 1);
            logger.debug("找到上一个快照日期: {}", previousDate);
            return previousDate;
        } else {
            // 这是最早的快照
            logger.debug("这是最早的快照，返回 null");
            return null;
        }
    }
    
    /**
     * 获取下一个快照日期（更新的快照）
     */
    public LocalDate getNextSnapshotDate(Long userId, LocalDate currentDate) {
        logger.debug("获取下一个快照日期 - userId: {}, currentDate: {}", userId, currentDate);
        List<LocalDate> snapshotDates = getSnapshotDates(userId);
        
        if (snapshotDates.isEmpty()) {
            logger.debug("没有任何快照，返回 null");
            return null; // 没有任何快照
        }
        
        // 如果当前日期不在快照列表中，找到最近的快照日期（大于当前日期的第一个）
        if (!snapshotDates.contains(currentDate)) {
            // 找到第一个大于当前日期的快照日期（最近的，因为列表是倒序的）
            Optional<LocalDate> nearestDate = snapshotDates.stream()
                    .filter(date -> date.isAfter(currentDate))
                    .findFirst();
            if (nearestDate.isPresent()) {
                // 如果找到了最近的快照，直接返回它（这就是"下一个快照"）
                logger.debug("找到最近的下一个快照日期: {}", nearestDate.get());
                return nearestDate.get();
            } else {
                // 当前日期晚于所有快照，返回null（已经是最新的）
                logger.debug("当前日期晚于所有快照，返回 null");
                return null;
            }
        }
        
        // 如果当前日期本身就是一个快照日期，找到列表中比它更新的快照
        int index = snapshotDates.indexOf(currentDate);
        logger.debug("当前日期在快照列表中的索引: {}", index);
        if (index > 0) {
            // 还有更新的快照（在列表中索引-1的位置，因为列表是倒序的）
            LocalDate nextDate = snapshotDates.get(index - 1);
            logger.debug("找到下一个快照日期: {}", nextDate);
            return nextDate;
        } else {
            // 这是最新的快照
            logger.debug("这是最新的快照，返回 null");
            return null;
        }
    }
    
    /**
     * 新建对账：将选中日期之前最近一次快照复制到选中日期并保存到数据库
     * 如果选中日期之前没有历史快照，则创建一个空快照（总金额为0，没有存款记录）
     */
    @Transactional
    public void createNewReconciliation(Long userId, LocalDate targetDate) {
        // 检查目标日期是否已有快照（只基于快照表判断）
        boolean hasSnapshot = snapshotRepository.existsByUserIdAndReconciliationDate(userId, targetDate);
        if (hasSnapshot) {
            throw new RuntimeException("该日期的对账快照已存在，无法重复创建");
        }
        
        // 查找目标日期之前最近的一次快照日期
        LocalDate previousDate = findLatestSnapshotDateBefore(userId, targetDate);
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Deposit> depositsToCopy = new ArrayList<>();
        
        if (previousDate != null) {
            // 有前一次快照，获取其存款记录
            depositsToCopy = depositRepository.findByUserIdAndReconciliationDate(userId, previousDate);
            
            // 复制存款记录到目标日期
            for (Deposit previousDeposit : depositsToCopy) {
                Deposit newDeposit = new Deposit();
                newDeposit.setUserId(userId);
                newDeposit.setAccountId(previousDeposit.getAccountId());
                newDeposit.setDepositType(previousDeposit.getDepositType());
                newDeposit.setDepositTime(previousDeposit.getDepositTime());
                newDeposit.setAmount(previousDeposit.getAmount());
                newDeposit.setInterestRate(previousDeposit.getInterestRate());
                newDeposit.setTerm(previousDeposit.getTerm());
                newDeposit.setNote(previousDeposit.getNote());
                newDeposit.setReconciliationDate(targetDate);
                
                depositRepository.save(newDeposit);
                
                // 累加总金额
                if (previousDeposit.getAmount() != null) {
                    totalAmount = totalAmount.add(previousDeposit.getAmount());
                }
            }
        }
        // 如果没有前一次快照，depositsToCopy 为空，totalAmount 为 0，创建空快照
        
        // 创建目标日期的快照记录（备注清空）
        ReconciliationSnapshot newSnapshot = new ReconciliationSnapshot();
        newSnapshot.setUserId(userId);
        newSnapshot.setReconciliationDate(targetDate);
        newSnapshot.setTotalAmount(totalAmount);
        newSnapshot.setNote(null); // 备注清空
        snapshotRepository.save(newSnapshot);
    }
    
    /**
     * 查找指定日期之前最近的一次快照日期（只基于快照表）
     */
    private LocalDate findLatestSnapshotDateBefore(Long userId, LocalDate beforeDate) {
        // 只从快照表获取，判断是否有快照应该基于快照表
        List<LocalDate> snapshotDates = getSnapshotDates(userId);
        
        if (snapshotDates.isEmpty()) {
            return null; // 没有任何快照
        }
        
        // 找到第一个小于 beforeDate 的日期（最近的，因为列表是倒序的）
        return snapshotDates.stream()
                .filter(date -> date.isBefore(beforeDate))
                .findFirst()
                .orElse(null); // 如果没有找到，返回 null（表示没有前一次快照）
    }
}
