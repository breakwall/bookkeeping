package com.bookkeeping.controller;

import com.bookkeeping.AbstractBaseTest;
import com.bookkeeping.dto.RegisterRequest;
import com.bookkeeping.entity.Account;
import com.bookkeeping.entity.Deposit;
import com.bookkeeping.entity.ReconciliationSnapshot;
import com.bookkeeping.repository.AccountRepository;
import com.bookkeeping.repository.DepositRepository;
import com.bookkeeping.repository.ReconciliationSnapshotRepository;
import com.bookkeeping.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StatisticsController 集成测试
 * 
 * 测试覆盖：
 * - GET /api/statistics/monthly - 月度统计
 * - GET /api/statistics/trend - 趋势统计（最近一年、最近半年、全部）
 * - GET /api/statistics/yearly - 年度统计
 * - JWT认证验证
 */
@DisplayName("StatisticsController 集成测试")
@AutoConfigureMockMvc
public class StatisticsControllerTest extends AbstractBaseTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private DepositRepository depositRepository;
    
    @Autowired
    private ReconciliationSnapshotRepository snapshotRepository;
    
    private String validToken;
    private Long testUserId;
    private Account account1;
    private Account account2;
    
    @BeforeEach
    public void setUp() {
        // 创建测试用户并获取Token
        RegisterRequest registerRequest = new RegisterRequest();
        String shortUsername = "u" + System.currentTimeMillis() % 1000000000L;
        String testUsername = shortUsername.length() > 20 ? shortUsername.substring(0, 20) : shortUsername;
        registerRequest.setUsername(testUsername);
        registerRequest.setPassword("testPassword123");
        registerRequest.setEmail(generateUniqueEmail());
        com.bookkeeping.dto.AuthResponse authResponse = userService.register(registerRequest);
        testUserId = authResponse.getId();
        validToken = authResponse.getToken();
        
        // 创建测试账户
        account1 = new Account();
        account1.setUserId(testUserId);
        account1.setName("测试账户1");
        account1.setType("定期存款");
        account1.setStatus(Account.AccountStatus.ACTIVE);
        account1 = accountRepository.save(account1);
        
        account2 = new Account();
        account2.setUserId(testUserId);
        account2.setName("测试账户2");
        account2.setType("活期存款");
        account2.setStatus(Account.AccountStatus.ACTIVE);
        account2 = accountRepository.save(account2);
        
        // 创建2023年和2024年的快照和存款记录
        createMonthlySnapshots(2023);
        createMonthlySnapshots(2024);
    }
    
    /**
     * 为指定年份创建月度快照和存款记录（1-3月）
     */
    private void createMonthlySnapshots(int year) {
        for (int month = 1; month <= 3; month++) {
            LocalDate date = LocalDate.of(year, month, 15);
            BigDecimal totalAmount = new BigDecimal("100000.00").multiply(BigDecimal.valueOf(month));
            
            // 创建快照
            ReconciliationSnapshot snapshot = new ReconciliationSnapshot();
            snapshot.setUserId(testUserId);
            snapshot.setReconciliationDate(date);
            snapshot.setTotalAmount(totalAmount);
            if (month == 2) { // 2月有备注
                snapshot.setNote(year + "年" + month + "月备注");
            }
            snapshotRepository.save(snapshot);
            
            // 创建存款记录
            Deposit deposit = new Deposit();
            deposit.setUserId(testUserId);
            deposit.setAccountId(account1.getId());
            deposit.setDepositType("定期");
            deposit.setDepositTime(date);
            deposit.setAmount(totalAmount);
            deposit.setReconciliationDate(date);
            depositRepository.save(deposit);
        }
    }
    
    @Test
    @DisplayName("UC-STAT-CTRL-001: GET /api/statistics/monthly 获取月度统计成功")
    public void testGetMonthlyStatistics_Success() throws Exception {
        // When & Then: 发送获取月度统计请求
        mockMvc.perform(get("/api/statistics/monthly")
                .param("month", "2024-01")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.month").value("2024-01"))
                .andExpect(jsonPath("$.data.totalAmount").exists())
                .andExpect(jsonPath("$.data.distribution").isArray());
        
        System.out.println("✓ UC-STAT-CTRL-001: GET /api/statistics/monthly 获取月度统计成功 - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-CTRL-002: GET /api/statistics/monthly 参数验证失败（月份格式错误）")
    public void testGetMonthlyStatistics_InvalidMonth() throws Exception {
        // When & Then: 发送无效的月份格式请求
        // Service层会尝试解析月份，如果格式无效会抛出异常，Controller层返回400
        mockMvc.perform(get("/api/statistics/monthly")
                .param("month", "invalid-month")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest()) // 月份格式无效，返回400
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        
        System.out.println("✓ UC-STAT-CTRL-002: GET /api/statistics/monthly 参数验证失败（月份格式错误） - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-CTRL-003: GET /api/statistics/trend 获取趋势统计（全部）")
    public void testGetTrendStatistics_All() throws Exception {
        // When & Then: 发送获取趋势统计请求（全部）
        // 注意：StatisticsService会返回从最早快照月份到当前月份的所有月份，包括没有数据的月份
        mockMvc.perform(get("/api/statistics/trend")
                .param("period", "all")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.period").value("all"))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data.length()").exists()) // 验证数组存在且有长度
                .andExpect(jsonPath("$.data.data[?(@.month == '2024-01')]").exists()) // 验证包含我们创建的数据
                .andExpect(jsonPath("$.data.data[?(@.month == '2024-02')]").exists())
                .andExpect(jsonPath("$.data.data[?(@.month == '2024-03')]").exists());
        
        System.out.println("✓ UC-STAT-CTRL-003: GET /api/statistics/trend 获取趋势统计（全部） - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-CTRL-004: GET /api/statistics/trend 获取趋势统计（最近一年）")
    public void testGetTrendStatistics_OneYear() throws Exception {
        // When & Then: 发送获取趋势统计请求（最近一年）
        mockMvc.perform(get("/api/statistics/trend")
                .param("period", "1y")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.period").value("1y"))
                .andExpect(jsonPath("$.data.data").isArray());
        
        System.out.println("✓ UC-STAT-CTRL-004: GET /api/statistics/trend 获取趋势统计（最近一年） - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-CTRL-005: GET /api/statistics/trend 获取趋势统计（最近半年）")
    public void testGetTrendStatistics_HalfYear() throws Exception {
        // When & Then: 发送获取趋势统计请求（最近半年）
        mockMvc.perform(get("/api/statistics/trend")
                .param("period", "6m")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.period").value("6m"))
                .andExpect(jsonPath("$.data.data").isArray());
        
        System.out.println("✓ UC-STAT-CTRL-005: GET /api/statistics/trend 获取趋势统计（最近半年） - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-CTRL-006: GET /api/statistics/yearly 获取年度统计成功")
    public void testGetYearlyStatistics_Success() throws Exception {
        // When & Then: 发送获取年度统计请求
        mockMvc.perform(get("/api/statistics/yearly")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data.length()").value(2)); // 2023年和2024年，共2年
        
        System.out.println("✓ UC-STAT-CTRL-006: GET /api/statistics/yearly 获取年度统计成功 - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-CTRL-007: GET /api/statistics/monthly 未授权访问")
    public void testGetMonthlyStatistics_Unauthorized() throws Exception {
        // When & Then: 发送请求但不提供Token
        mockMvc.perform(get("/api/statistics/monthly")
                .param("month", "2024-01"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.message").value("未授权，请先登录"));
        
        System.out.println("✓ UC-STAT-CTRL-007: GET /api/statistics/monthly 未授权访问 - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-CTRL-008: GET /api/statistics/trend 未授权访问")
    public void testGetTrendStatistics_Unauthorized() throws Exception {
        // When & Then: 发送请求但不提供Token
        mockMvc.perform(get("/api/statistics/trend")
                .param("period", "all"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.message").value("未授权，请先登录"));
        
        System.out.println("✓ UC-STAT-CTRL-008: GET /api/statistics/trend 未授权访问 - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-CTRL-009: GET /api/statistics/yearly 未授权访问")
    public void testGetYearlyStatistics_Unauthorized() throws Exception {
        // When & Then: 发送请求但不提供Token
        mockMvc.perform(get("/api/statistics/yearly"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.message").value("未授权，请先登录"));
        
        System.out.println("✓ UC-STAT-CTRL-009: GET /api/statistics/yearly 未授权访问 - 通过");
    }
    
    @Test
    @DisplayName("UC-STAT-CTRL-010: GET /api/statistics/monthly 获取无数据的月度统计")
    public void testGetMonthlyStatistics_NoData() throws Exception {
        // When & Then: 获取一个没有数据的月份统计
        // 注意：StatisticsService会使用最后一次快照的数据来填充没有数据的月份
        // 所以如果2025-01没有数据，会使用最近一次快照（2024-03）的数据
        mockMvc.perform(get("/api/statistics/monthly")
                .param("month", "2025-01") // 2025年还没有数据
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.month").value("2025-01"))
                .andExpect(jsonPath("$.data.totalAmount").exists()) // 使用最近一次快照的数据，不是0
                .andExpect(jsonPath("$.data.distribution").isArray());
        
        System.out.println("✓ UC-STAT-CTRL-010: GET /api/statistics/monthly 获取无数据的月度统计 - 通过");
    }
}
