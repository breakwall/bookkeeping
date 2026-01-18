package com.bookkeeping.controller;

import com.bookkeeping.dto.ApiResponse;
import com.bookkeeping.dto.ReconciliationDataResponse;
import com.bookkeeping.dto.ReconciliationHistoryResponse;
import com.bookkeeping.dto.SaveReconciliationRequest;
import com.bookkeeping.dto.UpdateSnapshotNoteRequest;
import com.bookkeeping.service.ReconciliationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {
    
    @Autowired
    private ReconciliationService reconciliationService;
    
    /**
     * 获取对账数据
     */
    @GetMapping
    public ApiResponse<ReconciliationDataResponse> getReconciliation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        // 如果没有传入日期，返回最近一次快照的日期
        if (date == null) {
            date = reconciliationService.getLatestReconciliationDate(userId);
            // 如果从未对账，返回空数据（使用今天的日期作为占位符）
            if (date == null) {
                date = LocalDate.now();
            }
        }
        ReconciliationDataResponse data = reconciliationService.getReconciliationData(userId, date);
        return ApiResponse.success(data);
    }
    
    /**
     * 保存对账快照
     */
    @PostMapping("/save")
    public ApiResponse<Void> saveReconciliation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody SaveReconciliationRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (date == null) {
            date = LocalDate.now();
        }
        // 使用请求参数中的date，如果为空则使用请求体中的date
        if (date == null) {
            date = request.getDate();
        }
        if (date == null) {
            date = LocalDate.now();
        }
        reconciliationService.saveReconciliation(userId, date, request);
        return ApiResponse.success("保存成功", null);
    }
    
    /**
     * 获取最近一次对账日期
     */
    @GetMapping("/latest")
    public ApiResponse<LocalDate> getLatestReconciliationDate(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        LocalDate date = reconciliationService.getLatestReconciliationDate(userId);
        return ApiResponse.success(date);
    }
    
    /**
     * 获取历史对账记录
     */
    @GetMapping("/history")
    public ApiResponse<ReconciliationHistoryResponse> getReconciliationHistory(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ReconciliationHistoryResponse history = reconciliationService.getReconciliationHistory(userId);
        return ApiResponse.success(history);
    }
    
    /**
     * 获取上一个快照日期
     */
    @GetMapping("/previous")
    public ApiResponse<LocalDate> getPreviousSnapshotDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        LocalDate previousDate = reconciliationService.getPreviousSnapshotDate(userId, date);
        return ApiResponse.success(previousDate);
    }
    
    /**
     * 获取下一个快照日期
     */
    @GetMapping("/next")
    public ApiResponse<LocalDate> getNextSnapshotDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        LocalDate nextDate = reconciliationService.getNextSnapshotDate(userId, date);
        return ApiResponse.success(nextDate);
    }
    
    /**
     * 新建对账：将选中日期之前最近一次快照复制到选中日期并保存到数据库
     */
    @PostMapping("/create-new")
    public ApiResponse<Void> createNewReconciliation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        reconciliationService.createNewReconciliation(userId, date);
        return ApiResponse.success("新建对账成功", null);
    }
    
    /**
     * 更新快照备注
     */
    @PutMapping("/note")
    public ApiResponse<Void> updateSnapshotNote(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody UpdateSnapshotNoteRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        reconciliationService.updateSnapshotNote(userId, date, request.getNote());
        return ApiResponse.success("更新备注成功", null);
    }
    
    /**
     * 获取所有快照日期列表（用于日历高亮）
     */
    @GetMapping("/snapshot-dates")
    public ApiResponse<java.util.List<LocalDate>> getSnapshotDates(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        java.util.List<LocalDate> dates = reconciliationService.getSnapshotDates(userId);
        return ApiResponse.success(dates);
    }
}
