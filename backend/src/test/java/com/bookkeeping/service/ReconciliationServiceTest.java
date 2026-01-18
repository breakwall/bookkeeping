package com.bookkeeping.service;

import com.bookkeeping.AbstractBaseTest;
import com.bookkeeping.dto.ReconciliationDataResponse;
import com.bookkeeping.dto.ReconciliationHistoryResponse;
import com.bookkeeping.dto.SaveReconciliationRequest;
import com.bookkeeping.entity.Account;
import com.bookkeeping.entity.Deposit;
import com.bookkeeping.entity.ReconciliationSnapshot;
import com.bookkeeping.repository.AccountRepository;
import com.bookkeeping.repository.DepositRepository;
import com.bookkeeping.repository.ReconciliationSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReconciliationService 单元测试
 * 
 * 测试覆盖：
 * - 获取对账数据（有快照/无快照）
 * - 保存对账快照
 * - 更新快照备注
 * - 获取最近一次对账日期
 * - 获取快照日期列表
 * - 上一个/下一个快照日期导航
 * - 新建对账（复制快照）
 */
@DisplayName("ReconciliationService 单元测试")
public class ReconciliationServiceTest extends AbstractBaseTest {
    
    @Autowired
    private ReconciliationService reconciliationService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private DepositRepository depositRepository;
    
    @Autowired
    private ReconciliationSnapshotRepository snapshotRepository;
    
    private Long userId;
    private Account account1;
    private Account account2;
    
    @BeforeEach
    public void setUp() {
        // 创建测试用户
        com.bookkeeping.dto.RegisterRequest registerRequest = new com.bookkeeping.dto.RegisterRequest();
        registerRequest.setUsername(generateUniqueUsername());
        registerRequest.setPassword("testPassword123");
        registerRequest.setEmail(generateUniqueEmail());
        com.bookkeeping.dto.AuthResponse authResponse = userService.register(registerRequest);
        userId = authResponse.getId();
        
        // 创建测试账户
        account1 = new Account();
        account1.setUserId(userId);
        account1.setName("测试账户1");
        account1.setType("定期存款");
        account1.setStatus(Account.AccountStatus.ACTIVE);
        account1 = accountRepository.save(account1);
        
        account2 = new Account();
        account2.setUserId(userId);
        account2.setName("测试账户2");
        account2.setType("活期存款");
        account2.setStatus(Account.AccountStatus.ACTIVE);
        account2 = accountRepository.save(account2);
    }
    
