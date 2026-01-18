package com.bookkeeping.controller;

import com.bookkeeping.dto.AccountResponse;
import com.bookkeeping.dto.ApiResponse;
import com.bookkeeping.dto.CreateAccountRequest;
import com.bookkeeping.dto.UpdateAccountRequest;
import com.bookkeeping.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    
    @Autowired
    private AccountService accountService;
    
    /**
     * 获取账户列表
     */
    @GetMapping
    public ApiResponse<List<AccountResponse>> getAccounts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<AccountResponse> accounts = accountService.getAccounts(userId);
        return ApiResponse.success(accounts);
    }
    
    /**
     * 获取启用的账户列表（用于对账管理等场景）
     */
    @GetMapping("/active")
    public ApiResponse<List<AccountResponse>> getActiveAccounts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<AccountResponse> accounts = accountService.getActiveAccounts(userId);
        return ApiResponse.success(accounts);
    }
    
    /**
     * 获取账户详情
     */
    @GetMapping("/{id}")
    public ApiResponse<AccountResponse> getAccount(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        AccountResponse account = accountService.getAccount(id, userId);
        return ApiResponse.success(account);
    }
    
    /**
     * 创建账户
     */
    @PostMapping
    public ApiResponse<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request, 
                                                      HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        AccountResponse account = accountService.createAccount(request, userId);
        return ApiResponse.success("创建成功", account);
    }
    
    /**
     * 更新账户
     */
    @PutMapping("/{id}")
    public ApiResponse<AccountResponse> updateAccount(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateAccountRequest request,
                                                       HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        AccountResponse account = accountService.updateAccount(id, request, userId);
        return ApiResponse.success("更新成功", account);
    }
    
    /**
     * 删除账户
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAccount(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        accountService.deleteAccount(id, userId);
        return ApiResponse.success("删除成功", null);
    }
    
    /**
     * 启用账户
     */
    @PostMapping("/{id}/enable")
    public ApiResponse<AccountResponse> enableAccount(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        AccountResponse account = accountService.enableAccount(id, userId);
        return ApiResponse.success("启用成功", account);
    }
    
    /**
     * 禁用账户
     */
    @PostMapping("/{id}/disable")
    public ApiResponse<AccountResponse> disableAccount(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        AccountResponse account = accountService.disableAccount(id, userId);
        return ApiResponse.success("禁用成功", account);
    }
}
