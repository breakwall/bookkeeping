package com.bookkeeping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountTrendStatisticsResponse {
    private String period;
    private List<String> months;
    private List<AccountSeries> accounts;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AccountSeries {
        private Long accountId;
        private String accountName;
        private List<BigDecimal> amounts;
    }
}
