package com.example.petcare.finance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcare.boarding.BoardingOrder;
import com.example.petcare.boarding.BoardingOrderMapper;
import com.example.petcare.order.ServiceOrder;
import com.example.petcare.order.ServiceOrderItem;
import com.example.petcare.order.ServiceOrderItemMapper;
import com.example.petcare.order.ServiceOrderMapper;
import com.example.petcare.serviceitem.ServiceItem;
import com.example.petcare.serviceitem.ServiceItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FinanceService {

    private static final String PAID = "PAID";
    private static final String UNPAID = "UNPAID";
    private static final String COMPLETED = "COMPLETED";
    private static final Set<String> EXCLUDED_STATUSES = Set.of("REJECTED", "CANCELLED");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ServiceOrderMapper orderMapper;
    private final ServiceOrderItemMapper orderItemMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final BoardingOrderMapper boardingOrderMapper;

    public FinanceService(
            ServiceOrderMapper orderMapper,
            ServiceOrderItemMapper orderItemMapper,
            ServiceItemMapper serviceItemMapper,
            BoardingOrderMapper boardingOrderMapper
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.boardingOrderMapper = boardingOrderMapper;
    }

    public FinanceSummary summary() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate monthStart = today.withDayOfMonth(1);
        return new FinanceSummary(
                metric(weekStart.atStartOfDay(), LocalDateTime.now()),
                metric(monthStart.atStartOfDay(), LocalDateTime.now())
        );
    }

    public List<FinanceServiceItemStat> serviceItems(FinanceQuery query) {
        LocalDateTime start = parseStart(query.getStartDate());
        LocalDateTime end = parseEnd(query.getEndDate());
        List<ServiceOrder> orders = ordersBetween(start, end);
        List<FinanceServiceItemStat> stats = new java.util.ArrayList<>();
        if (orders.isEmpty()) {
            stats.addAll(boardingStats(start, end, query));
            return stats;
        }

        List<Long> orderIds = orders.stream().map(ServiceOrder::getId).toList();
        List<ServiceOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<ServiceOrderItem>()
                .in(ServiceOrderItem::getOrderId, orderIds));
        if (query.getServiceItemId() != null) {
            items = items.stream()
                    .filter(item -> query.getServiceItemId().equals(item.getServiceItemId()))
                    .toList();
        }
        if (!items.isEmpty()) {
            Map<Long, ServiceItem> serviceItemMap = serviceItemMapper.selectByIds(items.stream()
                            .map(ServiceOrderItem::getServiceItemId)
                            .collect(Collectors.toSet()))
                    .stream()
                    .collect(Collectors.toMap(ServiceItem::getId, Function.identity()));
            stats.addAll(items.stream()
                    .filter(item -> matchesCategory(item, serviceItemMap, query.getCategory()))
                    .collect(Collectors.groupingBy(ServiceOrderItem::getServiceItemId))
                    .entrySet()
                    .stream()
                    .map(entry -> buildStat(entry.getKey(), entry.getValue(), serviceItemMap.get(entry.getKey())))
                    .toList());
        }
        stats.addAll(boardingStats(start, end, query));
        return stats.stream()
                .sorted((left, right) -> right.profit().compareTo(left.profit()))
                .toList();
    }

    private FinanceMetric metric(LocalDateTime start, LocalDateTime end) {
        List<ServiceOrder> orders = ordersBetween(start, end);
        BigDecimal revenue = orders.stream()
                .map(order -> value(order.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cost = orders.stream()
                .map(order -> value(order.getTotalCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal profit = orders.stream()
                .map(order -> value(order.getTotalProfit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingAmount = pendingOrdersBetween(start, end).stream()
                .map(order -> value(order.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BoardingOrder> boardingOrders = boardingOrdersBetween(start, end);
        revenue = revenue.add(boardingOrders.stream().map(order -> value(order.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add));
        cost = cost.add(boardingOrders.stream().map(order -> value(order.getTotalCost())).reduce(BigDecimal.ZERO, BigDecimal::add));
        profit = profit.add(boardingOrders.stream().map(order -> value(order.getTotalProfit())).reduce(BigDecimal.ZERO, BigDecimal::add));
        return new FinanceMetric(revenue.add(pendingAmount), revenue, pendingAmount, revenue, cost, profit, rate(profit, revenue));
    }

    private List<ServiceOrder> ordersBetween(LocalDateTime start, LocalDateTime end) {
        return orderMapper.selectList(new LambdaQueryWrapper<ServiceOrder>()
                .ge(ServiceOrder::getCreatedAt, format(start))
                .le(ServiceOrder::getCreatedAt, format(end))
                .eq(ServiceOrder::getPaymentStatus, PAID)
                .notIn(ServiceOrder::getStatus, EXCLUDED_STATUSES));
    }

    private List<BoardingOrder> boardingOrdersBetween(LocalDateTime start, LocalDateTime end) {
        return Collections.emptyList();
    }

    private List<ServiceOrder> pendingOrdersBetween(LocalDateTime start, LocalDateTime end) {
        return orderMapper.selectList(new LambdaQueryWrapper<ServiceOrder>()
                .ge(ServiceOrder::getCreatedAt, format(start))
                .le(ServiceOrder::getCreatedAt, format(end))
                .eq(ServiceOrder::getPaymentStatus, UNPAID)
                .eq(ServiceOrder::getStatus, COMPLETED));
    }

    private FinanceServiceItemStat buildStat(Long serviceItemId, List<ServiceOrderItem> items, ServiceItem serviceItem) {
        BigDecimal revenue = items.stream()
                .map(item -> value(item.getSubtotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cost = items.stream()
                .map(item -> value(item.getCostSubtotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal profit = items.stream()
                .map(item -> value(item.getProfit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int quantity = items.stream()
                .map(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .reduce(0, Integer::sum);
        long orderCount = items.stream().map(ServiceOrderItem::getOrderId).distinct().count();
        String serviceName = serviceItem == null ? items.getFirst().getServiceName() : serviceItem.getName();
        String category = serviceItem == null ? "-" : serviceItem.getCategory();
        return new FinanceServiceItemStat(serviceItemId, serviceName, category, orderCount, quantity, revenue, cost, profit, rate(profit, revenue));
    }

    private List<FinanceServiceItemStat> boardingStats(LocalDateTime start, LocalDateTime end, FinanceQuery query) {
        if (query.getServiceItemId() != null || (StringUtils.hasText(query.getCategory()) && !"托管".equals(query.getCategory()))) {
            return Collections.emptyList();
        }
        List<BoardingOrder> orders = boardingOrdersBetween(start, end);
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        BigDecimal revenue = orders.stream().map(order -> value(order.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cost = orders.stream().map(order -> value(order.getTotalCost())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal profit = orders.stream().map(order -> value(order.getTotalProfit())).reduce(BigDecimal.ZERO, BigDecimal::add);
        int quantity = orders.stream().map(order -> order.getChargeDays() == null ? 0 : order.getChargeDays()).reduce(0, Integer::sum);
        return List.of(new FinanceServiceItemStat(0L, "宠物托管", "托管", (long) orders.size(), quantity, revenue, cost, profit, rate(profit, revenue)));
    }

    private boolean matchesCategory(ServiceOrderItem item, Map<Long, ServiceItem> serviceItemMap, String category) {
        if (!StringUtils.hasText(category)) {
            return true;
        }
        ServiceItem serviceItem = serviceItemMap.get(item.getServiceItemId());
        return serviceItem != null && category.equals(serviceItem.getCategory());
    }

    private LocalDateTime parseStart(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDate.now().withDayOfMonth(1).atStartOfDay();
        }
        return LocalDate.parse(value).atStartOfDay();
    }

    private LocalDateTime parseEnd(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }
        return LocalDate.parse(value).atTime(LocalTime.MAX);
    }

    private BigDecimal rate(BigDecimal profit, BigDecimal revenue) {
        if (revenue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return profit.multiply(BigDecimal.valueOf(100)).divide(revenue, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String format(LocalDateTime time) {
        return time.format(DATE_TIME_FORMATTER);
    }
}
