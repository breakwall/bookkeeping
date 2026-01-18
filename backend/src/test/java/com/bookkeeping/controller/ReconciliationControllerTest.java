package com.bookkeeping.controller;

import com.bookkeeping.AbstractBaseTest;
import com.bookkeeping.dto.RegisterRequest;
import com.bookkeeping.dto.SaveReconciliationRequest;
import com.bookkeeping.dto.UpdateSnapshotNoteRequest;
import com.bookkeeping.entity.Account;
import com.bookkeeping.entity.Deposit;
import com.bookkeeping.entity.ReconciliationSnapshot;
import com.bookkeeping.repository.AccountRepository;
import com.bookkeeping.repository.DepositRepository;
import com.bookkeeping.repository.ReconciliationSnapshotRepository;
import com.bookkeeping.service.UserService;
import com.bookkeeping.util.JwtUtil;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ReconciliationController 集成测试
 * 
 * 测试覆盖：
 * - GET /api/reconciliation - 获取对账数据（有日期/无日期、有快照/无快照）
 * - POST /api/reconciliation/save - 保存对账快照
 * - GET /api/reconciliation/latest - 获取最近一次对账日期
 * - GET /api/reconciliation/history - 获取历史对账记录
 * - GET /api/reconciliation/previous - 获取上一个快照日期
 * - GET /api/reconciliation/next - 获取下一个快照日期
 * - POST /api/reconciliation/create-new - 新建对账
 * - PUT /api/reconciliation/note - 更新快照备注
 * - GET /api/reconciliation/snapshot-dates - 获取所有快照日期列表
 * - JWT认证验证
 */
