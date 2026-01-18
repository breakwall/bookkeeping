package com.bookkeeping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateDepositRequest {
    
    @NotNull(message = "账户ID不能为空")
    private Long accountId;
    
    @NotBlank(message = "存款类型不能为空")
    private String depositType;
    
    @NotNull(message = "存款时间不能为空")
    private LocalDate depositTime;
    
    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    private BigDecimal amount;
    
    private BigDecimal interestRate;
    
    private Integer term;
    
    private String note;
    
    @NotNull(message = "对账日期不能为空")
    private LocalDate reconciliationDate;
}
