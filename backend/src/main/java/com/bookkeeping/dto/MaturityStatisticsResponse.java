package com.bookkeeping.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class MaturityStatisticsResponse {
    private List<MaturityDataItem> data;
    
    public MaturityStatisticsResponse(List<MaturityDataItem> data) {
        this.data = data;
    }
    
    @Data
    public static class MaturityDataItem {
        private String accountName;
        private BigDecimal depositAmount;
        private LocalDate depositTime;
        private LocalDate maturityDate;
        private Long remainingDays;
        
        public MaturityDataItem(String accountName, BigDecimal depositAmount, 
                              LocalDate depositTime, LocalDate maturityDate, Long remainingDays) {
            this.accountName = accountName;
            this.depositAmount = depositAmount;
            this.depositTime = depositTime;
            this.maturityDate = maturityDate;
            this.remainingDays = remainingDays;
        }
    }
}