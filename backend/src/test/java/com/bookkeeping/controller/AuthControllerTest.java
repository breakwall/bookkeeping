package com.bookkeeping.controller;

import com.bookkeeping.AbstractBaseTest;
import com.bookkeeping.dto.AuthResponse;
import com.bookkeeping.dto.RegisterRequest;
import com.bookkeeping.repository.UserRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 集成测试
 * 
 * 测试覆盖：
 * - 用户注册API（成功、参数验证失败、用户名已存在）
 * - 用户登录API（成功、用户名或密码错误）
 * - 获取当前用户信息API（成功、未授权访问）
 * - 用户登出API
 */
@DisplayName("AuthController 集成测试")
@AutoConfigureMockMvc
public class AuthControllerTest extends AbstractBaseTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private String validToken;
    private Long testUserId;
    private String testUsername;
    
    @BeforeEach
    public void setUp() {
        // 创建一个测试用户用于需要认证的测试（使用符合长度要求的用户名）
        RegisterRequest registerRequest = new RegisterRequest();
        String shortUsername = "u" + System.currentTimeMillis() % 1000000000L;
        testUsername = shortUsername.length() > 20 ? shortUsername.substring(0, 20) : shortUsername;
        registerRequest.setUsername(testUsername);
        registerRequest.setPassword("testPassword123");
        registerRequest.setEmail(generateUniqueEmail());
        
        AuthResponse authResponse = userService.register(registerRequest);
        testUserId = authResponse.getId();
        validToken = authResponse.getToken();
    }
    
    @Test
    @DisplayName("UC-AUTH-101: POST /api/auth/register 注册成功")
    public void testRegister_Success() throws Exception {
        // Given: 准备注册请求（用户名长度必须在3-20个字符之间）
        RegisterRequest request = new RegisterRequest();
        // 使用更短的唯一用户名（符合3-20个字符的限制）
        String shortUsername = "u" + System.currentTimeMillis() % 1000000000L; // 最多10位数字
        request.setUsername(shortUsername.length() > 20 ? shortUsername.substring(0, 20) : shortUsername);
        request.setPassword("testPassword123");
        request.setEmail(generateUniqueEmail());
        
        String requestBody = objectMapper.writeValueAsString(request);
        
        // When & Then: 发送注册请求
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.username").value(request.getUsername()))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();
        
        // 验证Token有效性
        String responseBody = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
        String token = (String) dataMap.get("token");
        
        assertTrue(jwtUtil.validateToken(token));
        Long userIdFromToken = jwtUtil.getUserIdFromToken(token);
        // 处理类型转换：JSON中的数字可能是Integer或Long
        Object idObj = dataMap.get("id");
        Long userIdFromResponse = idObj instanceof Long ? (Long) idObj : ((Integer) idObj).longValue();
        assertEquals(userIdFromResponse, userIdFromToken);
        
        // 验证用户已保存到数据库
        assertTrue(userRepository.existsByUsername(request.getUsername()));
        
        System.out.println("✓ UC-AUTH-101: POST /api/auth/register 注册成功 - 通过");
    }
    
    @Test
    @DisplayName("UC-AUTH-102: POST /api/auth/register 参数验证失败（用户名为空）")
    public void testRegister_ValidationError_EmptyUsername() throws Exception {
        // Given: 准备无效的注册请求（用户名为空）
        RegisterRequest request = new RegisterRequest();
        request.setUsername(""); // 空用户名
        request.setPassword("testPassword123");
        request.setEmail(generateUniqueEmail());
        
        String requestBody = objectMapper.writeValueAsString(request);
        
        // When & Then: 应该返回400 Bad Request
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
        
        System.out.println("✓ UC-AUTH-102: POST /api/auth/register 参数验证失败（用户名为空） - 通过");
    }
    
    @Test
    @DisplayName("UC-AUTH-102: POST /api/auth/register 参数验证失败（密码太短）")
    public void testRegister_ValidationError_ShortPassword() throws Exception {
        // Given: 准备无效的注册请求（密码太短）
        RegisterRequest request = new RegisterRequest();
        String shortUsername = "u" + System.currentTimeMillis() % 1000000000L;
        request.setUsername(shortUsername.length() > 20 ? shortUsername.substring(0, 20) : shortUsername);
        request.setPassword("123"); // 密码太短
        request.setEmail(generateUniqueEmail());
        
        String requestBody = objectMapper.writeValueAsString(request);
        
        // When & Then: 应该返回400 Bad Request
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
        
        System.out.println("✓ UC-AUTH-102: POST /api/auth/register 参数验证失败（密码太短） - 通过");
    }
    
    @Test
    @DisplayName("UC-AUTH-102: POST /api/auth/register 用户名已存在")
    public void testRegister_UsernameExists() throws Exception {
        // Given: 先创建一个用户（使用符合长度要求的用户名）
        String shortUsername = "u" + System.currentTimeMillis() % 1000000000L;
        String existingUsername = shortUsername.length() > 20 ? shortUsername.substring(0, 20) : shortUsername;
        RegisterRequest firstRequest = new RegisterRequest();
        firstRequest.setUsername(existingUsername);
        firstRequest.setPassword("testPassword123");
        firstRequest.setEmail(generateUniqueEmail());
        userService.register(firstRequest);
        
        // 尝试用相同用户名再次注册
        RegisterRequest secondRequest = new RegisterRequest();
        secondRequest.setUsername(existingUsername);
        secondRequest.setPassword("anotherPassword");
        secondRequest.setEmail(generateUniqueEmail());
        
        String requestBody = objectMapper.writeValueAsString(secondRequest);
        
        // When & Then: 应该返回400 Bad Request（GlobalExceptionHandler将RuntimeException处理为BAD_REQUEST）
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名已存在"));
        
        System.out.println("✓ UC-AUTH-102: POST /api/auth/register 用户名已存在 - 通过");
    }
    
    @Test
    @DisplayName("UC-AUTH-103: POST /api/auth/login 登录成功")
    public void testLogin_Success() throws Exception {
        // Given: 准备登录请求（使用setUp中创建的用户）
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", testUsername);
        loginRequest.put("password", "testPassword123");
        
        String requestBody = objectMapper.writeValueAsString(loginRequest);
        
        // When & Then: 发送登录请求
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(testUserId))
                .andExpect(jsonPath("$.data.username").value(testUsername))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();
        
        // 验证Token有效性
        String responseBody = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
        String token = (String) dataMap.get("token");
        
        assertTrue(jwtUtil.validateToken(token));
        Long userIdFromToken = jwtUtil.getUserIdFromToken(token);
        assertEquals(testUserId, userIdFromToken);
        
        System.out.println("✓ UC-AUTH-103: POST /api/auth/login 登录成功 - 通过");
    }
    
    @Test
    @DisplayName("UC-AUTH-104: POST /api/auth/login 登录失败（用户名不存在）")
    public void testLogin_UserNotExists() throws Exception {
        // Given: 准备登录请求（用户不存在）
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "nonexistentuser");
        loginRequest.put("password", "anyPassword");
        
        String requestBody = objectMapper.writeValueAsString(loginRequest);
        
        // When & Then: 应该返回400 Bad Request（GlobalExceptionHandler将RuntimeException处理为BAD_REQUEST）
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
        
        System.out.println("✓ UC-AUTH-104: POST /api/auth/login 登录失败（用户名不存在） - 通过");
    }
    
    @Test
    @DisplayName("UC-AUTH-104: POST /api/auth/login 登录失败（密码错误）")
    public void testLogin_WrongPassword() throws Exception {
        // Given: 准备登录请求（密码错误）
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", testUsername);
        loginRequest.put("password", "wrongPassword");
        
        String requestBody = objectMapper.writeValueAsString(loginRequest);
        
        // When & Then: 应该返回400 Bad Request（GlobalExceptionHandler将RuntimeException处理为BAD_REQUEST）
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
        
        System.out.println("✓ UC-AUTH-104: POST /api/auth/login 登录失败（密码错误） - 通过");
    }
    
    @Test
    @DisplayName("UC-AUTH-105: GET /api/auth/me 获取当前用户信息成功")
    public void testGetCurrentUser_Success() throws Exception {
        // Given: 使用有效的JWT Token
        // When & Then: 发送获取当前用户信息请求
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(testUserId))
                .andExpect(jsonPath("$.data.username").value(testUsername))
                .andExpect(jsonPath("$.data.email").exists());
        
        System.out.println("✓ UC-AUTH-105: GET /api/auth/me 获取当前用户信息成功 - 通过");
    }
    
    @Test
    @DisplayName("UC-AUTH-106: GET /api/auth/me 未授权访问（没有Token）")
    public void testGetCurrentUser_NoToken() throws Exception {
        // When & Then: 发送请求但不提供Token
        // JWT Filter返回的响应格式是 {"message":"未授权，请先登录"}，没有data字段
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.message").value("未授权，请先登录"));
        
        System.out.println("✓ UC-AUTH-106: GET /api/auth/me 未授权访问（没有Token） - 通过");
    }
    
    @Test
    @DisplayName("UC-AUTH-106: GET /api/auth/me 未授权访问（无效Token）")
    public void testGetCurrentUser_InvalidToken() throws Exception {
        // When & Then: 发送请求但提供无效Token
        // JWT Filter返回的响应格式是 {"message":"未授权，请先登录"}，没有data字段
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer invalid_token_12345"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.message").value("未授权，请先登录"));
        
        System.out.println("✓ UC-AUTH-106: GET /api/auth/me 未授权访问（无效Token） - 通过");
    }
    
    @Test
    @DisplayName("UC-AUTH-107: POST /api/auth/logout 登出成功")
    public void testLogout_Success() throws Exception {
        // When & Then: 发送登出请求（logout不在白名单中，所以需要Token）
        // 虽然logout在业务逻辑上不需要Token，但由于JWT Filter会拦截所有不在白名单中的请求，所以需要提供Token
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("登出成功"));
        
        System.out.println("✓ UC-AUTH-107: POST /api/auth/logout 登出成功 - 通过");
    }
    
    @Test
    @DisplayName("UC-AUTH-108: POST /api/auth/login 参数验证失败（用户名为空）")
    public void testLogin_ValidationError_EmptyUsername() throws Exception {
        // Given: 准备无效的登录请求（用户名为空）
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", ""); // 空用户名
        loginRequest.put("password", "testPassword123");
        
        String requestBody = objectMapper.writeValueAsString(loginRequest);
        
        // When & Then: 应该返回400 Bad Request
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
        
        System.out.println("✓ UC-AUTH-108: POST /api/auth/login 参数验证失败（用户名为空） - 通过");
    }
}
