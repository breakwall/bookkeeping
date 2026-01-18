package com.bookkeeping.dto;

import com.bookkeeping.entity.Deposit;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DepositResponse {
    private Long id;
    private Long accountId;
    private String depositType;
    private LocalDate depositTime;
    private BigDecimal amount;
    private BigDecimal interestRate;
    private Integer term;
    private String note;
    private LocalDate reconciliationDate;
    
    public DepositResponse(Long id, Long accountId, String depositType, LocalDate depositTime,
                          BigDecimal amount, BigDecimal interestRate, Integer term,
                          String note, LocalDate reconciliationDate) {
        this.id = id;
        this.accountId = accountId;
        this.depositType = depositType;
        this.depositTime = depositTime;
        this.amount = amount;
        this.interestRate = interestRate;
        this.term = term;
        this.note = note;
        this.reconciliationDate = reconciliationDate;
    }
    
    public static DepositResponse fromEntity(Deposit deposit) {
        return new DepositResponse(
            deposit.getId(),
            deposit.getAccountId(),
            deposit.getDepositType(),
            deposit.getDepositTime(),
            deposit.getAmount(),
            deposit.getInterestRate(),
            deposit.getTerm(),
            deposit.getNote(),
            deposit.getReconciliationDate()
        );
    }
}
