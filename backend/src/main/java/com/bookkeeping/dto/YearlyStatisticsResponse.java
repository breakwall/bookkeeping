package com.bookkeeping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YearlyStatisticsResponse {
    private List<YearlyDataItem> data;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class YearlyDataItem {
        /**
         * 年份，格式：YYYY
         */
        private String year;
        
        /**
         * 该年的资产变化增值
         * 第一年：最后一次快照的totalAmount - 第一次快照的totalAmount
         * 第二年开始：该年最后一次快照的totalAmount - 上一年最后一次快照的totalAmount
         * 如果该年没有快照记录：为0
         */
        private BigDecimal increase;
    }
}
