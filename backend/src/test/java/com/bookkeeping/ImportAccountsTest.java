package com.bookkeeping;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入CSV中发现的银行/机构到账户管理表
 * 目标用户：justis
 * 
 * 注意：这是一个数据导入工具脚本，不是单元测试。
 * 使用 @Disabled 标记，确保在运行所有测试时不会执行。
 * 如需手动运行，请使用：mvn test -Dtest=ImportAccountsTest#importAccountsForJustis
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application.yml")
@Disabled("数据导入工具，不作为常规测试执行")
public class ImportAccountsTest {

    @Autowired
    private DataSource dataSource;

    /**
     * 导入17个银行/机构账户到用户justis
     */
    @Test
    public void importAccountsForJustis() {
        try (Connection conn = dataSource.getConnection()) {
            // 1. 查询用户justis的ID
            Long userId = getUserIdByUsername(conn, "justis");
            if (userId == null) {
                System.err.println("错误：未找到用户 'justis'，请先创建该用户");
                return;
            }
            
            System.out.println("找到用户: justis (ID: " + userId + ")");
            
            // 2. 检查是否已有账户
            int existingCount = getAccountCount(conn, userId);
            System.out.println("当前用户已有账户数: " + existingCount);
            if (existingCount > 0) {
                System.out.println("已存在的同名账户将被自动跳过");
            }
            
            // 3. 准备账户列表
            List<AccountInfo> accounts = createAccountList();
            
            // 4. 执行导入
            int successCount = 0;
            int skipCount = 0;
            
            System.out.println("\n开始导入账户...");
            for (AccountInfo account : accounts) {
                try {
                    int rowsAffected = insertAccount(conn, userId, account);
                    if (rowsAffected > 0) {
                        System.out.println("✓ 导入成功: " + account.name + " (" + account.type + ")");
                        successCount++;
                    } else {
                        System.out.println("- 跳过（已存在）: " + account.name + " (" + account.type + ")");
                        skipCount++;
                    }
                } catch (SQLException e) {
                    System.err.println("✗ 导入失败: " + account.name + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // 5. 显示结果
            System.out.println("\n" + "=".repeat(60));
            System.out.println("导入完成！");
            System.out.println("成功导入: " + successCount + " 个账户");
            System.out.println("跳过（已存在）: " + skipCount + " 个账户");
            System.out.println("总计: " + (successCount + skipCount) + " / " + accounts.size());
            
            // 6. 验证最终结果
            int finalCount = getAccountCount(conn, userId);
            System.out.println("\n当前用户总账户数: " + finalCount);
            
        } catch (SQLException e) {
            System.err.println("数据库错误: " + e.getMessage());
            e.printStackTrace();
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
     * 获取用户账户数量
     */
    private int getAccountCount(Connection conn, Long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM accounts WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * 插入账户（检查是否已存在，避免重复）
     */
    private int insertAccount(Connection conn, Long userId, AccountInfo account) throws SQLException {
        // 先检查是否已存在
        if (accountExists(conn, userId, account.name)) {
            return 0; // 已存在，跳过
        }
        
        // 插入新账户
        String sql = "INSERT INTO accounts (user_id, name, type, note, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, account.name);
            stmt.setString(3, account.type);
            stmt.setString(4, account.note);
            return stmt.executeUpdate();
        }
    }

    /**
     * 检查账户是否已存在
     */
    private boolean accountExists(Connection conn, Long userId, String accountName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM accounts WHERE user_id = ? AND name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, accountName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * 创建账户列表（17个账户）
     */
    private List<AccountInfo> createAccountList() {
        List<AccountInfo> accounts = new ArrayList<>();
        
        // 传统银行（10个）
        accounts.add(new AccountInfo("中国银行", "银行", "从CSV导入"));
        accounts.add(new AccountInfo("建设银行", "银行", "从CSV导入"));
        accounts.add(new AccountInfo("招商银行", "银行", "从CSV导入"));
        accounts.add(new AccountInfo("浦发银行", "银行", "从CSV导入"));
        accounts.add(new AccountInfo("众邦", "银行", "从CSV导入"));
        accounts.add(new AccountInfo("富民", "银行", "从CSV导入"));
        accounts.add(new AccountInfo("营口", "银行", "从CSV导入"));
        accounts.add(new AccountInfo("蓝海", "银行", "从CSV导入"));
        accounts.add(new AccountInfo("锡商银行", "银行", "从CSV导入"));
        accounts.add(new AccountInfo("辽沈", "银行", "从CSV导入"));
        
        // 支付宝
        accounts.add(new AccountInfo("支付宝", "支付宝", "从CSV导入"));
        
        // 微信
        accounts.add(new AccountInfo("微信", "微信", "从CSV导入"));
        
        // 理财APP
        accounts.add(new AccountInfo("京东金融", "理财APP", "从CSV导入"));
        accounts.add(new AccountInfo("壹钱包", "理财APP", "从CSV导入"));
        
        // 股票平台
        accounts.add(new AccountInfo("国金宝", "股票", "从CSV导入"));
        accounts.add(new AccountInfo("雪球", "股票", "从CSV导入"));
        
        return accounts;
    }

    /**
     * 账户信息内部类
     */
    private static class AccountInfo {
        String name;
        String type;
        String note;

        AccountInfo(String name, String type, String note) {
            this.name = name;
            this.type = type;
            this.note = note;
        }
    }
}
