package com.bookkeeping;

import com.bookkeeping.entity.ReconciliationSnapshot;
import com.bookkeeping.repository.ReconciliationSnapshotRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 迁移脚本：从存款表生成快照表数据
 * 
 * 关联关系说明：
 * - 快照表（reconciliation_snapshots）和存款表（deposits）通过 user_id + reconciliation_date 关联
 * - 一个快照记录对应一个对账日期的所有存款记录
 * - 快照表的 total_amount 是该日期下所有存款记录的金额总和
 * - 快照表的 note 是对账快照的备注（可以为空）
 * 
 * 注意：这是一个数据迁移脚本，不是单元测试。已经完成历史使命，但保留代码以备参考。
 * 使用 @Disabled 标记，确保在运行所有测试时不会执行。
 * 如需手动运行，请使用：mvn test -Dtest=MigrateReconciliationSnapshotsTest#migrateReconciliationSnapshots
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application.yml")
@Disabled("数据迁移脚本，已完成历史使命，不作为常规测试执行")
public class MigrateReconciliationSnapshotsTest {

    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private ReconciliationSnapshotRepository snapshotRepository;

    /**
     * 从存款表迁移数据到快照表
     * 遍历所有唯一的 (user_id, reconciliation_date) 组合，为每个组合创建快照记录
     * 
     * 注意：使用 JPA Repository 而不是原生 SQL，这样可以正确处理 Hibernate 的 ID 生成
     * 注意：使用 @Commit 确保数据被提交，而不是回滚
     */
    @Test
    @Commit
    public void migrateReconciliationSnapshots() {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("开始迁移快照数据...");
            
            // 1. 查询所有唯一的 (user_id, reconciliation_date) 组合，并计算总金额
            String querySql = "SELECT user_id, reconciliation_date, SUM(amount) as total_amount, COUNT(*) as deposit_count " +
                             "FROM deposits " +
                             "GROUP BY user_id, reconciliation_date " +
                             "ORDER BY user_id, reconciliation_date";
            
            List<SnapshotData> snapshotDataList = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(querySql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    Long userId = rs.getLong("user_id");
                    LocalDate reconciliationDate = rs.getDate("reconciliation_date").toLocalDate();
                    BigDecimal totalAmount = rs.getBigDecimal("total_amount");
                    int depositCount = rs.getInt("deposit_count");
                    
                    snapshotDataList.add(new SnapshotData(userId, reconciliationDate, totalAmount, depositCount));
                    
                    System.out.println(String.format("  发现: 用户ID=%d, 日期=%s, 总金额=%.2f, 存款记录数=%d",
                        userId, reconciliationDate, totalAmount, depositCount));
                }
            }
            
            if (snapshotDataList.isEmpty()) {
                System.out.println("没有找到需要迁移的数据");
                return;
            }
            
            System.out.println(String.format("\n共找到 %d 个唯一的 (用户ID, 对账日期) 组合\n", snapshotDataList.size()));
            
            int insertCount = 0;
            int updateCount = 0;
            int skipCount = 0;
            
            // 2. 为每个组合创建或更新快照记录（使用 JPA Repository）
            for (SnapshotData data : snapshotDataList) {
                // 检查是否已存在
                var existingSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(
                    data.userId, data.reconciliationDate);
                
                if (existingSnapshot.isPresent()) {
                    // 已存在，检查是否需要更新总金额
                    ReconciliationSnapshot snapshot = existingSnapshot.get();
                    if (snapshot.getTotalAmount().compareTo(data.totalAmount) != 0) {
                        // 总金额不一致，更新
                        snapshot.setTotalAmount(data.totalAmount);
                        snapshotRepository.save(snapshot);
                        updateCount++;
                        
                        System.out.println(String.format("  更新: 用户ID=%d, 日期=%s, 总金额 %.2f -> %.2f",
                            data.userId, data.reconciliationDate, snapshot.getTotalAmount(), data.totalAmount));
                    } else {
                        skipCount++;
                        System.out.println(String.format("  跳过: 用户ID=%d, 日期=%s (已存在且总金额一致)",
                            data.userId, data.reconciliationDate));
                    }
                } else {
                    // 不存在，创建新记录（使用 JPA Entity，Hibernate 会自动生成 ID）
                    ReconciliationSnapshot snapshot = new ReconciliationSnapshot();
                    snapshot.setUserId(data.userId);
                    snapshot.setReconciliationDate(data.reconciliationDate);
                    snapshot.setTotalAmount(data.totalAmount);
                    snapshot.setNote(null); // 历史数据没有备注
                    
                    snapshotRepository.save(snapshot);
                    insertCount++;
                    
                    System.out.println(String.format("  插入: 用户ID=%d, 日期=%s, 总金额=%.2f, 存款记录数=%d",
                        data.userId, data.reconciliationDate, data.totalAmount, data.depositCount));
                }
            }
            
            System.out.println("\n迁移完成！");
            System.out.println(String.format("  插入: %d 条", insertCount));
            System.out.println(String.format("  更新: %d 条", updateCount));
            System.out.println(String.format("  跳过: %d 条", skipCount));
            System.out.println(String.format("  总计: %d 条", snapshotDataList.size()));
            
        } catch (SQLException e) {
            System.err.println("数据库操作失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("迁移失败", e);
        }
    }
    
    /**
     * 快照数据内部类
     */
    private static class SnapshotData {
        Long userId;
        LocalDate reconciliationDate;
        BigDecimal totalAmount;
        int depositCount;
        
        SnapshotData(Long userId, LocalDate reconciliationDate, BigDecimal totalAmount, int depositCount) {
            this.userId = userId;
            this.reconciliationDate = reconciliationDate;
            this.totalAmount = totalAmount;
            this.depositCount = depositCount;
        }
    }
}
