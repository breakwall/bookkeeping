package com.bookkeeping.service;

import com.bookkeeping.AbstractBaseTest;
import com.bookkeeping.dto.MonthlyStatisticsResponse;
import com.bookkeeping.dto.TrendStatisticsResponse;
import com.bookkeeping.dto.YearlyStatisticsResponse;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * StatisticsService 单元测试
 * 
 * 测试覆盖：
 * - 月度统计（有记录/无记录）
 * - 趋势统计（不同时间范围：6m、1y、3y、all）
 * - 年度统计（第一年、后续年份、跨年、空年份）
 */
@DisplayName("StatisticsService 单元测试")
public class StatisticsServiceTest extends AbstractBaseTest {
    
    @Autowired
    private StatisticsService statisticsService;
    
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
    @DisplayName("UC-STAT-001: 月度统计（有记录）")
    public void testGetMonthlyStatistics_WithRecords() {
        // Given: 创建2024年1月的快照和存款记录
        String month = "2024-01";
        LocalDate date = LocalDate.of(2024, 1, 31);
        BigDecimal totalAmount = new BigDecimal("100000.00");
        
        ReconciliationSnapshot snapshot = new ReconciliationSnapshot();
        snapshot.setUserId(userId);
        snapshot.setReconciliationDate(date);
        snapshot.setTotalAmount(totalAmount);
        snapshotRepository.save(snapshot);
        
        Deposit deposit1 = new Deposit();
        deposit1.setUserId(userId);
        deposit1.setAccountId(account1.getId());
        deposit1.setDepositType("定期存款");
        deposit1.setDepositTime(date);
        deposit1.setAmount(new BigDecimal("60000.00"));
        deposit1.setReconciliationDate(date);
        depositRepository.save(deposit1);
        
        Deposit deposit2 = new Deposit();
        deposit2.setUserId(userId);
        deposit2.setAccountId(account2.getId());
        deposit2.setDepositType("活期存款");
        deposit2.setDepositTime(date);
        deposit2.setAmount(new BigDecimal("40000.00"));
        deposit2.setReconciliationDate(date);
        depositRepository.save(deposit2);
        
        // When: 获取月度统计
        MonthlyStatisticsResponse response = statisticsService.getMonthlyStatistics(userId, month);
        
        // Then: 验证结果
        assertNotNull(response);
        assertEquals(month, response.getMonth());
        assertEquals(totalAmount, response.getTotalAmount());
        assertEquals(2, response.getDistribution().size());
        
        // 验证账户分布
        MonthlyStatisticsResponse.AccountDistributionItem item1 = response.getDistribution().get(0);
        assertEquals(account1.getId(), item1.getAccountId());
        assertEquals(new BigDecimal("60000.00"), item1.getAmount());
        assertEquals(60.0, item1.getPercentage(), 0.01); // 60%
        
        MonthlyStatisticsResponse.AccountDistributionItem item2 = response.getDistribution().get(1);
        assertEquals(account2.getId(), item2.getAccountId());
        assertEquals(new BigDecimal("40000.00"), item2.getAmount());
        assertEquals(40.0, item2.getPercentage(), 0.01); // 40%
        
        System.out.println("✓ UC-STAT-001: 月度统计（有记录） - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-002: 月度统计（无记录，使用前一个月的数据）")
    public void testGetMonthlyStatistics_NoRecords_UsePrevious() {
        // Given: 创建2024年1月的快照（作为前一个月）
        LocalDate previousDate = LocalDate.of(2024, 1, 31);
        BigDecimal previousAmount = new BigDecimal("100000.00");
        createSnapshot(userId, previousDate, previousAmount);
        
        // 2024年2月没有记录
        String month = "2024-02";
        
        // When: 获取月度统计
        MonthlyStatisticsResponse response = statisticsService.getMonthlyStatistics(userId, month);
        
        // Then: 验证结果（应该使用前一个月的数据）
        assertNotNull(response);
        assertEquals(month, response.getMonth());
        assertEquals(previousAmount, response.getTotalAmount());
        assertTrue(response.getDistribution().isEmpty()); // 没有当月的存款记录
        
        System.out.println("✓ UC-STAT-002: 月度统计（无记录，使用前一个月的数据） - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-003: 趋势统计（最近6个月）")
    public void testGetTrendStatistics_6Months() {
        // Given: 创建最近6个月的快照数据
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 6; i++) {
            LocalDate date = now.minusMonths(i).withDayOfMonth(15);
            BigDecimal amount = new BigDecimal("100000").add(new BigDecimal(i * 10000));
            createSnapshot(userId, date, amount);
            
            // 创建存款记录
            createDeposit(userId, account1.getId(), date, amount);
        }
        
        // When: 获取趋势统计（最近6个月）
        TrendStatisticsResponse response = statisticsService.getTrendStatistics(userId, "6m");
        
        // Then: 验证结果
        assertNotNull(response);
        assertEquals("6m", response.getPeriod());
        assertNotNull(response.getData());
        assertTrue(response.getData().size() >= 6);
        
        System.out.println("✓ UC-STAT-003: 趋势统计（最近6个月） - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-004: 趋势统计（全部时间）")
    public void testGetTrendStatistics_All() {
        // Given: 创建跨年的快照数据
        LocalDate date1 = LocalDate.of(2023, 1, 15);
        LocalDate date2 = LocalDate.of(2023, 6, 15);
        LocalDate date3 = LocalDate.of(2024, 1, 15);
        LocalDate date4 = LocalDate.of(2024, 6, 15);
        
        createSnapshot(userId, date1, new BigDecimal("100000.00"));
        createSnapshot(userId, date2, new BigDecimal("110000.00"));
        createSnapshot(userId, date3, new BigDecimal("120000.00"));
        createSnapshot(userId, date4, new BigDecimal("130000.00"));
        
        // When: 获取趋势统计（全部时间）
        TrendStatisticsResponse response = statisticsService.getTrendStatistics(userId, "all");
        
        // Then: 验证结果
        assertNotNull(response);
        assertEquals("all", response.getPeriod());
        assertNotNull(response.getData());
        assertTrue(response.getData().size() >= 18); // 至少18个月（2023年1月到2024年6月）
        
        System.out.println("✓ UC-STAT-004: 趋势统计（全部时间） - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-005: 趋势统计包含快照备注")
    public void testGetTrendStatistics_WithNotes() {
        // Given: 创建2024年1月的多个快照（部分有备注）
        LocalDate date1 = LocalDate.of(2024, 1, 15);
        LocalDate date2 = LocalDate.of(2024, 1, 20);
        LocalDate date3 = LocalDate.of(2024, 1, 31);
        
        // 第一个快照有备注
        ReconciliationSnapshot snapshot1 = new ReconciliationSnapshot();
        snapshot1.setUserId(userId);
        snapshot1.setReconciliationDate(date1);
        snapshot1.setTotalAmount(new BigDecimal("100000.00"));
        snapshot1.setNote("第一次对账");
        snapshotRepository.save(snapshot1);
        
        // 第二个快照有备注
        ReconciliationSnapshot snapshot2 = new ReconciliationSnapshot();
        snapshot2.setUserId(userId);
        snapshot2.setReconciliationDate(date2);
        snapshot2.setTotalAmount(new BigDecimal("110000.00"));
        snapshot2.setNote("第二次对账");
        snapshotRepository.save(snapshot2);
        
        // 第三个快照没有备注（最后一次）
        createSnapshot(userId, date3, new BigDecimal("120000.00"));
        
        // When: 获取趋势统计（全部时间）
        TrendStatisticsResponse response = statisticsService.getTrendStatistics(userId, "all");
        
        // Then: 验证结果（应该包含所有有备注的快照备注）
        assertNotNull(response);
        TrendStatisticsResponse.TrendDataItem januaryItem = response.getData().stream()
                .filter(item -> item.getMonth().equals("2024-01"))
                .findFirst()
                .orElse(null);
        assertNotNull(januaryItem);
        assertNotNull(januaryItem.getNotes());
        assertEquals(2, januaryItem.getNotes().size()); // 应该有2条备注
        assertTrue(januaryItem.getNotes().contains("2024-01-15: 第一次对账"));
        assertTrue(januaryItem.getNotes().contains("2024-01-20: 第二次对账"));
        
        System.out.println("✓ UC-STAT-005: 趋势统计包含快照备注 - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-006: 年度统计（第一年）")
    public void testGetYearlyStatistics_FirstYear() {
        // Given: 创建2024年的多个快照（第一年）
        LocalDate date1 = LocalDate.of(2024, 1, 15);
        LocalDate date2 = LocalDate.of(2024, 6, 15);
        LocalDate date3 = LocalDate.of(2024, 12, 31);
        
        BigDecimal amount1 = new BigDecimal("100000.00");
        BigDecimal amount2 = new BigDecimal("110000.00");
        BigDecimal amount3 = new BigDecimal("120000.00");
        
        createSnapshot(userId, date1, amount1);
        createSnapshot(userId, date2, amount2);
        createSnapshot(userId, date3, amount3);
        
        // When: 获取年度统计
        YearlyStatisticsResponse response = statisticsService.getYearlyStatistics(userId);
        
        // Then: 验证结果
        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        
        YearlyStatisticsResponse.YearlyDataItem item = response.getData().get(0);
        assertEquals("2024", item.getYear());
        // 第一年：最后一次 - 第一次 = 120000 - 100000 = 20000
        assertEquals(new BigDecimal("20000.00"), item.getIncrease());
        
        System.out.println("✓ UC-STAT-006: 年度统计（第一年） - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-007: 年度统计（跨年，第二年及以后）")
    public void testGetYearlyStatistics_MultipleYears() {
        // Given: 创建2023年和2024年的快照
        // 2023年
        LocalDate date2023First = LocalDate.of(2023, 1, 15);
        LocalDate date2023Last = LocalDate.of(2023, 12, 31);
        BigDecimal amount2023First = new BigDecimal("100000.00");
        BigDecimal amount2023Last = new BigDecimal("110000.00");
        
        createSnapshot(userId, date2023First, amount2023First);
        createSnapshot(userId, date2023Last, amount2023Last);
        
        // 2024年
        LocalDate date2024First = LocalDate.of(2024, 1, 15);
        LocalDate date2024Last = LocalDate.of(2024, 12, 31);
        BigDecimal amount2024First = new BigDecimal("120000.00");
        BigDecimal amount2024Last = new BigDecimal("130000.00");
        
        createSnapshot(userId, date2024First, amount2024First);
        createSnapshot(userId, date2024Last, amount2024Last);
        
        // When: 获取年度统计
        YearlyStatisticsResponse response = statisticsService.getYearlyStatistics(userId);
        
        // Then: 验证结果
        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        
        // 2023年（第一年）：最后一次 - 第一次 = 110000 - 100000 = 10000
        YearlyStatisticsResponse.YearlyDataItem item2023 = response.getData().get(0);
        assertEquals("2023", item2023.getYear());
        assertEquals(new BigDecimal("10000.00"), item2023.getIncrease());
        
        // 2024年（第二年）：最后一次 - 2023年最后一次 = 130000 - 110000 = 20000
        YearlyStatisticsResponse.YearlyDataItem item2024 = response.getData().get(1);
        assertEquals("2024", item2024.getYear());
        assertEquals(new BigDecimal("20000.00"), item2024.getIncrease());
        
        System.out.println("✓ UC-STAT-007: 年度统计（跨年，第二年及以后） - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-008: 年度统计（空年份）")
    public void testGetYearlyStatistics_EmptyYear() {
        // Given: 创建2023年和2025年的快照（2024年为空）
        // 2023年
        LocalDate date2023Last = LocalDate.of(2023, 12, 31);
        createSnapshot(userId, date2023Last, new BigDecimal("100000.00"));
        
        // 2025年
        LocalDate date2025Last = LocalDate.of(2025, 12, 31);
        createSnapshot(userId, date2025Last, new BigDecimal("130000.00"));
        
        // When: 获取年度统计
        YearlyStatisticsResponse response = statisticsService.getYearlyStatistics(userId);
        
        // Then: 验证结果（应该包含2024年，但增值为0）
        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals(3, response.getData().size());
        
        // 2024年应该存在，但增值为0
        YearlyStatisticsResponse.YearlyDataItem item2024 = response.getData().stream()
                .filter(item -> item.getYear().equals("2024"))
                .findFirst()
                .orElse(null);
        assertNotNull(item2024);
        assertEquals(BigDecimal.ZERO, item2024.getIncrease());
        
        // 2025年的增值应该是：130000 - 100000 = 30000（相对于2023年最后一次）
        YearlyStatisticsResponse.YearlyDataItem item2025 = response.getData().stream()
                .filter(item -> item.getYear().equals("2025"))
                .findFirst()
                .orElse(null);
        assertNotNull(item2025);
        assertEquals(new BigDecimal("30000.00"), item2025.getIncrease());
        
        System.out.println("✓ UC-STAT-008: 年度统计（空年份） - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-009: 年度统计（某年只有一次快照）")
    public void testGetYearlyStatistics_SingleSnapshotInYear() {
        // Given: 2023年有多次快照，2024年只有一次快照
        // 2023年
        LocalDate date2023First = LocalDate.of(2023, 1, 15);
        LocalDate date2023Last = LocalDate.of(2023, 12, 31);
        createSnapshot(userId, date2023First, new BigDecimal("100000.00"));
        createSnapshot(userId, date2023Last, new BigDecimal("110000.00"));
        
        // 2024年只有一次快照
        LocalDate date2024 = LocalDate.of(2024, 6, 15);
        createSnapshot(userId, date2024, new BigDecimal("120000.00"));
        
        // When: 获取年度统计
        YearlyStatisticsResponse response = statisticsService.getYearlyStatistics(userId);
        
        // Then: 验证结果
        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        
        // 2024年只有一次快照，应该使用：该年最后一次 - 上一年最后一次 = 120000 - 110000 = 10000
        YearlyStatisticsResponse.YearlyDataItem item2024 = response.getData().get(1);
        assertEquals("2024", item2024.getYear());
        assertEquals(new BigDecimal("10000.00"), item2024.getIncrease());
        
        System.out.println("✓ UC-STAT-009: 年度统计（某年只有一次快照） - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-010: 年度统计（无快照记录）")
    public void testGetYearlyStatistics_NoSnapshots() {
        // Given: 没有任何快照记录
        
        // When: 获取年度统计
        YearlyStatisticsResponse response = statisticsService.getYearlyStatistics(userId);
        
        // Then: 验证结果（应该返回空列表）
        assertNotNull(response);
        assertNotNull(response.getData());
        assertTrue(response.getData().isEmpty());
        
        System.out.println("✓ UC-STAT-010: 年度统计（无快照记录） - 通过");
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
