package com.bookkeeping.repository;

import com.bookkeeping.entity.ReconciliationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReconciliationSnapshotRepository extends JpaRepository<ReconciliationSnapshot, Long> {
    
    /**
     * 根据用户ID和对账日期查找快照
     * 迁移后：所有日期已统一为字符串格式，直接使用字符串比较
     * 注意：显式转换日期字段为字符串，以兼容可能的时间戳格式
     */
    @Query(value = "SELECT id, user_id, " +
           "CAST(CASE WHEN typeof(reconciliation_date) = 'integer' THEN date(reconciliation_date/1000, 'unixepoch') ELSE reconciliation_date END AS TEXT) as reconciliation_date, " +
           "total_amount, note, created_at, updated_at " +
           "FROM reconciliation_snapshots WHERE user_id = :userId " +
           "AND (CASE WHEN typeof(reconciliation_date) = 'integer' THEN date(reconciliation_date/1000, 'unixepoch') ELSE reconciliation_date END) = :date", 
           nativeQuery = true)
    Optional<ReconciliationSnapshot> findByUserIdAndReconciliationDate(@Param("userId") Long userId, @Param("date") String date);
    
    /**
     * 根据用户ID和对账日期查找快照（使用 LocalDate）
     */
    default Optional<ReconciliationSnapshot> findByUserIdAndReconciliationDate(Long userId, LocalDate date) {
        return findByUserIdAndReconciliationDate(userId, date.toString());
    }
    
    /**
     * 根据用户ID查找所有快照，按日期倒序
     * 注意：显式转换日期字段为字符串，以兼容可能的时间戳格式
     */
    @Query(value = "SELECT id, user_id, " +
           "CAST(CASE WHEN typeof(reconciliation_date) = 'integer' THEN date(reconciliation_date/1000, 'unixepoch') ELSE reconciliation_date END AS TEXT) as reconciliation_date, " +
           "total_amount, note, created_at, updated_at " +
           "FROM reconciliation_snapshots WHERE user_id = :userId " +
           "ORDER BY reconciliation_date DESC", 
           nativeQuery = true)
    List<ReconciliationSnapshot> findByUserIdOrderByReconciliationDateDesc(@Param("userId") Long userId);
    
    /**
     * 根据用户ID查找指定日期范围内的快照
     */
    @Query("SELECT s FROM ReconciliationSnapshot s WHERE s.userId = :userId " +
           "AND s.reconciliationDate >= :startDate AND s.reconciliationDate <= :endDate " +
           "ORDER BY s.reconciliationDate DESC")
    List<ReconciliationSnapshot> findByUserIdAndReconciliationDateBetween(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * 根据用户ID查找指定月份的所有快照，返回该月的最后一次快照
     * 迁移后：使用原生 SQL，因为日期已统一为字符串格式
     * 注意：显式转换日期字段为字符串，以兼容可能的时间戳格式
     */
    @Query(value = "SELECT id, user_id, " +
           "CAST(CASE WHEN typeof(reconciliation_date) = 'integer' THEN date(reconciliation_date/1000, 'unixepoch') ELSE reconciliation_date END AS TEXT) as reconciliation_date, " +
           "total_amount, note, created_at, updated_at " +
           "FROM reconciliation_snapshots WHERE user_id = :userId " +
           "AND strftime('%Y', CASE WHEN typeof(reconciliation_date) = 'integer' THEN date(reconciliation_date/1000, 'unixepoch') ELSE reconciliation_date END) = :yearStr " +
           "AND strftime('%m', CASE WHEN typeof(reconciliation_date) = 'integer' THEN date(reconciliation_date/1000, 'unixepoch') ELSE reconciliation_date END) = :monthStr " +
           "ORDER BY reconciliation_date DESC", 
           nativeQuery = true)
    List<ReconciliationSnapshot> findByUserIdAndYearAndMonthNative(
        @Param("userId") Long userId,
        @Param("yearStr") String yearStr,
        @Param("monthStr") String monthStr
    );
    
    /**
     * 根据用户ID查找指定月份的所有快照（使用 int 参数）
     */
    default List<ReconciliationSnapshot> findByUserIdAndYearAndMonth(
        Long userId,
        int year,
        int month
    ) {
        String yearStr = String.valueOf(year);
        String monthStr = String.format("%02d", month); // 确保月份是两位数，如 "01", "02"
        return findByUserIdAndYearAndMonthNative(userId, yearStr, monthStr);
    }
    
    /**
     * 根据用户ID和对账日期删除快照
     * 迁移后：所有日期已统一为字符串格式，直接使用字符串比较
     */
    @Modifying
    @Query(value = "DELETE FROM reconciliation_snapshots WHERE user_id = :userId " +
           "AND reconciliation_date = :date", 
           nativeQuery = true)
    void deleteByUserIdAndReconciliationDate(@Param("userId") Long userId, @Param("date") String date);
    
    /**
     * 根据用户ID和对账日期删除快照（使用 LocalDate）
     */
    default void deleteByUserIdAndReconciliationDate(Long userId, LocalDate date) {
        deleteByUserIdAndReconciliationDate(userId, date.toString());
    }
    
    /**
     * 检查是否存在指定用户和日期的快照
     * 迁移后：所有日期已统一为字符串格式，直接使用字符串比较
     * 注意：SQLite 返回整数，需要手动转换
     */
    @Query(value = "SELECT COUNT(*) FROM reconciliation_snapshots WHERE user_id = :userId " +
           "AND reconciliation_date = :date", 
           nativeQuery = true)
    int countByUserIdAndReconciliationDateNative(@Param("userId") Long userId, @Param("date") String date);
    
    /**
     * 检查是否存在指定用户和日期的快照（使用 LocalDate）
     */
    default boolean existsByUserIdAndReconciliationDate(Long userId, LocalDate date) {
        return countByUserIdAndReconciliationDateNative(userId, date.toString()) > 0;
    }
    
    /**
     * 检查是否存在指定用户和日期的快照（使用 String）
     */
    default boolean existsByUserIdAndReconciliationDate(Long userId, String date) {
        return countByUserIdAndReconciliationDateNative(userId, date) > 0;
    }
    
    /**
     * 查找用户最新的快照日期
     */
    @Query("SELECT MAX(s.reconciliationDate) FROM ReconciliationSnapshot s WHERE s.userId = :userId")
    Optional<LocalDate> findMaxReconciliationDateByUserId(@Param("userId") Long userId);
}
