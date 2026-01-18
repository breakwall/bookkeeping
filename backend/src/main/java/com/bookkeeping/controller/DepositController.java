package com.bookkeeping.controller;

import com.bookkeeping.dto.ApiResponse;
import com.bookkeeping.dto.CreateDepositRequest;
import com.bookkeeping.dto.DepositResponse;
import com.bookkeeping.dto.UpdateDepositRequest;
import com.bookkeeping.service.DepositService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DepositController {
    
    @Autowired
    private DepositService depositService;
    
    /**
     * 获取账户在指定日期的存款记录
     */
    @GetMapping("/accounts/{accountId}/deposits")
    public ApiResponse<List<DepositResponse>> getDepositsByAccount(
            @PathVariable Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (date == null) {
            date = LocalDate.now();
        }
        List<DepositResponse> deposits = depositService.getDepositsByAccount(accountId, userId, date);
        return ApiResponse.success(deposits);
    }
    
    /**
     * 创建存款记录
     */
    @PostMapping("/deposits")
    public ApiResponse<DepositResponse> createDeposit(@Valid @RequestBody CreateDepositRequest request,
                                                      HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        DepositResponse deposit = depositService.createDeposit(request, userId);
        return ApiResponse.success("创建成功", deposit);
    }
    
    /**
     * 更新存款记录
     */
    @PutMapping("/deposits/{id}")
    public ApiResponse<DepositResponse> updateDeposit(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateDepositRequest request,
                                                      HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        DepositResponse deposit = depositService.updateDeposit(id, request, userId);
        return ApiResponse.success("更新成功", deposit);
    }
    
    /**
     * 删除存款记录
     */
    @DeleteMapping("/deposits/{id}")
    public ApiResponse<Void> deleteDeposit(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        depositService.deleteDeposit(id, userId);
        return ApiResponse.success("删除成功", null);
    }
}
