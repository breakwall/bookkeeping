package com.bookkeeping;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 从CSV文件导入存款记录到数据库
 * 目标用户：justis
 * 
 * 注意：这是一个数据导入工具脚本，不是单元测试。
 * 使用 @Disabled 标记，确保在运行所有测试时不会执行。
 * 如需手动运行，请使用：mvn test -Dtest=ImportCsvDepositsTest#importCsvDeposits
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application.yml")
@Disabled("数据导入工具，不作为常规测试执行")
public class ImportCsvDepositsTest {

    @Autowired
    private DataSource dataSource;

    // CSV文件所在目录（相对于项目根目录）
    private static final String CSV_DIR = "../csv_data";

    /**
     * 导入所有CSV文件中的存款记录
     */
    @Test
    public void importCsvDeposits() {
        try (Connection conn = dataSource.getConnection()) {
            // 1. 查询用户justis的ID
            Long userId = getUserIdByUsername(conn, "justis");
            if (userId == null) {
                System.err.println("错误：未找到用户 'justis'，请先创建该用户");
                return;
            }
            
            System.out.println("找到用户: justis (ID: " + userId + ")");
            
            // 2. 清空之前的对账快照数据
            System.out.println("\n清空之前的对账快照数据...");
            int deletedCount = deleteAllReconciliationData(conn, userId);
            System.out.println("已删除 " + deletedCount + " 条历史对账记录");
            
            // 3. 获取所有账户名称到ID的映射
            Map<String, Long> accountNameToIdMap = getAccountNameToIdMap(conn, userId);
            System.out.println("\n找到 " + accountNameToIdMap.size() + " 个账户:");
            accountNameToIdMap.keySet().stream().sorted().forEach(name -> 
                System.out.println("  - " + name));
            
            // 4. 获取所有CSV文件
            Path csvDir = Paths.get(CSV_DIR);
            if (!Files.exists(csvDir)) {
                System.err.println("错误：CSV目录不存在: " + csvDir.toAbsolutePath());
                return;
            }
            
            // 5. 处理每个CSV文件
            Files.list(csvDir)
                .filter(p -> p.getFileName().toString().matches("\\d{6}\\.csv"))
                .sorted()
                .forEach(csvFile -> {
                    try {
                        processCsvFile(conn, userId, accountNameToIdMap, csvFile);
                    } catch (Exception e) {
                        System.err.println("处理文件失败: " + csvFile.getFileName() + " - " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            
            System.out.println("\n导入完成！");
            
        } catch (SQLException | IOException e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理单个CSV文件
     */
    private void processCsvFile(Connection conn, Long userId, Map<String, Long> accountMap, Path csvFile) 
            throws IOException, SQLException {
        
        String fileName = csvFile.getFileName().toString();
        // 从文件名解析日期：240629 -> 2024-06-29
        LocalDate reconciliationDate = parseDateFromFileName(fileName);
        if (reconciliationDate == null) {
            System.err.println("跳过文件（无法解析日期）: " + fileName);
            return;
        }
        
        System.out.println("\n处理文件: " + fileName + " (对账日期: " + reconciliationDate + ")");
        
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile.toFile(), java.nio.charset.StandardCharsets.UTF_8))) {
            // 跳过标题行
            String headerLine = reader.readLine();
            if (headerLine == null || !headerLine.contains("银行")) {
                System.err.println("跳过文件（格式不正确）: " + fileName);
                return;
            }
            
            // 检查是否有term列
            boolean hasTermColumn = headerLine.contains("term") || headerLine.split(",").length > 6;
            
            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                
                try {
                    DepositRecord record = parseCsvLine(line, hasTermColumn);
                    if (record == null) {
                        System.err.println("  行 " + lineNum + ": 解析返回null，跳过。行内容: " + line.substring(0, Math.min(50, line.length())));
                        skipCount++;
                        continue;
                    }
                    
                    // 查找账户ID
                    Long accountId = accountMap.get(record.bankName);
                    if (accountId == null) {
                        System.err.println("  行 " + lineNum + ": 未找到账户 '" + record.bankName + "'，跳过");
                        skipCount++;
                        continue;
                    }
                    
                    // 导入存款记录
                    try {
                        if (insertDeposit(conn, userId, accountId, record, reconciliationDate)) {
                            successCount++;
                        } else {
                            System.err.println("  行 " + lineNum + ": 插入返回false，跳过。银行: " + record.bankName + ", 金额: " + record.amount);
                            skipCount++;
                        }
                    } catch (SQLException e) {
                        System.err.println("  行 " + lineNum + ": 插入失败: " + e.getMessage() + "，银行: " + record.bankName);
                        errorCount++;
                    }
                    
                } catch (Exception e) {
                    System.err.println("  行 " + lineNum + " 解析失败: " + e.getMessage() + "，行内容: " + line.substring(0, Math.min(50, line.length())));
                    errorCount++;
                }
            }
        }
        
        System.out.println("  成功: " + successCount + ", 跳过: " + skipCount + ", 错误: " + errorCount);
    }

    /**
     * 解析CSV行
     * 使用 split(",", -1) 来保留所有空字符串，确保能正确解析末尾的空列
     */
    private DepositRecord parseCsvLine(String line, boolean hasTermColumn) {
        // 使用 -1 参数来保留所有空字符串，包括末尾的空列
        String[] parts = line.split(",", -1);
        if (parts.length < 6) {
            // 如果列数不足6列，可能是空行或格式错误，返回null
            return null;
        }
        
        DepositRecord record = new DepositRecord();
        
        try {
            // 银行名称
            record.bankName = parts[0].trim();
            if (record.bankName.isEmpty()) {
                return null;
            }
            
            // 存款类型
            String depositType = parts[1].trim();
            // 股票 -> 理财
            if ("股票".equals(depositType)) {
                depositType = "理财";
            }
            record.depositType = depositType;
            
            // 金额
            if (parts[2].trim().isEmpty()) {
                return null;
            }
            record.amount = new BigDecimal(parts[2].trim());
            
            // 利率（可能为空）
            // CSV中的利率是小数格式（如0.0540表示5.40%）
            // 根据用户要求，数据库中的利息应该存成小数格式
            // 注意：数据库字段DECIMAL(5,2)只有2位小数，但CSV中有4位小数（如0.0540）
            // 如果直接存储可能超出精度，这里先按小数格式存储，如果导入失败需要调整数据库schema
            String interestRateStr = parts.length > 3 ? parts[3].trim() : "";
            if (!interestRateStr.isEmpty()) {
                record.interestRate = new BigDecimal(interestRateStr);
            }
            
            // 存入日期（可能为空，如果为空使用1970-01-01）
            String depositDateStr = parts.length > 4 ? parts[4].trim() : "";
            record.depositTime = parseDepositDate(depositDateStr);
            
            // 备注
            record.note = parts.length > 5 ? parts[5].trim() : "";
            
            // term（如果有）
            if (hasTermColumn && parts.length > 6) {
                String termStr = parts[6].trim();
                if (!termStr.isEmpty()) {
                    try {
                        record.term = Integer.parseInt(termStr);
                    } catch (NumberFormatException e) {
                        // term解析失败，保持为null
                    }
                }
            }
            
            return record;
            
        } catch (NumberFormatException e) {
            // 数字解析失败（金额、利率等）
            return null;
        } catch (Exception e) {
            // 其他异常，返回null
            return null;
        }
    }

    /**
     * 从文件名解析日期：240629 -> 2024-06-29
     */
    private LocalDate parseDateFromFileName(String fileName) {
        // 提取文件名中的6位数字：240629
        Pattern pattern = Pattern.compile("(\\d{6})\\.csv");
        java.util.regex.Matcher matcher = pattern.matcher(fileName);
        if (!matcher.find()) {
            return null;
        }
        
        String dateStr = matcher.group(1);
        // YYMMDD -> YYYY-MM-DD
        int year = Integer.parseInt(dateStr.substring(0, 2));
        int month = Integer.parseInt(dateStr.substring(2, 4));
        int day = Integer.parseInt(dateStr.substring(4, 6));
        
        // 年份处理：24 -> 2024, 26 -> 2026
        if (year < 50) {
            year += 2000;
        } else {
            year += 1900;
        }
        
        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            System.err.println("日期解析失败: " + dateStr + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析存入日期（支持多种格式）
     */
    private LocalDate parseDepositDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            // 默认为1970-01-01
            return LocalDate.of(1970, 1, 1);
        }
        
        dateStr = dateStr.trim();
        
        // 支持的日期格式
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy/M/dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/d")
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // 继续尝试下一个格式
            }
        }
        
        // 如果所有格式都失败，返回默认日期
        System.err.println("日期格式无法解析: " + dateStr + "，使用默认日期1970-01-01");
        return LocalDate.of(1970, 1, 1);
    }

