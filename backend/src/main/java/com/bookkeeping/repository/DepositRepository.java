package com.bookkeeping.repository;

import com.bookkeeping.entity.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface DepositRepository extends JpaRepository<Deposit, Long> {
    
    /**
     * 检查账户是否有存款记录
     */
    boolean existsByAccountId(Long accountId);
    
    /**
     * 查询账户的所有存款记录
     * 注意：显式转换日期字段为字符串，以兼容可能的时间戳格式
     */
    @Query(value = "SELECT id, user_id, account_id, deposit_type, " +
           "CAST(CASE WHEN typeof(deposit_time) = 'integer' THEN date(deposit_time/1000, 'unixepoch') ELSE deposit_time END AS TEXT) as deposit_time, " +
           "amount, interest_rate, term, note, " +
           "CAST(CASE WHEN typeof(reconciliation_date) = 'integer' THEN date(reconciliation_date/1000, 'unixepoch') ELSE reconciliation_date END AS TEXT) as reconciliation_date, " +
           "created_at, updated_at " +
           "FROM deposits WHERE account_id = :accountId", 
           nativeQuery = true)
    List<Deposit> findByAccountId(@Param("accountId") Long accountId);
    
    /**
     * 查询指定用户和日期的所有存款记录
     * 迁移后：所有日期已统一为字符串格式，直接使用字符串比较
     * 注意：显式转换日期字段为字符串，以兼容可能的时间戳格式
     */
    @Query(value = "SELECT id, user_id, account_id, deposit_type, " +
           "CAST(CASE WHEN typeof(deposit_time) = 'integer' THEN date(deposit_time/1000, 'unixepoch') ELSE deposit_time END AS TEXT) as deposit_time, " +
           "amount, interest_rate, term, note, " +
           "CAST(CASE WHEN typeof(reconciliation_date) = 'integer' THEN date(reconciliation_date/1000, 'unixepoch') ELSE reconciliation_date END AS TEXT) as reconciliation_date, " +
           "created_at, updated_at " +
           "FROM deposits WHERE user_id = :userId " +
           "AND (CASE WHEN typeof(reconciliation_date) = 'integer' THEN date(reconciliation_date/1000, 'unixepoch') ELSE reconciliation_date END) = :date", 
           nativeQuery = true)
    List<Deposit> findByUserIdAndReconciliationDate(@Param("userId") Long userId, @Param("date") String date);
    
    /**
     * 查询指定用户和日期的所有存款记录（使用 LocalDate）
     */
    default List<Deposit> findByUserIdAndReconciliationDate(Long userId, LocalDate date) {
        return findByUserIdAndReconciliationDate(userId, date.toString());
    }
    
    /**
     * 查询指定账户和日期的存款记录
     * 迁移后：所有日期已统一为字符串格式，直接使用字符串比较
     * 注意：显式转换日期字段为字符串，以兼容可能的时间戳格式
     */
    @Query(value = "SELECT id, user_id, account_id, deposit_type, " +
           "CAST(CASE WHEN typeof(deposit_time) = 'integer' THEN date(deposit_time/1000, 'unixepoch') ELSE deposit_time END AS TEXT) as deposit_time, " +
           "amount, interest_rate, term, note, " +
           "CAST(CASE WHEN typeof(reconciliation_date) = 'integer' THEN date(reconciliation_date/1000, 'unixepoch') ELSE reconciliation_date END AS TEXT) as reconciliation_date, " +
           "created_at, updated_at " +
           "FROM deposits WHERE account_id = :accountId " +
           "AND (CASE WHEN typeof(reconciliation_date) = 'integer' THEN date(reconciliation_date/1000, 'unixepoch') ELSE reconciliation_date END) = :date", 
           nativeQuery = true)
    List<Deposit> findByAccountIdAndReconciliationDate(@Param("accountId") Long accountId, @Param("date") String date);
    
    /**
     * 查询指定账户和日期的存款记录（使用 LocalDate）
     */
    default List<Deposit> findByAccountIdAndReconciliationDate(Long accountId, LocalDate date) {
        return findByAccountIdAndReconciliationDate(accountId, date.toString());
    }
    
    /**
     * 查询用户的所有记录，按日期倒序
     * 注意：显式转换日期字段为字符串，以兼容可能的时间戳格式
     */
    @Query(value = "SELECT id, user_id, account_id, deposit_type, " +
           "CAST(CASE WHEN typeof(deposit_time) = 'integer' THEN date(deposit_time/1000, 'unixepoch') ELSE deposit_time END AS TEXT) as deposit_time, " +
           "amount, interest_rate, term, note, " +
           "CAST(CASE WHEN typeof(reconciliation_date) = 'integer' THEN date(reconciliation_date/1000, 'unixepoch') ELSE reconciliation_date END AS TEXT) as reconciliation_date, " +
           "created_at, updated_at " +
           "FROM deposits WHERE user_id = :userId " +
           "ORDER BY reconciliation_date DESC", 
           nativeQuery = true)
    List<Deposit> findByUserIdOrderByReconciliationDateDesc(@Param("userId") Long userId);
    
    /**
     * 查询用户最近一次对账日期
     */
    @Query("SELECT MAX(d.reconciliationDate) FROM Deposit d WHERE d.userId = :userId")
    Optional<LocalDate> findMaxReconciliationDateByUserId(@Param("userId") Long userId);
    
    /**
     * 批量查询指定日期的记录（用于统计）
     * 迁移后：所有日期已统一为字符串格式，直接使用字符串比较
     * 注意：显式转换日期字段为字符串，以兼容可能的时间戳格式
     */
    @Query(value = "SELECT id, user_id, account_id, deposit_type, " +
           "CAST(CASE WHEN typeof(deposit_time) = 'integer' THEN date(deposit_time/1000, 'unixepoch') ELSE deposit_time END AS TEXT) as deposit_time, " +
           "amount, interest_rate, term, note, " +
           "CAST(CASE WHEN typeof(reconciliation_date) = 'integer' THEN date(reconciliation_date/1000, 'unixepoch') ELSE reconciliation_date END AS TEXT) as reconciliation_date, " +
           "created_at, updated_at " +
           "FROM deposits WHERE user_id = :userId " +
           "AND (CASE WHEN typeof(reconciliation_date) = 'integer' THEN date(reconciliation_date/1000, 'unixepoch') ELSE reconciliation_date END) IN (" +
           "SELECT value FROM json_each(:dates))", 
           nativeQuery = true)
    List<Deposit> findByUserIdAndReconciliationDateInNative(@Param("userId") Long userId, @Param("dates") String datesJson);
    
    /**
     * 批量查询指定日期的记录（使用 LocalDate）
     */
    default List<Deposit> findByUserIdAndReconciliationDateIn(Long userId, List<LocalDate> dates) {
        if (dates == null || dates.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        // 使用循环查询（如果日期数量不多）
        if (dates.size() <= 10) {
            return dates.stream()
                    .flatMap(date -> findByUserIdAndReconciliationDate(userId, date).stream())
                    .collect(Collectors.toList());
        } else {
            // 如果日期很多，使用原生查询
            return findByUserIdAndReconciliationDateInNative(userId, 
                dates.stream().map(d -> "\"" + d.toString() + "\"").collect(Collectors.joining(",", "[", "]")));
        }
    }
    
    /**
     * 删除指定日期的所有记录（保存快照时先删除旧数据）
     * 迁移后：所有日期已统一为字符串格式，直接使用字符串比较
     */
    @Modifying
    @Query(value = "DELETE FROM deposits WHERE user_id = :userId " +
           "AND reconciliation_date = :date", 
           nativeQuery = true)
    void deleteByUserIdAndReconciliationDate(@Param("userId") Long userId, @Param("date") String date);
    
    /**
     * 删除指定日期的所有记录（使用 LocalDate）
     */
    default void deleteByUserIdAndReconciliationDate(Long userId, LocalDate date) {
        deleteByUserIdAndReconciliationDate(userId, date.toString());
    }
    
    /**
     * 根据ID和用户ID查询存款记录（防止跨用户访问）
     */
    Optional<Deposit> findByIdAndUserId(Long id, Long userId);
    
    /**
     * 检查指定日期的对账数据是否存在
     * 迁移后：所有日期已统一为字符串格式，直接使用字符串比较
     * 注意：SQLite 返回整数，需要手动转换
     */
    @Query(value = "SELECT COUNT(*) FROM deposits WHERE user_id = :userId " +
           "AND reconciliation_date = :date", 
           nativeQuery = true)
    int countByUserIdAndReconciliationDateForExists(@Param("userId") Long userId, @Param("date") String date);
    
    /**
     * 检查指定日期的对账数据是否存在（使用 LocalDate）
     */
    default boolean existsByUserIdAndReconciliationDate(Long userId, LocalDate date) {
        return countByUserIdAndReconciliationDateForExists(userId, date.toString()) > 0;
    }
    
    /**
     * 检查指定日期的对账数据是否存在（使用 String）
     */
    default boolean existsByUserIdAndReconciliationDate(Long userId, String date) {
        return countByUserIdAndReconciliationDateForExists(userId, date) > 0;
    }
    
    /**
     * 统计指定用户和日期的存款记录数
     * 迁移后：所有日期已统一为字符串格式，直接使用字符串比较
     */
    @Query(value = "SELECT COUNT(*) FROM deposits WHERE user_id = :userId " +
           "AND reconciliation_date = :date", 
           nativeQuery = true)
    long countByUserIdAndReconciliationDate(@Param("userId") Long userId, @Param("date") String date);
    
    /**
     * 统计指定用户和日期的存款记录数（使用 LocalDate）
     */
    default long countByUserIdAndReconciliationDate(Long userId, LocalDate date) {
        return countByUserIdAndReconciliationDate(userId, date.toString());
    }
}
