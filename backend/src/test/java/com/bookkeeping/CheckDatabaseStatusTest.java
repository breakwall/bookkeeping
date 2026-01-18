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

/**
 * 检查数据库状态：查询快照和存款记录数量
 * 
 * 注意：这是一个数据库状态检查工具，不是单元测试。
 * 使用 @Disabled 标记，确保在运行所有测试时不会执行。
 * 如需手动运行，请使用：mvn test -Dtest=CheckDatabaseStatusTest#checkDatabaseStatus
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application.yml")
@Disabled("数据库状态检查工具，不作为常规测试执行")
public class CheckDatabaseStatusTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void checkDatabaseStatus() {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("========================================");
            System.out.println("数据库状态检查");
            System.out.println("========================================\n");
            
            // 1. 检查快照记录
            System.out.println("1. 快照记录 (reconciliation_snapshots):");
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as count FROM reconciliation_snapshots WHERE user_id = 1");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int snapshotCount = rs.getInt("count");
                    System.out.println("   用户ID=1 的快照数量: " + snapshotCount);
                    
                    if (snapshotCount > 0) {
                        // 列出所有快照日期
                        try (PreparedStatement stmt2 = conn.prepareStatement(
                                "SELECT reconciliation_date, total_amount, note " +
                                "FROM reconciliation_snapshots " +
                                "WHERE user_id = 1 " +
                                "ORDER BY reconciliation_date DESC");
                             ResultSet rs2 = stmt2.executeQuery()) {
                            System.out.println("   快照列表:");
                            while (rs2.next()) {
                                String date = rs2.getString("reconciliation_date");
                                String amount = rs2.getString("total_amount");
                                String note = rs2.getString("note");
                                System.out.println(String.format("     - 日期: %s, 总金额: %s, 备注: %s",
                                    date, amount, note != null ? note : "(空)"));
                            }
                        }
                    } else {
                        System.out.println("   ⚠️  没有快照记录！");
                    }
                }
            }
            
            System.out.println();
            
            // 2. 检查存款记录
            System.out.println("2. 存款记录 (deposits):");
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as count FROM deposits WHERE user_id = 1 AND reconciliation_date IS NOT NULL");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int depositCount = rs.getInt("count");
                    System.out.println("   用户ID=1 的存款记录数量 (有对账日期的): " + depositCount);
                    
                    if (depositCount > 0) {
                        // 按对账日期分组统计
                        try (PreparedStatement stmt2 = conn.prepareStatement(
                                "SELECT reconciliation_date, COUNT(*) as count, SUM(amount) as total " +
                                "FROM deposits " +
                                "WHERE user_id = 1 AND reconciliation_date IS NOT NULL " +
                                "GROUP BY reconciliation_date " +
                                "ORDER BY reconciliation_date DESC");
                             ResultSet rs2 = stmt2.executeQuery()) {
                            System.out.println("   按对账日期分组:");
                            while (rs2.next()) {
                                String date = rs2.getString("reconciliation_date");
                                int count = rs2.getInt("count");
                                String total = rs2.getString("total");
                                System.out.println(String.format("     - 日期: %s, 记录数: %d, 总金额: %s",
                                    date, count, total));
                            }
                        }
                    } else {
                        System.out.println("   ⚠️  没有存款记录！");
                    }
                }
            }
            
            System.out.println();
            
            // 3. 检查是否有存款记录但没有快照记录（需要迁移）
            System.out.println("3. 数据一致性检查:");
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT d.reconciliation_date, COUNT(*) as deposit_count, SUM(d.amount) as total_amount " +
                    "FROM deposits d " +
                    "WHERE d.user_id = 1 AND d.reconciliation_date IS NOT NULL " +
                    "AND NOT EXISTS ( " +
                    "  SELECT 1 FROM reconciliation_snapshots s " +
                    "  WHERE s.user_id = d.user_id AND s.reconciliation_date = d.reconciliation_date " +
                    ") " +
                    "GROUP BY d.reconciliation_date " +
                    "ORDER BY d.reconciliation_date");
                 ResultSet rs = stmt.executeQuery()) {
                
                boolean hasOrphanDeposits = false;
                while (rs.next()) {
                    if (!hasOrphanDeposits) {
                        System.out.println("   发现以下日期有存款记录但没有快照记录（可能需要迁移）:");
                        hasOrphanDeposits = true;
                    }
                    String date = rs.getString("reconciliation_date");
                    int count = rs.getInt("deposit_count");
                    String total = rs.getString("total_amount");
                    System.out.println(String.format("     - 日期: %s, 存款记录数: %d, 总金额: %s",
                        date, count, total));
                }
                
                if (!hasOrphanDeposits) {
                    System.out.println("   ✓ 数据一致：所有有存款记录的日期都有对应的快照记录");
                }
            }
            
            System.out.println();
            System.out.println("========================================");
            System.out.println("检查完成");
            System.out.println("========================================");
            
        } catch (SQLException e) {
            System.err.println("数据库操作失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("检查失败", e);
        }
    }
}
