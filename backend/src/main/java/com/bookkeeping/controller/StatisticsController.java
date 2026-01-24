package com.bookkeeping.controller;

import com.bookkeeping.dto.ApiResponse;
import com.bookkeeping.dto.MaturityStatisticsResponse;
import com.bookkeeping.dto.MonthlyStatisticsResponse;
import com.bookkeeping.dto.TrendStatisticsResponse;
import com.bookkeeping.dto.YearlyStatisticsResponse;
import com.bookkeeping.service.StatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    
    @Autowired
    private StatisticsService statisticsService;
    
    /**
     * 按月统计
     */
    @GetMapping("/monthly")
    public ApiResponse<MonthlyStatisticsResponse> getMonthlyStatistics(
            @RequestParam String month,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        MonthlyStatisticsResponse response = statisticsService.getMonthlyStatistics(userId, month);
        return ApiResponse.success(response);
    }
    
    /**
     * 趋势统计
     */
    @GetMapping("/trend")
    public ApiResponse<TrendStatisticsResponse> getTrendStatistics(
            @RequestParam String period,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        TrendStatisticsResponse response = statisticsService.getTrendStatistics(userId, period);
        return ApiResponse.success(response);
    }
    
    /**
     * 年度统计
     */
    @GetMapping("/yearly")
    public ApiResponse<YearlyStatisticsResponse> getYearlyStatistics(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        YearlyStatisticsResponse response = statisticsService.getYearlyStatistics(userId);
        return ApiResponse.success(response);
    }
    
    /**
     * 到期统计
     */
    @GetMapping("/maturity")
    public ApiResponse<MaturityStatisticsResponse> getMaturityStatistics(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        MaturityStatisticsResponse response = statisticsService.getMaturityStatistics(userId);
        return ApiResponse.success(response);
    }
}
