package com.bookkeeping.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * SQLite LocalDate 类型转换器
 * 统一将 LocalDate 转换为字符串格式存储（'YYYY-MM-DD'）
 * 读取时兼容时间戳格式（自动转换）
 * 
 * 注意：这个转换器只对 JPA 实体字段生效，原生 SQL 查询需要单独处理
 */
@Converter(autoApply = true)
public class SQLiteLocalDateConverter implements AttributeConverter<LocalDate, String> {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    @Override
    public String convertToDatabaseColumn(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        // 统一转换为字符串格式 'YYYY-MM-DD'
        return localDate.toString();
    }
    
    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = dbData.trim();
        
        // 兼容两种格式：时间戳（数字字符串）和日期字符串
        try {
            // 首先尝试解析为时间戳（毫秒）
            // 检查是否是纯数字（可能是时间戳）
            if (trimmed.matches("^\\d+$")) {
                try {
                    long timestamp = Long.parseLong(trimmed);
                    // 时间戳通常是毫秒，但如果是秒级时间戳（小于某个阈值），需要乘以1000
                    if (timestamp < 10000000000L) {
                        // 可能是秒级时间戳，转换为毫秒
                        timestamp = timestamp * 1000;
                    }
                    return new java.util.Date(timestamp).toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                } catch (NumberFormatException e) {
                    // 继续尝试解析为日期字符串
                } catch (IllegalArgumentException e) {
                    // 继续尝试解析为日期字符串
                }
            }
            
            // 尝试解析为日期字符串（YYYY-MM-DD）
            return LocalDate.parse(trimmed, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("无法解析日期格式: " + trimmed + "，期望格式: YYYY-MM-DD 或时间戳（毫秒）", e);
        }
    }
}
