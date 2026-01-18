package com.bookkeeping.service;

import com.bookkeeping.AbstractBaseTest;
import com.bookkeeping.dto.CreateDepositRequest;
import com.bookkeeping.dto.RegisterRequest;
import com.bookkeeping.dto.UpdateDepositRequest;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DepositService 单元测试
 * 
 * 测试覆盖：
 * - 创建存款记录（同步更新快照总金额）
 * - 更新存款记录（同步更新快照总金额）
 * - 删除存款记录（同步更新快照总金额）
 * - 获取账户存款记录
 */
@DisplayName("DepositService 单元测试")
public class DepositServiceTest extends AbstractBaseTest {
    
    @Autowired
    private DepositService depositService;
    
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
    private LocalDate reconciliationDate;
    
    @BeforeEach
    public void setUp() {
        // 创建测试用户
        RegisterRequest registerRequest = new RegisterRequest();
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
        
        reconciliationDate = LocalDate.of(2024, 1, 15);
    }
    
    @Test
    @DisplayName("UC-DEPOSIT-001: 创建存款记录后，快照总金额同步更新")
    public void testCreateDeposit_SyncSnapshotTotalAmount() {
        // Given: 创建一个快照
        ReconciliationSnapshot snapshot = new ReconciliationSnapshot();
        snapshot.setUserId(userId);
        snapshot.setReconciliationDate(reconciliationDate);
        snapshot.setTotalAmount(new BigDecimal("0.00")); // 初始总金额为0
        snapshot.setNote("测试备注");
        snapshotRepository.save(snapshot);
        
        // When: 创建存款记录
        CreateDepositRequest request = new CreateDepositRequest();
        request.setAccountId(account1.getId());
        request.setDepositType("定期");
        request.setDepositTime(reconciliationDate);
        request.setAmount(new BigDecimal("50000.00"));
        request.setReconciliationDate(reconciliationDate);
        
        depositService.createDeposit(request, userId);
        
        // Then: 验证快照总金额已同步更新
        ReconciliationSnapshot updatedSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(userId, reconciliationDate)
                .orElseThrow();
        assertEquals(new BigDecimal("50000.00"), updatedSnapshot.getTotalAmount());
        
        System.out.println("✓ UC-DEPOSIT-001: 创建存款记录后，快照总金额同步更新 - 通过");
    }
    
    @Test
    @DisplayName("UC-DEPOSIT-002: 更新存款记录金额后，快照总金额同步更新")
    public void testUpdateDeposit_SyncSnapshotTotalAmount() {
        // Given: 创建一个快照和存款记录
        ReconciliationSnapshot snapshot = new ReconciliationSnapshot();
        snapshot.setUserId(userId);
        snapshot.setReconciliationDate(reconciliationDate);
        snapshot.setTotalAmount(new BigDecimal("100000.00"));
        snapshot.setNote("测试备注");
        snapshotRepository.save(snapshot);
        
        Deposit deposit1 = new Deposit();
        deposit1.setUserId(userId);
        deposit1.setAccountId(account1.getId());
        deposit1.setDepositType("定期");
        deposit1.setDepositTime(reconciliationDate);
        deposit1.setAmount(new BigDecimal("60000.00"));
        deposit1.setReconciliationDate(reconciliationDate);
        deposit1 = depositRepository.save(deposit1);
        
        Deposit deposit2 = new Deposit();
        deposit2.setUserId(userId);
        deposit2.setAccountId(account2.getId());
        deposit2.setDepositType("活期");
        deposit2.setDepositTime(reconciliationDate);
        deposit2.setAmount(new BigDecimal("40000.00"));
        deposit2.setReconciliationDate(reconciliationDate);
        depositRepository.save(deposit2);
        
        // 初始总金额应该是 100000.00
        ReconciliationSnapshot initialSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(userId, reconciliationDate)
                .orElseThrow();
        assertEquals(new BigDecimal("100000.00"), initialSnapshot.getTotalAmount());
        
        // When: 更新第一个存款记录的金额
        UpdateDepositRequest updateRequest = new UpdateDepositRequest();
        updateRequest.setDepositType("定期");
        updateRequest.setDepositTime(reconciliationDate);
        updateRequest.setAmount(new BigDecimal("80000.00")); // 从 60000 增加到 80000
        
        depositService.updateDeposit(deposit1.getId(), updateRequest, userId);
        
        // Then: 验证快照总金额已同步更新为 120000.00 (80000 + 40000)
        ReconciliationSnapshot updatedSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(userId, reconciliationDate)
                .orElseThrow();
        assertEquals(new BigDecimal("120000.00"), updatedSnapshot.getTotalAmount());
        
        // 验证存款记录已更新
        Deposit updatedDeposit = depositRepository.findById(deposit1.getId()).orElseThrow();
        assertEquals(new BigDecimal("80000.00"), updatedDeposit.getAmount());
        
        System.out.println("✓ UC-DEPOSIT-002: 更新存款记录金额后，快照总金额同步更新 - 通过");
    }
    
