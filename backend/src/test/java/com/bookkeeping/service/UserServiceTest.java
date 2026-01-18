package com.bookkeeping.service;

import com.bookkeeping.AbstractBaseTest;
import com.bookkeeping.dto.AuthResponse;
import com.bookkeeping.dto.LoginRequest;
import com.bookkeeping.dto.RegisterRequest;
import com.bookkeeping.entity.User;
import com.bookkeeping.repository.UserRepository;
import com.bookkeeping.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserService 单元测试示例
 * 
 * 测试用例：
 * - UC-AUTH-001: 正常注册成功
 * - UC-AUTH-002: 用户名已存在，注册失败
 * - UC-AUTH-004: 验证密码是否正确加密
 * - UC-AUTH-005: 正常登录成功
 * - UC-AUTH-006: 密码错误，登录失败
 * - UC-AUTH-007: 用户不存在，登录失败
 */
@DisplayName("UserService 单元测试")
public class UserServiceTest extends AbstractBaseTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Test
    @DisplayName("UC-AUTH-001: 正常注册成功")
    public void testRegister_Success() {
        // Given: 准备测试数据
        String username = generateUniqueUsername();
        String password = "testPassword123";
        String email = generateUniqueEmail();
        
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(email);
        
        // When: 执行注册
        AuthResponse response = userService.register(request);
        
        // Then: 验证结果
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(username, response.getUsername());
        assertNotNull(response.getToken());
        
        // 验证用户已保存到数据库
        Optional<User> savedUser = userRepository.findByUsername(username);
        assertTrue(savedUser.isPresent());
        assertEquals(username, savedUser.get().getUsername());
        assertEquals(email, savedUser.get().getEmail());
        
        // 验证Token有效
        assertTrue(jwtUtil.validateToken(response.getToken()));
        Long userIdFromToken = jwtUtil.getUserIdFromToken(response.getToken());
        assertEquals(response.getId(), userIdFromToken);
        
        System.out.println("✓ UC-AUTH-001: 正常注册成功 - 通过");
        System.out.println("  注册用户: " + username);
        System.out.println("  用户ID: " + response.getId());
        System.out.println("  Token: " + response.getToken().substring(0, 20) + "...");
    }
    
    @Test
    @DisplayName("UC-AUTH-002: 用户名已存在，注册失败")
    public void testRegister_UsernameExists() {
        // Given: 先创建一个用户
        String username = generateUniqueUsername();
        String password = "testPassword123";
        String email = generateUniqueEmail();
        
        RegisterRequest firstRequest = new RegisterRequest();
        firstRequest.setUsername(username);
        firstRequest.setPassword(password);
        firstRequest.setEmail(email);
        userService.register(firstRequest);
        
        // 尝试用相同用户名再次注册
        RegisterRequest secondRequest = new RegisterRequest();
        secondRequest.setUsername(username);  // 相同的用户名
        secondRequest.setPassword("anotherPassword");
        secondRequest.setEmail(generateUniqueEmail());
        
        // When & Then: 应该抛出异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.register(secondRequest);
        });
        
        assertEquals("用户名已存在", exception.getMessage());
        
        System.out.println("✓ UC-AUTH-002: 用户名已存在，注册失败 - 通过");
    }
    
    @Test
    @DisplayName("UC-AUTH-004: 验证密码是否正确加密")
    public void testRegister_PasswordEncrypted() {
        // Given: 准备测试数据
        String username = generateUniqueUsername();
        String password = "testPassword123";
        
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(generateUniqueEmail());
        
        // When: 执行注册
        userService.register(request);
        
        // Then: 验证密码已加密存储
        Optional<User> savedUser = userRepository.findByUsername(username);
        assertTrue(savedUser.isPresent());
        
        String storedPasswordHash = savedUser.get().getPasswordHash();
        assertNotNull(storedPasswordHash);
        assertNotEquals(password, storedPasswordHash);  // 密码不应该明文存储
        
        // 验证加密后的密码可以通过BCrypt验证
        assertTrue(passwordEncoder.matches(password, storedPasswordHash));
        
        System.out.println("✓ UC-AUTH-004: 验证密码是否正确加密 - 通过");
        System.out.println("  原始密码: " + password);
        System.out.println("  加密后: " + storedPasswordHash.substring(0, 20) + "...");
    }
    
    @Test
    @DisplayName("UC-AUTH-005: 正常登录成功")
    public void testLogin_Success() {
        // Given: 先注册一个用户
        String username = generateUniqueUsername();
        String password = "testPassword123";
        String email = generateUniqueEmail();
        
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword(password);
        registerRequest.setEmail(email);
        AuthResponse registerResponse = userService.register(registerRequest);
        
        // 准备登录请求
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        
        // When: 执行登录
        AuthResponse loginResponse = userService.login(loginRequest);
        
        // Then: 验证结果
        assertNotNull(loginResponse);
        assertEquals(registerResponse.getId(), loginResponse.getId());
        assertEquals(username, loginResponse.getUsername());
        assertNotNull(loginResponse.getToken());
        
        // 验证Token有效
        assertTrue(jwtUtil.validateToken(loginResponse.getToken()));
        
        System.out.println("✓ UC-AUTH-005: 正常登录成功 - 通过");
        System.out.println("  登录用户: " + username);
        System.out.println("  用户ID: " + loginResponse.getId());
    }
    
    @Test
    @DisplayName("UC-AUTH-006: 密码错误，登录失败")
    public void testLogin_WrongPassword() {
        // Given: 先注册一个用户
        String username = generateUniqueUsername();
        String correctPassword = "testPassword123";
        String wrongPassword = "wrongPassword";
        
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword(correctPassword);
        registerRequest.setEmail(generateUniqueEmail());
        userService.register(registerRequest);
        
        // 准备错误的登录请求
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(wrongPassword);  // 错误的密码
        
        // When & Then: 应该抛出异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.login(loginRequest);
        });
        
        assertEquals("用户名或密码错误", exception.getMessage());
        
        System.out.println("✓ UC-AUTH-006: 密码错误，登录失败 - 通过");
    }
    
    @Test
    @DisplayName("UC-AUTH-007: 用户不存在，登录失败")
    public void testLogin_UserNotExists() {
        // Given: 准备一个不存在的用户名
        String nonExistentUsername = generateUniqueUsername();
        
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(nonExistentUsername);
        loginRequest.setPassword("anyPassword");
        
        // When & Then: 应该抛出异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.login(loginRequest);
        });
        
        assertEquals("用户名或密码错误", exception.getMessage());
        
        System.out.println("✓ UC-AUTH-007: 用户不存在，登录失败 - 通过");
    }
}
