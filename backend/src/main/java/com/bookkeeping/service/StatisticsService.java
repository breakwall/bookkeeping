package com.bookkeeping.service;

import com.bookkeeping.dto.AccountTrendStatisticsResponse;
import com.bookkeeping.dto.MaturityStatisticsResponse;
import com.bookkeeping.dto.MonthlyStatisticsResponse;
import com.bookkeeping.dto.TrendStatisticsResponse;
import com.bookkeeping.dto.YearlyStatisticsResponse;
import com.bookkeeping.entity.Account;
import com.bookkeeping.entity.Deposit;
import com.bookkeeping.entity.ReconciliationSnapshot;
import com.bookkeeping.repository.AccountRepository;
import com.bookkeeping.repository.DepositRepository;
import com.bookkeeping.repository.ReconciliationSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsService {
    
    @Autowired
    private DepositRepository depositRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private ReconciliationSnapshotRepository snapshotRepository;
    
    /**
     * 按月统计
     */
    public MonthlyStatisticsResponse getMonthlyStatistics(Long userId, String month) {
        // 解析月份
        LocalDate monthStart = LocalDate.parse(month + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        
        // 优先从快照表查找，如果没有则从存款表查找（兼容历史数据）
        List<ReconciliationSnapshot> allSnapshots = snapshotRepository.findByUserIdOrderByReconciliationDateDesc(userId);
        Optional<LocalDate> lastDateInMonth;
        
        if (!allSnapshots.isEmpty()) {
            // 有快照记录，从快照表获取日期
            lastDateInMonth = allSnapshots.stream()
                    .map(ReconciliationSnapshot::getReconciliationDate)
                    .filter(date -> !date.isBefore(monthStart) && !date.isAfter(monthEnd))
                    .distinct()
                    .max(Comparator.naturalOrder());
        } else {
            // 没有快照记录，从存款表获取日期（兼容历史数据）
            List<Deposit> allDeposits = depositRepository.findByUserIdOrderByReconciliationDateDesc(userId);
            lastDateInMonth = allDeposits.stream()
                    .map(Deposit::getReconciliationDate)
                    .filter(date -> !date.isBefore(monthStart) && !date.isAfter(monthEnd))
                    .distinct()
                    .max(Comparator.naturalOrder());
        }
        
        LocalDate targetDate;
        if (lastDateInMonth.isPresent()) {
            targetDate = lastDateInMonth.get();
        } else {
            // 该月没有记录，向前查找最近月份
            targetDate = findLatestDateBefore(userId, monthStart);
            if (targetDate == null) {
                // 完全找不到记录，返回空数据
                return new MonthlyStatisticsResponse(month, BigDecimal.ZERO, new ArrayList<>());
            }
        }
        
        // 优先从快照表获取总金额，如果没有则实时计算（兼容历史数据）
        BigDecimal totalAmount = snapshotRepository.findByUserIdAndReconciliationDate(userId, targetDate)
                .map(ReconciliationSnapshot::getTotalAmount)
                .orElseGet(() -> {
                    // 没有快照记录，实时计算总金额
                    List<Deposit> deposits = depositRepository.findByUserIdAndReconciliationDate(userId, targetDate);
                    return deposits.stream()
                            .map(Deposit::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                });
        
        // 获取该日期的存款记录（用于计算账户分布）
        List<Deposit> deposits = depositRepository.findByUserIdAndReconciliationDate(userId, targetDate);
        
        if (deposits.isEmpty()) {
            return new MonthlyStatisticsResponse(month, totalAmount, new ArrayList<>());
        }
        
        // 按账户分组统计
        Map<Long, List<Deposit>> depositsByAccount = deposits.stream()
                .collect(Collectors.groupingBy(Deposit::getAccountId));
        
        // 构建分布数据
        List<MonthlyStatisticsResponse.AccountDistributionItem> distribution = new ArrayList<>();
        
        for (Map.Entry<Long, List<Deposit>> entry : depositsByAccount.entrySet()) {
            Long accountId = entry.getKey();
            List<Deposit> accountDeposits = entry.getValue();
            
            // 计算该账户的总金额
            BigDecimal accountAmount = accountDeposits.stream()
                    .map(Deposit::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // 计算百分比（保留2位小数）
            Double percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? accountAmount.divide(totalAmount, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                            .setScale(2, RoundingMode.HALF_UP)
                            .doubleValue()
                    : 0.0;
            
            // 获取账户名称
            String accountName = accountRepository.findById(accountId)
                    .map(Account::getName)
                    .orElse("未知账户");
            
            distribution.add(new MonthlyStatisticsResponse.AccountDistributionItem(
                accountId,
                accountName,
                accountAmount,
                percentage
            ));
        }
        
        return new MonthlyStatisticsResponse(month, totalAmount, distribution);
    }
    
    /**
     * 趋势统计
     */
    public TrendStatisticsResponse getTrendStatistics(Long userId, String period) {
        // 优先从快照表获取，如果没有则从存款表获取（兼容历史数据）
        List<ReconciliationSnapshot> allSnapshots = snapshotRepository.findByUserIdOrderByReconciliationDateDesc(userId);
        boolean hasSnapshots = !allSnapshots.isEmpty();
        
        // 计算时间范围
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;
        
        if (hasSnapshots) {
            // 有快照记录，从快照表获取日期范围
            switch (period) {
                case "6m":
                    startDate = endDate.minusMonths(5).withDayOfMonth(1); // 最近6个月（包含当前月）
                    break;
                case "1y":
                    startDate = endDate.minusMonths(11).withDayOfMonth(1); // 最近12个月
                    break;
                case "3y":
                    startDate = endDate.minusMonths(35).withDayOfMonth(1); // 最近36个月
                    break;
                case "all":
                    // 找到最早的对账日期
                    Optional<LocalDate> earliestDate = allSnapshots.stream()
                            .map(ReconciliationSnapshot::getReconciliationDate)
                            .min(Comparator.naturalOrder());
                    startDate = earliestDate.orElse(endDate).withDayOfMonth(1);
                    break;
                default:
                    startDate = endDate.minusMonths(11).withDayOfMonth(1);
            }
        } else {
            // 没有快照记录，从存款表获取日期范围
            List<Deposit> allDeposits = depositRepository.findByUserIdOrderByReconciliationDateDesc(userId);
            if (allDeposits.isEmpty()) {
                return new TrendStatisticsResponse(period, new ArrayList<>());
            }
            
            switch (period) {
                case "6m":
                    startDate = endDate.minusMonths(5).withDayOfMonth(1);
                    break;
                case "1y":
                    startDate = endDate.minusMonths(11).withDayOfMonth(1);
                    break;
                case "3y":
                    startDate = endDate.minusMonths(35).withDayOfMonth(1);
                    break;
                case "all":
                    Optional<LocalDate> earliestDate = allDeposits.stream()
                            .map(Deposit::getReconciliationDate)
                            .min(Comparator.naturalOrder());
                    startDate = earliestDate.orElse(endDate).withDayOfMonth(1);
                    break;
                default:
                    startDate = endDate.minusMonths(11).withDayOfMonth(1);
            }
        }
        
        // 获取每个月的最后一次对账日期
        Map<String, LocalDate> monthlyLastDates;
        if (hasSnapshots) {
            // 从快照表获取
            monthlyLastDates = allSnapshots.stream()
                    .filter(s -> !s.getReconciliationDate().isBefore(startDate))
                    .collect(Collectors.groupingBy(
                        s -> s.getReconciliationDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> list.stream()
                                    .map(ReconciliationSnapshot::getReconciliationDate)
                                    .max(Comparator.naturalOrder())
                                    .orElse(null)
                        )
                    ));
        } else {
            // 从存款表获取（兼容历史数据）
            List<Deposit> depositsForDates = depositRepository.findByUserIdOrderByReconciliationDateDesc(userId);
            monthlyLastDates = depositsForDates.stream()
                    .filter(d -> !d.getReconciliationDate().isBefore(startDate))
                    .collect(Collectors.groupingBy(
                        d -> d.getReconciliationDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> list.stream()
                                    .map(Deposit::getReconciliationDate)
                                    .max(Comparator.naturalOrder())
                                    .orElse(null)
                        )
                    ));
        }
        
        // 生成所有月份列表
        List<String> allMonths = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            allMonths.add(current.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            current = current.plusMonths(1);
        }
        
        // 获取每个月的总额和备注
        List<TrendStatisticsResponse.TrendDataItem> data = new ArrayList<>();
        BigDecimal lastAmount = BigDecimal.ZERO; // 用于存储前一个月的值
        
        // 按月份分组获取该月的所有快照（用于收集备注）
        Map<String, List<ReconciliationSnapshot>> monthlySnapshots;
        if (hasSnapshots) {
            monthlySnapshots = allSnapshots.stream()
                    .filter(s -> !s.getReconciliationDate().isBefore(startDate))
                    .collect(Collectors.groupingBy(
                        s -> s.getReconciliationDate().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    ));
        } else {
            monthlySnapshots = new java.util.HashMap<>();
        }
        
        for (String month : allMonths) {
            LocalDate lastDate = monthlyLastDates.get(month);
            BigDecimal monthAmount;
            List<String> notes = new ArrayList<>();
            
            if (lastDate != null) {
                if (hasSnapshots) {
                    // 优先从快照表获取总金额
                    Optional<ReconciliationSnapshot> lastSnapshot = snapshotRepository.findByUserIdAndReconciliationDate(userId, lastDate);
                    monthAmount = lastSnapshot
                            .map(ReconciliationSnapshot::getTotalAmount)
                            .orElseGet(() -> {
                                // 没有快照记录，实时计算
                                List<Deposit> monthDeposits = depositRepository.findByUserIdAndReconciliationDate(userId, lastDate);
                                return monthDeposits.stream()
                                        .map(Deposit::getAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                            });
                    
                    // 收集该月的快照备注
                    // 如果该月有任何快照有备注，则收集该月所有有备注的快照备注
                    List<ReconciliationSnapshot> monthSnapshots = monthlySnapshots.getOrDefault(month, new ArrayList<>());
                    boolean hasAnyNote = monthSnapshots.stream()
                            .anyMatch(s -> s.getNote() != null && !s.getNote().trim().isEmpty());
                    if (hasAnyNote) {
                        // 该月有快照有备注，收集该月所有有备注的快照备注
                        notes = monthSnapshots.stream()
                                .filter(s -> s.getNote() != null && !s.getNote().trim().isEmpty())
                                .sorted(Comparator.comparing(ReconciliationSnapshot::getReconciliationDate))
                                .map(s -> s.getReconciliationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ": " + s.getNote())
                                .collect(Collectors.toList());
                    }
                    // 如果该月没有任何快照有备注，notes 保持为空列表（不显示任何备注）
                } else {
                    // 从存款表实时计算总金额（兼容历史数据，没有快照备注）
                    List<Deposit> monthDeposits = depositRepository.findByUserIdAndReconciliationDate(userId, lastDate);
                    monthAmount = monthDeposits.stream()
                            .map(Deposit::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    // notes 保持为空列表（没有快照记录）
                }
                lastAmount = monthAmount; // 更新前一个月的值
            } else {
                // 该月没有记录，使用前一个月的值
                monthAmount = lastAmount;
                // notes 保持为空列表（没有快照记录）
            }
            
            data.add(new TrendStatisticsResponse.TrendDataItem(month, monthAmount, notes));
        }
        
        return new TrendStatisticsResponse(period, data);
    }

    /**
     * 账户趋势统计（堆叠面积图）
     */
    public AccountTrendStatisticsResponse getAccountTrendStatistics(Long userId, String period) {
        List<ReconciliationSnapshot> allSnapshots = snapshotRepository.findByUserIdOrderByReconciliationDateDesc(userId);
        boolean hasSnapshots = !allSnapshots.isEmpty();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        if (hasSnapshots) {
            switch (period) {
                case "6m":
                    startDate = endDate.minusMonths(5).withDayOfMonth(1);
                    break;
                case "1y":
                    startDate = endDate.minusMonths(11).withDayOfMonth(1);
                    break;
                case "3y":
                    startDate = endDate.minusMonths(35).withDayOfMonth(1);
                    break;
                case "all":
                    Optional<LocalDate> earliestDate = allSnapshots.stream()
                            .map(ReconciliationSnapshot::getReconciliationDate)
                            .min(Comparator.naturalOrder());
                    startDate = earliestDate.orElse(endDate).withDayOfMonth(1);
                    break;
                default:
                    startDate = endDate.minusMonths(11).withDayOfMonth(1);
            }
        } else {
            List<Deposit> allDeposits = depositRepository.findByUserIdOrderByReconciliationDateDesc(userId);
            if (allDeposits.isEmpty()) {
                return new AccountTrendStatisticsResponse(period, new ArrayList<>(), new ArrayList<>());
            }

            switch (period) {
                case "6m":
                    startDate = endDate.minusMonths(5).withDayOfMonth(1);
                    break;
                case "1y":
                    startDate = endDate.minusMonths(11).withDayOfMonth(1);
                    break;
                case "3y":
                    startDate = endDate.minusMonths(35).withDayOfMonth(1);
                    break;
                case "all":
                    Optional<LocalDate> earliestDate = allDeposits.stream()
                            .map(Deposit::getReconciliationDate)
                            .min(Comparator.naturalOrder());
                    startDate = earliestDate.orElse(endDate).withDayOfMonth(1);
                    break;
                default:
                    startDate = endDate.minusMonths(11).withDayOfMonth(1);
            }
        }

        Map<String, LocalDate> monthlyLastDates;
        if (hasSnapshots) {
            monthlyLastDates = allSnapshots.stream()
                    .filter(s -> !s.getReconciliationDate().isBefore(startDate))
                    .collect(Collectors.groupingBy(
                            s -> s.getReconciliationDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    list -> list.stream()
                                            .map(ReconciliationSnapshot::getReconciliationDate)
                                            .max(Comparator.naturalOrder())
                                            .orElse(null)
                            )
                    ));
        } else {
            List<Deposit> depositsForDates = depositRepository.findByUserIdOrderByReconciliationDateDesc(userId);
            monthlyLastDates = depositsForDates.stream()
                    .filter(d -> !d.getReconciliationDate().isBefore(startDate))
                    .collect(Collectors.groupingBy(
                            d -> d.getReconciliationDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    list -> list.stream()
                                            .map(Deposit::getReconciliationDate)
                                            .max(Comparator.naturalOrder())
                                            .orElse(null)
                            )
                    ));
        }

        List<String> allMonths = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            allMonths.add(current.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            current = current.plusMonths(1);
        }

        List<Account> accounts = accountRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (accounts.isEmpty()) {
            return new AccountTrendStatisticsResponse(period, allMonths, new ArrayList<>());
        }

        Map<Long, List<BigDecimal>> accountMonthlyAmounts = new LinkedHashMap<>();
        Map<Long, BigDecimal> lastAmounts = new HashMap<>();
        for (Account account : accounts) {
            accountMonthlyAmounts.put(account.getId(), new ArrayList<>());
            lastAmounts.put(account.getId(), BigDecimal.ZERO);
        }

        for (String month : allMonths) {
            LocalDate lastDate = monthlyLastDates.get(month);
            if (lastDate != null) {
                List<Deposit> monthDeposits = depositRepository.findByUserIdAndReconciliationDate(userId, lastDate);
                Map<Long, BigDecimal> monthSums = monthDeposits.stream()
                        .collect(Collectors.groupingBy(
                                Deposit::getAccountId,
                                Collectors.mapping(Deposit::getAmount,
                                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                        ));

                for (Account account : accounts) {
                    BigDecimal amount = monthSums.getOrDefault(account.getId(), BigDecimal.ZERO);
                    lastAmounts.put(account.getId(), amount);
                    accountMonthlyAmounts.get(account.getId()).add(amount);
                }
            } else {
                for (Account account : accounts) {
                    accountMonthlyAmounts.get(account.getId()).add(lastAmounts.get(account.getId()));
                }
            }
        }

        List<AccountTrendStatisticsResponse.AccountSeries> series = new ArrayList<>();
        for (Account account : accounts) {
            series.add(new AccountTrendStatisticsResponse.AccountSeries(
                    account.getId(),
                    account.getName(),
                    accountMonthlyAmounts.get(account.getId())
            ));
        }

        return new AccountTrendStatisticsResponse(period, allMonths, series);
    }
    
    /**
     * 年度统计：统计每年的资产变化增值
     */
    public YearlyStatisticsResponse getYearlyStatistics(Long userId) {
        // 获取所有快照记录，按日期从早到晚排序
        List<ReconciliationSnapshot> allSnapshots = snapshotRepository.findByUserIdOrderByReconciliationDateDesc(userId);
        if (allSnapshots.isEmpty()) {
            // 没有快照记录，返回空列表
            return new YearlyStatisticsResponse(new ArrayList<>());
        }
        
        // 按日期从早到晚排序
        allSnapshots.sort(Comparator.comparing(ReconciliationSnapshot::getReconciliationDate));
        
        // 找到第一年（最早的快照所在的年份）
        LocalDate earliestDate = allSnapshots.get(0).getReconciliationDate();
        int firstYear = earliestDate.getYear();
        
        // 找到最晚的日期，确定年份范围
        LocalDate latestDate = allSnapshots.get(allSnapshots.size() - 1).getReconciliationDate();
        int lastYear = latestDate.getYear();
        
        // 按年份分组，获取每年的第一次和最后一次快照
        Map<Integer, List<ReconciliationSnapshot>> snapshotsByYear = allSnapshots.stream()
                .collect(Collectors.groupingBy(s -> s.getReconciliationDate().getYear()));
        
        // 生成所有年份列表（从第一年到最后一年）
        List<Integer> allYears = new ArrayList<>();
        for (int year = firstYear; year <= lastYear; year++) {
            allYears.add(year);
        }
        
        // 计算每年的增值
        List<YearlyStatisticsResponse.YearlyDataItem> data = new ArrayList<>();
        BigDecimal previousYearLastAmount = null; // 上一年最后一次快照的总额
        
        for (int year : allYears) {
            List<ReconciliationSnapshot> yearSnapshots = snapshotsByYear.get(year);
            BigDecimal increase;
            
            if (yearSnapshots == null || yearSnapshots.isEmpty()) {
                // 该年没有快照记录，增值为0
                increase = BigDecimal.ZERO;
                // lastYearEndAmount 保持不变，继续用于后续年份的计算
            } else {
                // 获取该年的第一次和最后一次快照
                ReconciliationSnapshot firstSnapshot = yearSnapshots.get(0);
                ReconciliationSnapshot lastSnapshot = yearSnapshots.get(yearSnapshots.size() - 1);
                
                if (year == firstYear) {
                    // 第一年：最后一次 - 第一次
                    // 特殊情况：如果第一年只有一次快照，增值 = totalAmount（视为从0开始）
                    if (firstSnapshot.getId().equals(lastSnapshot.getId())) {
                        increase = lastSnapshot.getTotalAmount();
                    } else {
                        increase = lastSnapshot.getTotalAmount().subtract(firstSnapshot.getTotalAmount());
                    }
                } else {
                    // 第二年开始：该年最后一次 - 上一年最后一次
                    if (previousYearLastAmount != null) {
                        // 上一年有快照（或更早的年份有快照），直接用上一年最后一次
                        increase = lastSnapshot.getTotalAmount().subtract(previousYearLastAmount);
                    } else {
                        // 上一年没有快照，需要查找前一年（或更早）的最后一次快照
                        Optional<ReconciliationSnapshot> previousYearLastSnapshot = findLastSnapshotBeforeYear(userId, year, allSnapshots);
                        if (previousYearLastSnapshot.isPresent()) {
                            BigDecimal previousYearAmount = previousYearLastSnapshot.get().getTotalAmount();
                            increase = lastSnapshot.getTotalAmount().subtract(previousYearAmount);
                            previousYearLastAmount = previousYearAmount; // 更新上一年最后一次
                        } else {
                            // 找不到前一年的快照（这种情况理论上不应该发生，因为第一年一定有快照）
                            // 使用该年的第一次快照作为起始值（相当于这一年从0开始）
                            increase = lastSnapshot.getTotalAmount().subtract(firstSnapshot.getTotalAmount());
                        }
                    }
                }
                
                // 更新上一年最后一次快照的总额（用于下一年计算）
                previousYearLastAmount = lastSnapshot.getTotalAmount();
            }
            
            data.add(new YearlyStatisticsResponse.YearlyDataItem(
                String.valueOf(year),
                increase
            ));
        }
        
        return new YearlyStatisticsResponse(data);
    }
    
    /**
     * 查找指定年份之前最近的一次快照（用于计算跨年增值）
     */
    private Optional<ReconciliationSnapshot> findLastSnapshotBeforeYear(Long userId, int year, List<ReconciliationSnapshot> allSnapshots) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        return allSnapshots.stream()
                .filter(s -> s.getReconciliationDate().isBefore(yearStart))
                .max(Comparator.comparing(ReconciliationSnapshot::getReconciliationDate));
    }
    
    /**
     * 向前查找最近的对账日期
     */
    private LocalDate findLatestDateBefore(Long userId, LocalDate beforeDate) {
        // 优先从快照表获取，如果没有则从存款表获取（兼容历史数据）
        List<ReconciliationSnapshot> allSnapshots = snapshotRepository.findByUserIdOrderByReconciliationDateDesc(userId);
        List<LocalDate> uniqueDates;
        
        if (!allSnapshots.isEmpty()) {
            // 从快照表获取日期
            uniqueDates = allSnapshots.stream()
                    .map(ReconciliationSnapshot::getReconciliationDate)
                    .distinct()
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
        } else {
            // 从存款表获取日期（兼容历史数据）
            uniqueDates = depositRepository.findByUserIdOrderByReconciliationDateDesc(userId)
                    .stream()
                    .map(Deposit::getReconciliationDate)
                    .distinct()
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
        }
        
        // 找到第一个小于beforeDate的日期
        return uniqueDates.stream()
                .filter(date -> date.isBefore(beforeDate))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 到期统计：统计最近1年内到期的定期存款
     */
    public MaturityStatisticsResponse getMaturityStatistics(Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate oneYearLater = now.plusDays(365);
        
        // 获取最近一次快照日期
        LocalDate latestSnapshotDate = getLatestSnapshotDate(userId);
        if (latestSnapshotDate == null) {
            // 没有快照记录，返回空列表
            return new MaturityStatisticsResponse(new ArrayList<>());
        }
        
        // 获取最近一次快照的所有存款记录
        List<Deposit> deposits = depositRepository.findByUserIdAndReconciliationDate(userId, latestSnapshotDate);
        
        // 获取相关账户信息
        Set<Long> accountIds = deposits.stream().map(Deposit::getAccountId).collect(Collectors.toSet());
        List<Account> accounts = accountRepository.findAllById(accountIds);
        Map<Long, String> accountNameMap = accounts.stream()
                .collect(Collectors.toMap(Account::getId, Account::getName));
        
        // 筛选定期存款并计算到期信息
        List<MaturityStatisticsResponse.MaturityDataItem> maturityData = deposits.stream()
                .filter(deposit -> "定期".equals(deposit.getDepositType()) && deposit.getTerm() != null)
                .map(deposit -> {
                    // 计算到期时间：存款时间 + 存期（年）* 365天
                    LocalDate maturityDate = deposit.getDepositTime().plusDays(
                            deposit.getTerm().multiply(new BigDecimal("365")).longValue()
                    );
                    
                    // 计算剩余天数
                    long remainingDays = ChronoUnit.DAYS.between(now, maturityDate);
                    
                    // 只返回1年内到期且未过期的
                    if (remainingDays >= 0 && maturityDate.isBefore(oneYearLater.plusDays(1))) {
                        String accountName = accountNameMap.get(deposit.getAccountId());
                        return new MaturityStatisticsResponse.MaturityDataItem(
                                accountName != null ? accountName : "未知账户",
                                deposit.getAmount(),
                                deposit.getDepositTime(),
                                maturityDate,
                                remainingDays
                        );
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(MaturityStatisticsResponse.MaturityDataItem::getMaturityDate))
                .collect(Collectors.toList());
        
        return new MaturityStatisticsResponse(maturityData);
    }
    
    /**
     * 获取最近一次快照日期
     */
    private LocalDate getLatestSnapshotDate(Long userId) {
        return snapshotRepository.findByUserIdOrderByReconciliationDateDesc(userId)
                .stream()
                .findFirst()
                .map(ReconciliationSnapshot::getReconciliationDate)
                .orElse(null);
    }
}
