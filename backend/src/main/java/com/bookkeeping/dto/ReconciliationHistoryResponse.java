package com.bookkeeping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class ReconciliationHistoryResponse {
    private List<HistoryItem> dates;
    
    @Data
    @AllArgsConstructor
    public static class HistoryItem {
        private LocalDate date;
        private Long recordCount;
    }
}