@DisplayName("ReconciliationController 集成测试")
@AutoConfigureMockMvc
public class ReconciliationControllerTest extends AbstractBaseTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
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
    private LocalDate date1;
    private LocalDate date2;
    private LocalDate date3;
    
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
        
        // 准备测试日期
        date1 = LocalDate.of(2024, 1, 15);
        date2 = LocalDate.of(2024, 2, 15);
        date3 = LocalDate.of(2024, 3, 15);
        
        // 创建测试快照和存款记录
        createTestSnapshot(date1, "备注1");
        createTestSnapshot(date2, "备注2");
        createTestSnapshot(date3, null);
    }
    
    private void createTestSnapshot(LocalDate date, String note) {
        BigDecimal totalAmount = new BigDecimal("100000.00");
        
        // 创建快照
        ReconciliationSnapshot snapshot = new ReconciliationSnapshot();
        snapshot.setUserId(testUserId);
        snapshot.setReconciliationDate(date);
        snapshot.setTotalAmount(totalAmount);
        snapshot.setNote(note);
        snapshotRepository.save(snapshot);
        
        // 创建存款记录
        Deposit deposit1 = new Deposit();
        deposit1.setUserId(testUserId);
        deposit1.setAccountId(account1.getId());
        deposit1.setDepositType("定期");
        deposit1.setDepositTime(date);
        deposit1.setAmount(new BigDecimal("60000.00"));
        deposit1.setReconciliationDate(date);
        depositRepository.save(deposit1);
        
        Deposit deposit2 = new Deposit();
        deposit2.setUserId(testUserId);
        deposit2.setAccountId(account2.getId());
        deposit2.setDepositType("活期");
        deposit2.setDepositTime(date);
        deposit2.setAmount(new BigDecimal("40000.00"));
        deposit2.setReconciliationDate(date);
        depositRepository.save(deposit2);
    }
    
    @Test
    @DisplayName("UC-RECON-CTRL-001: GET /api/reconciliation 获取对账数据（指定日期，有快照）")
    public void testGetReconciliation_WithDate_WithSnapshot() throws Exception {
        // When & Then: 发送获取对账数据请求
        mockMvc.perform(get("/api/reconciliation")
                .param("date", date1.toString())
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.date").value(date1.toString()))
                .andExpect(jsonPath("$.data.totalAmount").value(100000.00))
                .andExpect(jsonPath("$.data.note").value("备注1"))
                .andExpect(jsonPath("$.data.accounts").isArray())
                .andExpect(jsonPath("$.data.accounts.length()").value(2));
        
        System.out.println("✓ UC-RECON-CTRL-001: GET /api/reconciliation 获取对账数据（指定日期，有快照） - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-CTRL-002: GET /api/reconciliation 获取对账数据（未指定日期，返回最近一次）")
    public void testGetReconciliation_NoDate_ReturnsLatest() throws Exception {
        // When & Then: 发送获取对账数据请求（不指定日期）
        mockMvc.perform(get("/api/reconciliation")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.date").exists())
                .andExpect(jsonPath("$.data.totalAmount").exists());
        
        System.out.println("✓ UC-RECON-CTRL-002: GET /api/reconciliation 获取对账数据（未指定日期，返回最近一次） - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-CTRL-003: GET /api/reconciliation 获取对账数据（指定日期，无快照）")
    public void testGetReconciliation_WithDate_NoSnapshot() throws Exception {
        // Given: 使用一个没有快照的日期
        LocalDate noSnapshotDate = LocalDate.of(2024, 4, 15);
        
        // When & Then: 发送获取对账数据请求
        mockMvc.perform(get("/api/reconciliation")
                .param("date", noSnapshotDate.toString())
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.date").value(noSnapshotDate.toString()))
                .andExpect(jsonPath("$.data.totalAmount").value(0))
                .andExpect(jsonPath("$.data.accounts").isArray())
                .andExpect(jsonPath("$.data.accounts.length()").value(0));
        
        System.out.println("✓ UC-RECON-CTRL-003: GET /api/reconciliation 获取对账数据（指定日期，无快照） - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-CTRL-004: GET /api/reconciliation 未授权访问")
    public void testGetReconciliation_Unauthorized() throws Exception {
        // When & Then: 发送请求但不提供Token
        mockMvc.perform(get("/api/reconciliation")
                .param("date", date1.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.message").value("未授权，请先登录"));
        
        System.out.println("✓ UC-RECON-CTRL-004: GET /api/reconciliation 未授权访问 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-CTRL-005: POST /api/reconciliation/save 保存对账快照成功")
    public void testSaveReconciliation_Success() throws Exception {
        // Given: 准备保存对账请求
        LocalDate saveDate = LocalDate.of(2024, 4, 15);
        SaveReconciliationRequest request = new SaveReconciliationRequest();
        request.setDate(saveDate);
        request.setNote("保存测试备注");
        
        SaveReconciliationRequest.AccountDepositData accountData1 = new SaveReconciliationRequest.AccountDepositData();
        accountData1.setAccountId(account1.getId());
        List<SaveReconciliationRequest.DepositData> deposits1 = new ArrayList<>();
        SaveReconciliationRequest.DepositData deposit1 = new SaveReconciliationRequest.DepositData();
        deposit1.setDepositType("定期");
        deposit1.setDepositTime(saveDate);
        deposit1.setAmount(new BigDecimal("50000.00"));
        deposits1.add(deposit1);
        accountData1.setDeposits(deposits1);
        
        List<SaveReconciliationRequest.AccountDepositData> accounts = new ArrayList<>();
        accounts.add(accountData1);
        request.setAccounts(accounts);
        
        String requestBody = objectMapper.writeValueAsString(request);
        
        // When & Then: 发送保存对账请求
        mockMvc.perform(post("/api/reconciliation/save")
                .param("date", saveDate.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("保存成功"));
        
        // 验证快照已保存
        assertTrue(snapshotRepository.existsByUserIdAndReconciliationDate(testUserId, saveDate));
        
        System.out.println("✓ UC-RECON-CTRL-005: POST /api/reconciliation/save 保存对账快照成功 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-CTRL-006: GET /api/reconciliation/latest 获取最近一次对账日期")
    public void testGetLatestReconciliationDate_Success() throws Exception {
        // When & Then: 发送获取最近一次对账日期请求
        mockMvc.perform(get("/api/reconciliation/latest")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").value(date3.toString())); // 最近的日期是date3
        
        System.out.println("✓ UC-RECON-CTRL-006: GET /api/reconciliation/latest 获取最近一次对账日期 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-CTRL-007: GET /api/reconciliation/history 获取历史对账记录")
    public void testGetReconciliationHistory_Success() throws Exception {
        // When & Then: 发送获取历史对账记录请求
        mockMvc.perform(get("/api/reconciliation/history")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.dates").isArray())
                .andExpect(jsonPath("$.data.dates.length()").value(3)); // 有3个快照日期
        
        System.out.println("✓ UC-RECON-CTRL-007: GET /api/reconciliation/history 获取历史对账记录 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-CTRL-008: GET /api/reconciliation/previous 获取上一个快照日期")
    public void testGetPreviousSnapshotDate_Success() throws Exception {
        // When & Then: 发送获取上一个快照日期请求
        mockMvc.perform(get("/api/reconciliation/previous")
                .param("date", date2.toString())
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").value(date1.toString())); // date2的上一个是date1
        
        System.out.println("✓ UC-RECON-CTRL-008: GET /api/reconciliation/previous 获取上一个快照日期 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-CTRL-009: GET /api/reconciliation/next 获取下一个快照日期")
    public void testGetNextSnapshotDate_Success() throws Exception {
        // When & Then: 发送获取下一个快照日期请求
        mockMvc.perform(get("/api/reconciliation/next")
                .param("date", date2.toString())
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").value(date3.toString())); // date2的下一个是date3
        
        System.out.println("✓ UC-RECON-CTRL-009: GET /api/reconciliation/next 获取下一个快照日期 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-CTRL-010: POST /api/reconciliation/create-new 新建对账成功")
    public void testCreateNewReconciliation_Success() throws Exception {
        // Given: 准备新建对账的日期（在已有快照之后）
        LocalDate newDate = LocalDate.of(2024, 4, 15);
        
        // When & Then: 发送新建对账请求
        mockMvc.perform(post("/api/reconciliation/create-new")
                .param("date", newDate.toString())
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("新建对账成功"));
        
        // 验证新快照已创建
        assertTrue(snapshotRepository.existsByUserIdAndReconciliationDate(testUserId, newDate));
        
        // 验证新快照的数据是从最近一次快照复制的（date3）
        ReconciliationSnapshot newSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(testUserId, newDate)
                .orElseThrow();
        ReconciliationSnapshot latestSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(testUserId, date3)
                .orElseThrow();
        assertEquals(latestSnapshot.getTotalAmount(), newSnapshot.getTotalAmount());
        
        System.out.println("✓ UC-RECON-CTRL-010: POST /api/reconciliation/create-new 新建对账成功 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-CTRL-011: PUT /api/reconciliation/note 更新快照备注成功")
    public void testUpdateSnapshotNote_Success() throws Exception {
        // Given: 准备更新备注请求
        UpdateSnapshotNoteRequest request = new UpdateSnapshotNoteRequest();
        request.setNote("更新后的备注");
        String requestBody = objectMapper.writeValueAsString(request);
        
        // When & Then: 发送更新备注请求
        mockMvc.perform(put("/api/reconciliation/note")
                .param("date", date3.toString()) // date3原本没有备注
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("更新备注成功"));
        
        // 验证备注已更新
        ReconciliationSnapshot snapshot = snapshotRepository.findByUserIdAndReconciliationDate(testUserId, date3)
                .orElseThrow();
        assertEquals("更新后的备注", snapshot.getNote());
        
        System.out.println("✓ UC-RECON-CTRL-011: PUT /api/reconciliation/note 更新快照备注成功 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-CTRL-012: GET /api/reconciliation/snapshot-dates 获取所有快照日期列表")
    public void testGetSnapshotDates_Success() throws Exception {
        // When & Then: 发送获取快照日期列表请求
        mockMvc.perform(get("/api/reconciliation/snapshot-dates")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3)); // 有3个快照日期
        
        System.out.println("✓ UC-RECON-CTRL-012: GET /api/reconciliation/snapshot-dates 获取所有快照日期列表 - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-CTRL-013: GET /api/reconciliation/previous 没有上一个快照，返回null")
    public void testGetPreviousSnapshotDate_NoPrevious() throws Exception {
        // When & Then: 对最早的快照请求上一个日期，应该返回null
        mockMvc.perform(get("/api/reconciliation/previous")
                .param("date", date1.toString())
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").isEmpty()); // 没有上一个快照，返回null
        
        System.out.println("✓ UC-RECON-CTRL-013: GET /api/reconciliation/previous 没有上一个快照，返回null - 通过");
    }
    
    @Test
    @DisplayName("UC-RECON-CTRL-014: GET /api/reconciliation/next 没有下一个快照，返回null")
    public void testGetNextSnapshotDate_NoNext() throws Exception {
        // When & Then: 对最晚的快照请求下一个日期，应该返回null
        mockMvc.perform(get("/api/reconciliation/next")
                .param("date", date3.toString())
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").isEmpty()); // 没有下一个快照，返回null
        
        System.out.println("✓ UC-RECON-CTRL-014: GET /api/reconciliation/next 没有下一个快照，返回null - 通过");
    }
}