    @Test
    @DisplayName("UC-RECON-001: 获取有快照的对账数据")
    public void testGetReconciliationData_WithSnapshot() {
        // Given: 创建一个快照和存款记录
        LocalDate date = LocalDate.of(2024, 1, 15);
        BigDecimal totalAmount = new BigDecimal("100000.00");
        String note = "测试备注";
        
        ReconciliationSnapshot snapshot = new ReconciliationSnapshot();
        snapshot.setUserId(userId);
        snapshot.setReconciliationDate(date);
        snapshot.setTotalAmount(totalAmount);
        snapshot.setNote(note);
        snapshotRepository.save(snapshot);
        
        Deposit deposit1 = new Deposit();
        deposit1.setUserId(userId);
        deposit1.setAccountId(account1.getId());
        deposit1.setDepositType("定期存款");
        deposit1.setDepositTime(date);
        deposit1.setAmount(new BigDecimal("50000.00"));
        deposit1.setReconciliationDate(date);
        depositRepository.save(deposit1);
        
        Deposit deposit2 = new Deposit();
        deposit2.setUserId(userId);
        deposit2.setAccountId(account2.getId());
        deposit2.setDepositType("活期存款");
        deposit2.setDepositTime(date);
        deposit2.setAmount(new BigDecimal("50000.00"));
        deposit2.setReconciliationDate(date);
        depositRepository.save(deposit2);
        
        // When: 获取对账数据
        ReconciliationDataResponse response = reconciliationService.getReconciliationData(userId, date);
        
        // Then: 验证结果
        assertNotNull(response);
        assertEquals(date, response.getDate());
        assertEquals(note, response.getNote());
        assertEquals(totalAmount, response.getTotalAmount());
        assertEquals(2, response.getAccounts().size());
        
        // 验证账户数据
        assertEquals(account1.getName(), response.getAccounts().get(0).getAccountName());
        assertEquals(1, response.getAccounts().get(0).getDeposits().size());
        assertEquals(new BigDecimal("50000.00"), response.getAccounts().get(0).getDeposits().get(0).getAmount());
        
        System.out.println("✓ UC-RECON-001: 获取有快照的对账数据 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-002: 获取无快照的对账数据")
    public void testGetReconciliationData_WithoutSnapshot() {
        // Given: 选择一个没有快照的日期
        LocalDate date = LocalDate.of(2024, 1, 20);
        
        // When: 获取对账数据
        ReconciliationDataResponse response = reconciliationService.getReconciliationData(userId, date);
        
        // Then: 验证结果（应该返回空数据）
        assertNotNull(response);
        assertEquals(date, response.getDate());
        assertNull(response.getNote());
        assertEquals(BigDecimal.ZERO, response.getTotalAmount());
        assertTrue(response.getAccounts().isEmpty());
        
        System.out.println("✓ UC-RECON-002: 获取无快照的对账数据 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-003: 保存对账快照成功")
    public void testSaveReconciliation_Success() {
        // Given: 准备保存请求
        LocalDate date = LocalDate.of(2024, 2, 1);
        String note = "2月对账";
        
        SaveReconciliationRequest request = new SaveReconciliationRequest();
        request.setDate(date);
        request.setNote(note);
        
        List<SaveReconciliationRequest.AccountDepositData> accountDataList = new ArrayList<>();
        
        // 账户1的存款记录
        SaveReconciliationRequest.AccountDepositData accountData1 = new SaveReconciliationRequest.AccountDepositData();
        accountData1.setAccountId(account1.getId());
        List<SaveReconciliationRequest.DepositData> deposits1 = new ArrayList<>();
        SaveReconciliationRequest.DepositData deposit1 = new SaveReconciliationRequest.DepositData();
        deposit1.setDepositType("定期存款");
        deposit1.setDepositTime(date);
        deposit1.setAmount(new BigDecimal("60000.00"));
        deposit1.setInterestRate(new BigDecimal("3.5"));
        deposit1.setTerm(12);
        deposits1.add(deposit1);
        accountData1.setDeposits(deposits1);
        accountDataList.add(accountData1);
        
        // 账户2的存款记录
        SaveReconciliationRequest.AccountDepositData accountData2 = new SaveReconciliationRequest.AccountDepositData();
        accountData2.setAccountId(account2.getId());
        List<SaveReconciliationRequest.DepositData> deposits2 = new ArrayList<>();
        SaveReconciliationRequest.DepositData deposit2 = new SaveReconciliationRequest.DepositData();
        deposit2.setDepositType("活期存款");
        deposit2.setDepositTime(date);
        deposit2.setAmount(new BigDecimal("40000.00"));
        deposits2.add(deposit2);
        accountData2.setDeposits(deposits2);
        accountDataList.add(accountData2);
        
        request.setAccounts(accountDataList);
        
        // When: 保存对账快照
        reconciliationService.saveReconciliation(userId, date, request);
        
        // Then: 验证快照和存款记录已保存
        ReconciliationSnapshot savedSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(userId, date)
                .orElse(null);
        assertNotNull(savedSnapshot);
        assertEquals(new BigDecimal("100000.00"), savedSnapshot.getTotalAmount());
        assertEquals(note, savedSnapshot.getNote());
        
        List<Deposit> savedDeposits = depositRepository.findByUserIdAndReconciliationDate(userId, date);
        assertEquals(2, savedDeposits.size());
        
        System.out.println("✓ UC-RECON-003: 保存对账快照成功 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-004: 更新快照备注成功")
    public void testUpdateSnapshotNote_Success() {
        // Given: 创建一个快照
        LocalDate date = LocalDate.of(2024, 3, 1);
        ReconciliationSnapshot snapshot = new ReconciliationSnapshot();
        snapshot.setUserId(userId);
        snapshot.setReconciliationDate(date);
        snapshot.setTotalAmount(new BigDecimal("100000.00"));
        snapshot.setNote("原始备注");
        snapshotRepository.save(snapshot);
        
        // When: 更新备注
        String newNote = "更新后的备注";
        reconciliationService.updateSnapshotNote(userId, date, newNote);
        
        // Then: 验证备注已更新
        ReconciliationSnapshot updatedSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(userId, date)
                .orElse(null);
        assertNotNull(updatedSnapshot);
        assertEquals(newNote, updatedSnapshot.getNote());
        
        System.out.println("✓ UC-RECON-004: 更新快照备注成功 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-005: 更新不存在的快照备注应该失败")
    public void testUpdateSnapshotNote_NotFound() {
        // Given: 选择一个没有快照的日期
        LocalDate date = LocalDate.of(2024, 3, 2);
        
        // When & Then: 应该抛出异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reconciliationService.updateSnapshotNote(userId, date, "新备注");
        });
        
        assertEquals("该日期的对账快照不存在", exception.getMessage());
        
        System.out.println("✓ UC-RECON-005: 更新不存在的快照备注应该失败 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-006: 获取最近一次对账日期")
    public void testGetLatestReconciliationDate() {
        // Given: 创建多个快照（不同日期）
        LocalDate date1 = LocalDate.of(2024, 1, 15);
        LocalDate date2 = LocalDate.of(2024, 2, 15);
        LocalDate date3 = LocalDate.of(2024, 3, 15);
        
        createSnapshot(userId, date1, new BigDecimal("100000.00"));
        createSnapshot(userId, date2, new BigDecimal("110000.00"));
        createSnapshot(userId, date3, new BigDecimal("120000.00"));
        
        // When: 获取最近一次对账日期
        LocalDate latestDate = reconciliationService.getLatestReconciliationDate(userId);
        
        // Then: 验证结果
        assertNotNull(latestDate);
        assertEquals(date3, latestDate); // 应该是最新的日期
        
        System.out.println("✓ UC-RECON-006: 获取最近一次对账日期 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-007: 获取快照日期列表")
    public void testGetSnapshotDates() {
        // Given: 创建多个快照
        LocalDate date1 = LocalDate.of(2024, 1, 15);
        LocalDate date2 = LocalDate.of(2024, 2, 15);
        LocalDate date3 = LocalDate.of(2024, 3, 15);
        
        createSnapshot(userId, date1, new BigDecimal("100000.00"));
        createSnapshot(userId, date2, new BigDecimal("110000.00"));
        createSnapshot(userId, date3, new BigDecimal("120000.00"));
        
        // When: 获取快照日期列表
        List<LocalDate> dates = reconciliationService.getSnapshotDates(userId);
        
        // Then: 验证结果（应该按日期倒序）
        assertNotNull(dates);
        assertEquals(3, dates.size());
        assertEquals(date3, dates.get(0)); // 最新的在前
        assertEquals(date2, dates.get(1));
        assertEquals(date1, dates.get(2)); // 最早的在后
        
        System.out.println("✓ UC-RECON-007: 获取快照日期列表 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-008: 获取上一个快照日期（当前日期是快照日期）")
    public void testGetPreviousSnapshotDate_CurrentIsSnapshot() {
        // Given: 创建多个快照
        LocalDate date1 = LocalDate.of(2024, 1, 15);
        LocalDate date2 = LocalDate.of(2024, 2, 15);
        LocalDate date3 = LocalDate.of(2024, 3, 15);
        
        createSnapshot(userId, date1, new BigDecimal("100000.00"));
        createSnapshot(userId, date2, new BigDecimal("110000.00"));
        createSnapshot(userId, date3, new BigDecimal("120000.00"));
        
        // When: 获取date2的上一个快照日期
        LocalDate previousDate = reconciliationService.getPreviousSnapshotDate(userId, date2);
        
        // Then: 验证结果
        assertNotNull(previousDate);
        assertEquals(date1, previousDate); // 应该是更早的快照
        
        System.out.println("✓ UC-RECON-008: 获取上一个快照日期（当前日期是快照日期） - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-009: 获取下一个快照日期（当前日期是快照日期）")
    public void testGetNextSnapshotDate_CurrentIsSnapshot() {
        // Given: 创建多个快照
        LocalDate date1 = LocalDate.of(2024, 1, 15);
        LocalDate date2 = LocalDate.of(2024, 2, 15);
        LocalDate date3 = LocalDate.of(2024, 3, 15);
        
        createSnapshot(userId, date1, new BigDecimal("100000.00"));
        createSnapshot(userId, date2, new BigDecimal("110000.00"));
        createSnapshot(userId, date3, new BigDecimal("120000.00"));
        
        // When: 获取date2的下一个快照日期
        LocalDate nextDate = reconciliationService.getNextSnapshotDate(userId, date2);
        
        // Then: 验证结果
        assertNotNull(nextDate);
        assertEquals(date3, nextDate); // 应该是更新的快照
        
        System.out.println("✓ UC-RECON-009: 获取下一个快照日期（当前日期是快照日期） - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-010: 新建对账（复制前一次快照）")
    public void testCreateNewReconciliation_WithPreviousSnapshot() {
        // Given: 创建一个历史快照和存款记录
        LocalDate previousDate = LocalDate.of(2024, 4, 1);
        BigDecimal previousDepositAmount = new BigDecimal("50000.00");
        
        ReconciliationSnapshot previousSnapshot = new ReconciliationSnapshot();
        previousSnapshot.setUserId(userId);
        previousSnapshot.setReconciliationDate(previousDate);
        previousSnapshot.setTotalAmount(new BigDecimal("100000.00")); // 快照总金额（用于测试，但复制时会重新计算）
        snapshotRepository.save(previousSnapshot);
        
        Deposit previousDeposit = new Deposit();
        previousDeposit.setUserId(userId);
        previousDeposit.setAccountId(account1.getId());
        previousDeposit.setDepositType("定期存款");
        previousDeposit.setDepositTime(previousDate);
        previousDeposit.setAmount(previousDepositAmount);
        previousDeposit.setReconciliationDate(previousDate);
        depositRepository.save(previousDeposit);
        
        // 目标日期
        LocalDate targetDate = LocalDate.of(2024, 5, 1);
        
        // When: 新建对账
        reconciliationService.createNewReconciliation(userId, targetDate);
        
        // Then: 验证新快照已创建，且数据已复制
        // 注意：新快照的总金额是根据复制的存款记录累加的，而不是原快照的总金额
        ReconciliationSnapshot newSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(userId, targetDate)
                .orElse(null);
        assertNotNull(newSnapshot);
        assertEquals(previousDepositAmount, newSnapshot.getTotalAmount()); // 应该是复制的存款记录的总金额
        assertNull(newSnapshot.getNote()); // 备注应该清空
        
        List<Deposit> newDeposits = depositRepository.findByUserIdAndReconciliationDate(userId, targetDate);
        assertEquals(1, newDeposits.size());
        assertEquals(previousDepositAmount, newDeposits.get(0).getAmount());
        assertEquals(targetDate, newDeposits.get(0).getReconciliationDate());
        
        System.out.println("✓ UC-RECON-010: 新建对账（复制前一次快照） - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-011: 新建对账（没有前一次快照，创建空快照）")
    public void testCreateNewReconciliation_NoPreviousSnapshot() {
        // Given: 没有历史快照，目标日期
        LocalDate targetDate = LocalDate.of(2024, 6, 1);
        
        // When: 新建对账
        reconciliationService.createNewReconciliation(userId, targetDate);
        
        // Then: 验证空快照已创建
        ReconciliationSnapshot newSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(userId, targetDate)
                .orElse(null);
        assertNotNull(newSnapshot);
        assertEquals(BigDecimal.ZERO, newSnapshot.getTotalAmount());
        assertNull(newSnapshot.getNote());
        
        List<Deposit> newDeposits = depositRepository.findByUserIdAndReconciliationDate(userId, targetDate);
        assertTrue(newDeposits.isEmpty()); // 没有存款记录
        
        System.out.println("✓ UC-RECON-011: 新建对账（没有前一次快照，创建空快照） - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-012: 新建对账失败（目标日期已有快照）")
    public void testCreateNewReconciliation_AlreadyExists() {
        // Given: 目标日期已有快照
        LocalDate targetDate = LocalDate.of(2024, 7, 1);
        createSnapshot(userId, targetDate, new BigDecimal("100000.00"));
        
        // When & Then: 应该抛出异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reconciliationService.createNewReconciliation(userId, targetDate);
        });
        
        assertEquals("该日期的对账快照已存在，无法重复创建", exception.getMessage());
        
        System.out.println("✓ UC-RECON-012: 新建对账失败（目标日期已有快照） - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-013: 获取对账历史记录")
    public void testGetReconciliationHistory() {
        // Given: 创建多个快照和存款记录
        LocalDate date1 = LocalDate.of(2024, 8, 1);
        LocalDate date2 = LocalDate.of(2024, 8, 15);
        
        createSnapshot(userId, date1, new BigDecimal("100000.00"));
        createSnapshot(userId, date2, new BigDecimal("110000.00"));
        
        // 为date1创建2条存款记录
        createDeposit(userId, account1.getId(), date1, new BigDecimal("50000.00"));
        createDeposit(userId, account2.getId(), date1, new BigDecimal("50000.00"));
        
        // 为date2创建1条存款记录
        createDeposit(userId, account1.getId(), date2, new BigDecimal("110000.00"));
        
        // When: 获取对账历史记录
        ReconciliationHistoryResponse history = reconciliationService.getReconciliationHistory(userId);
        
        // Then: 验证结果
        assertNotNull(history);
        assertNotNull(history.getDates());
        assertEquals(2, history.getDates().size());
        
        // 验证第一条记录（date2，最新的在前）
        assertEquals(date2, history.getDates().get(0).getDate());
        assertEquals(1, history.getDates().get(0).getRecordCount());
        
        // 验证第二条记录（date1）
        assertEquals(date1, history.getDates().get(1).getDate());
        assertEquals(2, history.getDates().get(1).getRecordCount());
        
        System.out.println("✓ UC-RECON-013: 获取对账历史记录 - 通过");
    }
    
    // 辅助方法：创建快照
    private void createSnapshot(Long userId, LocalDate date, BigDecimal totalAmount) {
        ReconciliationSnapshot snapshot = new ReconciliationSnapshot();
        snapshot.setUserId(userId);
        snapshot.setReconciliationDate(date);
        snapshot.setTotalAmount(totalAmount);
        snapshotRepository.save(snapshot);
    }
    
    // 辅助方法：创建存款记录
    private void createDeposit(Long userId, Long accountId, LocalDate date, BigDecimal amount) {
        Deposit deposit = new Deposit();
        deposit.setUserId(userId);
        deposit.setAccountId(accountId);
        deposit.setDepositType("定期存款");
        deposit.setDepositTime(date);
        deposit.setAmount(amount);
        deposit.setReconciliationDate(date);
        depositRepository.save(deposit);
    }
}
