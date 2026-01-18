package com.bookkeeping.controller;

import com.bookkeeping.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {
    
    /**
     * 健康检查接口
     */
    @GetMapping("/")
    public ApiResponse<Map<String, String>> health() {
        Map<String, String> data = new HashMap<>();
        data.put("status", "ok");
        data.put("message", "记账管理系统后端服务运行正常");
        return ApiResponse.success(data);
    }
    
    /**
     * API健康检查
     */
    @GetMapping("/api/health")
    public ApiResponse<Map<String, String>> apiHealth() {
        Map<String, String> data = new HashMap<>();
        data.put("status", "ok");
        data.put("service", "bookkeeping-backend");
        data.put("version", "1.0.0");
        return ApiResponse.success(data);
    }
}