    /**
     * 插入存款记录
     */
    private boolean insertDeposit(Connection conn, Long userId, Long accountId, DepositRecord record, 
                                 LocalDate reconciliationDate) throws SQLException {
        
        String sql = "INSERT INTO deposits (user_id, account_id, deposit_type, deposit_time, amount, " +
                     "interest_rate, term, note, reconciliation_date, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, accountId);
            stmt.setString(3, record.depositType);
            stmt.setDate(4, java.sql.Date.valueOf(record.depositTime));
            stmt.setBigDecimal(5, record.amount);
            
            if (record.interestRate != null) {
                // 尝试设置利率，如果精度超出范围会抛出异常
                stmt.setBigDecimal(6, record.interestRate);
            } else {
                stmt.setNull(6, java.sql.Types.DECIMAL);
            }
            
            if (record.term != null) {
                stmt.setInt(7, record.term);
            } else {
                stmt.setNull(7, java.sql.Types.INTEGER);
            }
            
            stmt.setString(8, record.note != null ? record.note : "");
            stmt.setDate(9, java.sql.Date.valueOf(reconciliationDate));
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * 清空指定用户的所有对账快照数据
     */
    private int deleteAllReconciliationData(Connection conn, Long userId) throws SQLException {
        // SQLite默认是autocommit模式，但为了确保删除操作完成，我们显式执行
        String sql = "DELETE FROM deposits WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            int deletedCount = stmt.executeUpdate();
            return deletedCount;
        }
    }

    /**
     * 根据用户名查询用户ID
     */
    private Long getUserIdByUsername(Connection conn, String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        return null;
    }

    /**
     * 获取账户名称到ID的映射
     */
    private Map<String, Long> getAccountNameToIdMap(Connection conn, Long userId) throws SQLException {
        Map<String, Long> map = new HashMap<>();
        String sql = "SELECT id, name FROM accounts WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("name"), rs.getLong("id"));
                }
            }
        }
        return map;
    }

    /**
     * 存款记录内部类
     */
    private static class DepositRecord {
        String bankName;
        String depositType;
        BigDecimal amount;
        BigDecimal interestRate;
        LocalDate depositTime;
        String note;
        Integer term;
    }
}
