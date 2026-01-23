package com.bookkeeping.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SaveReconciliationRequest {
    
    @NotNull(message = "对账日期不能为空")
    private LocalDate date;
    
    private String note; // 快照备注
    
    @Valid
    private List<AccountDepositData> accounts;
    
    @Data
    public static class AccountDepositData {
        @NotNull(message = "账户ID不能为空")
        private Long accountId;
        
        @Valid
        private List<DepositData> deposits;
    }
    
    @Data
    public static class DepositData {
        private Long id; // 如果为null，表示新记录
        private String depositType;
        private java.time.LocalDate depositTime;
        private java.math.BigDecimal amount;
        private java.math.BigDecimal interestRate;
        private java.math.BigDecimal term;
        private String note;
    }
}
