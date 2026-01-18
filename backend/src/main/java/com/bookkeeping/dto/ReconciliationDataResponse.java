package com.bookkeeping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class ReconciliationDataResponse {
    private LocalDate date;
    private String note; // 快照备注
    private java.math.BigDecimal totalAmount; // 快照总金额
    private List<AccountDepositData> accounts;
    
    @Data
    @AllArgsConstructor
    public static class AccountDepositData {
        private Long accountId;
        private String accountName;
        private List<DepositResponse> deposits;
    }
}
