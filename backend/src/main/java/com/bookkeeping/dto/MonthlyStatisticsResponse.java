package com.bookkeeping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class MonthlyStatisticsResponse {
    private String month;
    private BigDecimal totalAmount;
    private List<AccountDistributionItem> distribution;
    
    @Data
    @AllArgsConstructor
    public static class AccountDistributionItem {
        private Long accountId;
        private String accountName;
        private BigDecimal amount;
        private Double percentage;
    }
}