    @Test
    @DisplayName("UC-DEPOSIT-003: 删除存款记录后，快照总金额同步更新")
    public void testDeleteDeposit_SyncSnapshotTotalAmount() {
        // Given: 创建一个快照和两个存款记录
        ReconciliationSnapshot snapshot = new ReconciliationSnapshot();
        snapshot.setUserId(userId);
        snapshot.setReconciliationDate(reconciliationDate);
        snapshot.setTotalAmount(new BigDecimal("100000.00"));
        snapshot.setNote("测试备注");
        snapshotRepository.save(snapshot);
        
        Deposit deposit1 = new Deposit();
        deposit1.setUserId(userId);
        deposit1.setAccountId(account1.getId());
        deposit1.setDepositType("定期");
        deposit1.setDepositTime(reconciliationDate);
        deposit1.setAmount(new BigDecimal("60000.00"));
        deposit1.setReconciliationDate(reconciliationDate);
        deposit1 = depositRepository.save(deposit1);
        
        Deposit deposit2 = new Deposit();
        deposit2.setUserId(userId);
        deposit2.setAccountId(account2.getId());
        deposit2.setDepositType("活期");
        deposit2.setDepositTime(reconciliationDate);
        deposit2.setAmount(new BigDecimal("40000.00"));
        deposit2.setReconciliationDate(reconciliationDate);
        deposit2 = depositRepository.save(deposit2);
        
        // 初始总金额应该是 100000.00
        ReconciliationSnapshot initialSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(userId, reconciliationDate)
                .orElseThrow();
        assertEquals(new BigDecimal("100000.00"), initialSnapshot.getTotalAmount());
        
        // When: 删除第一个存款记录
        depositService.deleteDeposit(deposit1.getId(), userId);
        
        // Then: 验证快照总金额已同步更新为 40000.00（只剩第二个存款记录）
        ReconciliationSnapshot updatedSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(userId, reconciliationDate)
                .orElseThrow();
        assertEquals(new BigDecimal("40000.00"), updatedSnapshot.getTotalAmount());
        
        // 验证存款记录已删除
        Optional<Deposit> deletedDeposit = depositRepository.findById(deposit1.getId());
        assertFalse(deletedDeposit.isPresent());
        
        // 验证第二个存款记录仍然存在
        assertTrue(depositRepository.findById(deposit2.getId()).isPresent());
        
        System.out.println("✓ UC-DEPOSIT-003: 删除存款记录后，快照总金额同步更新 - 通过");
    }
    
    @Test
    @DisplayName("UC-DEPOSIT-004: 创建多个存款记录，快照总金额正确累加")
    public void testCreateMultipleDeposits_SnapshotTotalAmountAccumulates() {
        // Given: 创建一个快照
        ReconciliationSnapshot snapshot = new ReconciliationSnapshot();
        snapshot.setUserId(userId);
        snapshot.setReconciliationDate(reconciliationDate);
        snapshot.setTotalAmount(new BigDecimal("0.00"));
        snapshot.setNote("测试备注");
        snapshotRepository.save(snapshot);
        
        // When: 创建多个存款记录
        CreateDepositRequest request1 = new CreateDepositRequest();
        request1.setAccountId(account1.getId());
        request1.setDepositType("定期");
        request1.setDepositTime(reconciliationDate);
        request1.setAmount(new BigDecimal("50000.00"));
        request1.setReconciliationDate(reconciliationDate);
        depositService.createDeposit(request1, userId);
        
        CreateDepositRequest request2 = new CreateDepositRequest();
        request2.setAccountId(account2.getId());
        request2.setDepositType("活期");
        request2.setDepositTime(reconciliationDate);
        request2.setAmount(new BigDecimal("30000.00"));
        request2.setReconciliationDate(reconciliationDate);
        depositService.createDeposit(request2, userId);
        
        // Then: 验证快照总金额是累加后的值
        ReconciliationSnapshot updatedSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(userId, reconciliationDate)
                .orElseThrow();
        assertEquals(new BigDecimal("80000.00"), updatedSnapshot.getTotalAmount()); // 50000 + 30000
        
        System.out.println("✓ UC-DEPOSIT-004: 创建多个存款记录，快照总金额正确累加 - 通过");
    }
    
