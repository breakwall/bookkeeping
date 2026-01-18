package com.bookkeeping.controller;

import com.bookkeeping.dto.ApiResponse;
import com.bookkeeping.dto.AuthResponse;
import com.bookkeeping.dto.LoginRequest;
import com.bookkeeping.dto.RegisterRequest;
import com.bookkeeping.dto.UserInfoResponse;
import com.bookkeeping.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ApiResponse.success("注册成功", response);
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ApiResponse.success("登录成功", response);
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserInfoResponse response = userService.getCurrentUser(userId);
        return ApiResponse.success(response);
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // 前端删除token即可，这里只是返回成功消息
        return ApiResponse.success("登出成功", null);
    }
}
