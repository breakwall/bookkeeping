package com.bookkeeping.service;

import com.bookkeeping.dto.MaturityStatisticsResponse;
import com.bookkeeping.entity.Account;
import com.bookkeeping.entity.Deposit;
import com.bookkeeping.entity.ReconciliationSnapshot;
import com.bookkeeping.repository.AccountRepository;
import com.bookkeeping.repository.DepositRepository;
import com.bookkeeping.repository.ReconciliationSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("到期统计服务测试")
public class MaturityStatisticsServiceTest {

    @Autowired
    private StatisticsService statisticsService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private ReconciliationSnapshotRepository snapshotRepository;
    
    @Autowired
    private DepositRepository depositRepository;

    @Test
    @DisplayName("UC-STAT-012: 存款到期统计 - 筛选一年内到期的定期存款")
    void testGetMaturityStatistics() {
        // 准备测试数据
        Long userId = 1L;
        
        // 创建账户
        Account account1 = new Account();
        account1.setUserId(userId);
        account1.setName("工商银行");
        account1.setType("储蓄卡");
        account1 = accountRepository.save(account1);
        
        Account account2 = new Account();
        account2.setUserId(userId);
        account2.setName("建设银行");
        account2.setType("储蓄卡");
        account2 = accountRepository.save(account2);
        
        // 创建最新快照
        ReconciliationSnapshot snapshot = new ReconciliationSnapshot();
        snapshot.setUserId(userId);
        snapshot.setReconciliationDate(LocalDate.now());
        snapshot.setTotalAmount(BigDecimal.valueOf(300000));
        snapshot = snapshotRepository.save(snapshot);
        
        LocalDate now = LocalDate.now();
        
        // 创建存款记录
        // 1. 30天后到期的定期存款（应该包含）
        Deposit deposit1 = new Deposit();
        deposit1.setUserId(userId);
        deposit1.setAccountId(account1.getId());
        deposit1.setDepositType("定期");
        deposit1.setAmount(BigDecimal.valueOf(100000));
        deposit1.setDepositTime(now.minusDays(335)); // 一年前存入，30天后到期
        deposit1.setTerm(BigDecimal.valueOf(1.0)); // 1年期
        deposit1.setReconciliationDate(now);
        depositRepository.save(deposit1);
        
        // 2. 200天后到期的定期存款（应该包含）
        Deposit deposit2 = new Deposit();
        deposit2.setUserId(userId);
        deposit2.setAccountId(account2.getId());
        deposit2.setDepositType("定期");
        deposit2.setAmount(BigDecimal.valueOf(50000));
        deposit2.setDepositTime(now.minusDays(165)); // 165天前存入，200天后到期
        deposit2.setTerm(BigDecimal.valueOf(1.0)); // 1年期
        deposit2.setReconciliationDate(now);
        depositRepository.save(deposit2);
        
        // 3. 活期存款（应该排除）
        Deposit deposit3 = new Deposit();
        deposit3.setUserId(userId);
        deposit3.setAccountId(account1.getId());
        deposit3.setDepositType("活期");
        deposit3.setAmount(BigDecimal.valueOf(20000));
        deposit3.setDepositTime(now.minusDays(100));
        deposit3.setTerm(BigDecimal.valueOf(0)); // 活期
        deposit3.setReconciliationDate(now);
        depositRepository.save(deposit3);
        
        // 4. 400天后到期的定期存款（超过一年，应该排除）
        Deposit deposit4 = new Deposit();
        deposit4.setUserId(userId);
        deposit4.setAccountId(account1.getId());
        deposit4.setDepositType("定期");
        deposit4.setAmount(BigDecimal.valueOf(80000));
        deposit4.setDepositTime(now.plusDays(35)); // 35天后存入，400天后到期
        deposit4.setTerm(BigDecimal.valueOf(1.0)); // 1年期
        deposit4.setReconciliationDate(now);
        depositRepository.save(deposit4);
        
        // 执行测试
        MaturityStatisticsResponse result = statisticsService.getMaturityStatistics(userId);
        
        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size(), "应该只有2个定期存款在一年内到期");
        
        // 验证数据按到期时间排序（从近到远）
        List<MaturityStatisticsResponse.MaturityDataItem> maturityData = result.getData();
        
        // 第一个应该是30天后到期的
        MaturityStatisticsResponse.MaturityDataItem item1 = maturityData.get(0);
        assertEquals("工商银行", item1.getAccountName());
        assertEquals(BigDecimal.valueOf(100000), item1.getDepositAmount());
        assertTrue(item1.getRemainingDays() <= 31 && item1.getRemainingDays() >= 29, 
                  "剩余天数应该约为30天，实际为：" + item1.getRemainingDays());
        
        // 第二个应该是200天后到期的
        MaturityStatisticsResponse.MaturityDataItem item2 = maturityData.get(1);
        assertEquals("建设银行", item2.getAccountName());
        assertEquals(BigDecimal.valueOf(50000), item2.getDepositAmount());
        assertTrue(item2.getRemainingDays() <= 201 && item2.getRemainingDays() >= 199, 
                  "剩余天数应该约为200天，实际为：" + item2.getRemainingDays());
        
        // 验证排序正确（第一个到期时间更近）
        assertTrue(item1.getRemainingDays() < item2.getRemainingDays(), 
                  "应该按照到期时间从近到远排序");
    }
    
    @Test
    @DisplayName("UC-STAT-013: 到期统计 - 无最新快照数据")
    void testGetMaturityStatisticsWithNoSnapshot() {
        Long userId = 999L; // 不存在的用户ID
        
        MaturityStatisticsResponse result = statisticsService.getMaturityStatistics(userId);
        
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(0, result.getData().size(), "没有快照时应该返回空列表");
    }
}