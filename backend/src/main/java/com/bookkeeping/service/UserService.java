package com.bookkeeping.service;

import com.bookkeeping.dto.AuthResponse;
import com.bookkeeping.dto.LoginRequest;
import com.bookkeeping.dto.RegisterRequest;
import com.bookkeeping.dto.UserInfoResponse;
import com.bookkeeping.entity.User;
import com.bookkeeping.repository.UserRepository;
import com.bookkeeping.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 用户注册
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 如果提供了邮箱，检查邮箱是否已存在
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("邮箱已被使用");
            }
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        
        user = userRepository.save(user);
        
        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        return new AuthResponse(user.getId(), user.getUsername(), token);
    }
    
    /**
     * 用户登录
     */
    public AuthResponse login(LoginRequest request) {
        // 查找用户
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        return new AuthResponse(user.getId(), user.getUsername(), token);
    }
    
    /**
     * 获取当前用户信息
     */
    public UserInfoResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return new UserInfoResponse(user.getId(), user.getUsername(), user.getEmail());
    }
}
