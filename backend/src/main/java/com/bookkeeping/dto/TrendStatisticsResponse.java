package com.bookkeeping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrendStatisticsResponse {
    private String period;
    private List<TrendDataItem> data;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TrendDataItem {
        private String month;
        private BigDecimal totalAmount;
        /**
         * 该月所有有备注的快照备注列表，格式：["2024-01-15: 备注1", "2024-01-20: 备注2"]
         * 如果该月最后一次快照没有备注，则此列表为空（即使前面快照有备注也不显示）
         * 如果该月最后一次快照有备注，则显示该月所有有备注的快照备注
         */
        private List<String> notes;
    }
}
