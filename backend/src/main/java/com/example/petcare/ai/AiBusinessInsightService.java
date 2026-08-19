package com.example.petcare.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcare.boarding.BoardingOrder;
import com.example.petcare.boarding.BoardingOrderMapper;
import com.example.petcare.customer.Customer;
import com.example.petcare.customer.CustomerMapper;
import com.example.petcare.finance.FinanceQuery;
import com.example.petcare.finance.FinanceService;
import com.example.petcare.finance.FinanceServiceItemStat;
import com.example.petcare.finance.FinanceSummary;
import com.example.petcare.order.ServiceOrder;
import com.example.petcare.order.ServiceOrderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiBusinessInsightService {

    private static final String PAID = "PAID";
    private static final Set<String> EXCLUDED_STATUSES = Set.of("REJECTED", "CANCELLED");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FinanceService financeService;
    private final ServiceOrderMapper serviceOrderMapper;
    private final BoardingOrderMapper boardingOrderMapper;
    private final CustomerMapper customerMapper;

    public AiBusinessInsightService(
            FinanceService financeService,
            ServiceOrderMapper serviceOrderMapper,
            BoardingOrderMapper boardingOrderMapper,
            CustomerMapper customerMapper
    ) {
        this.financeService = financeService;
        this.serviceOrderMapper = serviceOrderMapper;
        this.boardingOrderMapper = boardingOrderMapper;
        this.customerMapper = customerMapper;
    }

    public AiBusinessSummary summary() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29);
        String startAt = startDate.atStartOfDay().format(DATE_TIME_FORMATTER);
        String endAt = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        FinanceSummary financeSummary = financeService.summary();
        FinanceQuery financeQuery = new FinanceQuery();
        financeQuery.setStartDate(startDate.format(DATE_FORMATTER));
        financeQuery.setEndDate(today.format(DATE_FORMATTER));

        List<ServiceOrder> serviceOrders = serviceOrderMapper.selectList(new LambdaQueryWrapper<ServiceOrder>()
                .ge(ServiceOrder::getCreatedAt, startAt)
                .le(ServiceOrder::getCreatedAt, endAt)
                .notIn(ServiceOrder::getStatus, EXCLUDED_STATUSES));
        List<BoardingOrder> boardingOrders = boardingOrderMapper.selectList(new LambdaQueryWrapper<BoardingOrder>()
                .ge(BoardingOrder::getCreatedAt, startAt)
                .le(BoardingOrder::getCreatedAt, endAt)
                .notIn(BoardingOrder::getStatus, EXCLUDED_STATUSES));

        return new AiBusinessSummary(
                financeSummary.week(),
                financeSummary.month(),
                topCustomers(serviceOrders, boardingOrders),
                financeService.serviceItems(financeQuery).stream().limit(5).toList(),
                statusCounts(serviceOrders, boardingOrders),
                LocalDateTime.now().format(DATE_TIME_FORMATTER)
        );
    }

    public String promptContext() {
        AiBusinessSummary summary = summary();
        StringBuilder builder = new StringBuilder();
        builder.append("系统经营数据摘要，生成时间：").append(summary.generatedAt()).append("\n");
        builder.append("本周：营收 ").append(money(summary.week().revenue()))
                .append("，成本 ").append(money(summary.week().cost()))
                .append("，净利润 ").append(money(summary.week().profit()))
                .append("，利润率 ").append(percent(summary.week().profitRate())).append("\n");
        builder.append("本月：营收 ").append(money(summary.month().revenue()))
                .append("，成本 ").append(money(summary.month().cost()))
                .append("，净利润 ").append(money(summary.month().profit()))
                .append("，利润率 ").append(percent(summary.month().profitRate())).append("\n");

        builder.append("近 30 天优质客户候选：");
        if (summary.topCustomers().isEmpty()) {
            builder.append("暂无已支付客户数据");
        } else {
            builder.append(summary.topCustomers().stream()
                    .map(customer -> "%s，订单 %d，已支付 %s，利润 %s，最近订单 %s".formatted(
                            customer.customerName(),
                            customer.orderCount(),
                            money(customer.paidAmount()),
                            money(customer.profit()),
                            customer.lastOrderAt() == null ? "-" : customer.lastOrderAt()
                    ))
                    .collect(Collectors.joining("；")));
        }
        builder.append("\n");

        builder.append("近 30 天服务项目利润排行：");
        if (summary.topServiceItems().isEmpty()) {
            builder.append("暂无已支付服务项目数据");
        } else {
            builder.append(summary.topServiceItems().stream()
                    .map(item -> "%s/%s，订单 %d，营收 %s，成本 %s，利润 %s，利润率 %s".formatted(
                            item.serviceName(),
                            item.category(),
                            item.orderCount(),
                            money(item.revenue()),
                            money(item.cost()),
                            money(item.profit()),
                            percent(item.profitRate())
                    ))
                    .collect(Collectors.joining("；")));
        }
        builder.append("\n");

        builder.append("近 30 天订单状态：")
                .append(summary.orderStatusCounts().stream()
                        .map(item -> item.status() + " " + item.count())
                        .collect(Collectors.joining("，")));
        return builder.toString();
    }

    private List<AiCustomerInsight> topCustomers(List<ServiceOrder> serviceOrders, List<BoardingOrder> boardingOrders) {
        Map<Long, CustomerAccumulator> accumulators = new HashMap<>();
        serviceOrders.stream()
                .filter(order -> PAID.equals(order.getPaymentStatus()))
                .forEach(order -> accumulate(accumulators, order.getCustomerId(), order.getTotalAmount(), order.getTotalProfit(), order.getCreatedAt()));
        boardingOrders.stream()
                .filter(order -> PAID.equals(order.getPaymentStatus()))
                .forEach(order -> accumulate(accumulators, order.getCustomerId(), order.getTotalAmount(), order.getTotalProfit(), order.getCreatedAt()));
        if (accumulators.isEmpty()) {
            return List.of();
        }
        Map<Long, Customer> customerMap = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                        .in(Customer::getId, accumulators.keySet()))
                .stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        return accumulators.entrySet().stream()
                .map(entry -> {
                    Customer customer = customerMap.get(entry.getKey());
                    CustomerAccumulator value = entry.getValue();
                    return new AiCustomerInsight(
                            entry.getKey(),
                            customer == null ? "客户" + entry.getKey() : customer.getName(),
                            value.orderCount,
                            value.paidAmount,
                            value.profit,
                            value.lastOrderAt
                    );
                })
                .sorted(Comparator
                        .comparing(AiCustomerInsight::paidAmount, Comparator.reverseOrder())
                        .thenComparing(AiCustomerInsight::orderCount, Comparator.reverseOrder()))
                .limit(5)
                .toList();
    }

    private void accumulate(Map<Long, CustomerAccumulator> accumulators, Long customerId, BigDecimal amount, BigDecimal profit, String createdAt) {
        if (customerId == null) {
            return;
        }
        CustomerAccumulator accumulator = accumulators.computeIfAbsent(customerId, id -> new CustomerAccumulator());
        accumulator.orderCount += 1;
        accumulator.paidAmount = accumulator.paidAmount.add(value(amount));
        accumulator.profit = accumulator.profit.add(value(profit));
        if (accumulator.lastOrderAt == null || String.valueOf(createdAt).compareTo(accumulator.lastOrderAt) > 0) {
            accumulator.lastOrderAt = createdAt;
        }
    }

    private List<AiStatusCount> statusCounts(List<ServiceOrder> serviceOrders, List<BoardingOrder> boardingOrders) {
        Map<String, Long> counts = new LinkedHashMap<>();
        List<String> statuses = new ArrayList<>();
        serviceOrders.forEach(order -> statuses.add("服务-" + order.getStatus()));
        boardingOrders.forEach(order -> statuses.add("托管-" + order.getStatus()));
        statuses.forEach(status -> counts.put(status, counts.getOrDefault(status, 0L) + 1));
        return counts.entrySet().stream()
                .map(entry -> new AiStatusCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String money(BigDecimal value) {
        return "￥" + value(value).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String percent(BigDecimal value) {
        return value(value).setScale(2, java.math.RoundingMode.HALF_UP) + "%";
    }

    private static class CustomerAccumulator {
        private long orderCount;
        private BigDecimal paidAmount = BigDecimal.ZERO;
        private BigDecimal profit = BigDecimal.ZERO;
        private String lastOrderAt;
    }
}