    @Test
    @DisplayName("UC-DEPOSIT-005: 更新存款记录后，如果日期无快照，不更新快照")
    public void testUpdateDeposit_NoSnapshotExists() {
        // Given: 创建一个存款记录，但没有快照
        Deposit deposit = new Deposit();
        deposit.setUserId(userId);
        deposit.setAccountId(account1.getId());
        deposit.setDepositType("定期");
        deposit.setDepositTime(reconciliationDate);
        deposit.setAmount(new BigDecimal("50000.00"));
        deposit.setReconciliationDate(reconciliationDate);
        deposit = depositRepository.save(deposit);
        
        // 确认没有快照
        assertFalse(snapshotRepository.existsByUserIdAndReconciliationDate(userId, reconciliationDate));
        
        // When: 更新存款记录
        UpdateDepositRequest updateRequest = new UpdateDepositRequest();
        updateRequest.setDepositType("定期");
        updateRequest.setDepositTime(reconciliationDate);
        updateRequest.setAmount(new BigDecimal("80000.00"));
        
        depositService.updateDeposit(deposit.getId(), updateRequest, userId);
        
        // Then: 验证存款记录已更新，但仍然没有快照（不会创建快照）
        Deposit updatedDeposit = depositRepository.findById(deposit.getId()).orElseThrow();
        assertEquals(new BigDecimal("80000.00"), updatedDeposit.getAmount());
        
        assertFalse(snapshotRepository.existsByUserIdAndReconciliationDate(userId, reconciliationDate));
        
        System.out.println("✓ UC-DEPOSIT-005: 更新存款记录后，如果日期无快照，不更新快照 - 通过");
    }
    
    @Test
    @DisplayName("UC-DEPOSIT-006: 删除最后一个存款记录后，快照总金额变为0")
    public void testDeleteLastDeposit_SnapshotTotalAmountBecomesZero() {
        // Given: 创建一个快照和一个存款记录
        ReconciliationSnapshot snapshot = new ReconciliationSnapshot();
        snapshot.setUserId(userId);
        snapshot.setReconciliationDate(reconciliationDate);
        snapshot.setTotalAmount(new BigDecimal("50000.00"));
        snapshot.setNote("测试备注");
        snapshotRepository.save(snapshot);
        
        Deposit deposit = new Deposit();
        deposit.setUserId(userId);
        deposit.setAccountId(account1.getId());
        deposit.setDepositType("定期");
        deposit.setDepositTime(reconciliationDate);
        deposit.setAmount(new BigDecimal("50000.00"));
        deposit.setReconciliationDate(reconciliationDate);
        deposit = depositRepository.save(deposit);
        
        // When: 删除唯一的存款记录
        depositService.deleteDeposit(deposit.getId(), userId);
        
        // Then: 验证快照总金额变为0
        ReconciliationSnapshot updatedSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(userId, reconciliationDate)
                .orElseThrow();
        assertEquals(BigDecimal.ZERO, updatedSnapshot.getTotalAmount());
        
        // 验证存款记录已删除
        assertFalse(depositRepository.findById(deposit.getId()).isPresent());
        
        System.out.println("✓ UC-DEPOSIT-006: 删除最后一个存款记录后，快照总金额变为0 - 通过");
    }
    
    @Test
    @DisplayName("UC-DEPOSIT-007: 获取账户存款记录")
    public void testGetDepositsByAccount() {
        // Given: 创建存款记录
        Deposit deposit1 = new Deposit();
        deposit1.setUserId(userId);
        deposit1.setAccountId(account1.getId());
        deposit1.setDepositType("定期");
        deposit1.setDepositTime(reconciliationDate);
        deposit1.setAmount(new BigDecimal("50000.00"));
        deposit1.setReconciliationDate(reconciliationDate);
        depositRepository.save(deposit1);
        
        Deposit deposit2 = new Deposit();
        deposit2.setUserId(userId);
        deposit2.setAccountId(account1.getId()); // 同一个账户
        deposit2.setDepositType("定期");
        deposit2.setDepositTime(reconciliationDate);
        deposit2.setAmount(new BigDecimal("30000.00"));
        deposit2.setReconciliationDate(reconciliationDate);
        depositRepository.save(deposit2);
        
        Deposit deposit3 = new Deposit();
        deposit3.setUserId(userId);
        deposit3.setAccountId(account2.getId()); // 不同的账户
        deposit3.setDepositType("活期");
        deposit3.setDepositTime(reconciliationDate);
        deposit3.setAmount(new BigDecimal("20000.00"));
        deposit3.setReconciliationDate(reconciliationDate);
        depositRepository.save(deposit3);
        
        // When: 获取账户1的存款记录
        var deposits = depositService.getDepositsByAccount(account1.getId(), userId, reconciliationDate);
        
        // Then: 验证返回的存款记录
        assertEquals(2, deposits.size());
        assertEquals(account1.getId(), deposits.get(0).getAccountId());
        assertEquals(account1.getId(), deposits.get(1).getAccountId());
        
        System.out.println("✓ UC-DEPOSIT-007: 获取账户存款记录 - 通过");
    }
}
