package com.example.petcare.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcare.customer.Customer;
import com.example.petcare.customer.CustomerMapper;
import com.example.petcare.order.ServiceOrderItem;
import com.example.petcare.order.ServiceOrderItemMapper;
import com.example.petcare.order.ServiceOrder;
import com.example.petcare.order.ServiceOrderMapper;
import com.example.petcare.pet.Pet;
import com.example.petcare.pet.PetMapper;
import com.example.petcare.serviceitem.ServiceItem;
import com.example.petcare.serviceitem.ServiceItemMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final String PENDING = "PENDING";
    private static final Set<String> ACTIVE_STATUSES = Set.of("PENDING", "CONFIRMED", "IN_SERVICE");
    private static final Set<String> EXCLUDED_FINANCE_STATUSES = Set.of("REJECTED", "CANCELLED");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CustomerMapper customerMapper;
    private final PetMapper petMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final ServiceOrderMapper orderMapper;
    private final ServiceOrderItemMapper orderItemMapper;

    public DashboardService(
            CustomerMapper customerMapper,
            PetMapper petMapper,
            ServiceItemMapper serviceItemMapper,
            ServiceOrderMapper orderMapper,
            ServiceOrderItemMapper orderItemMapper
    ) {
        this.customerMapper = customerMapper;
        this.petMapper = petMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    public DashboardSummary summary() {
        String todayStart = LocalDate.now().atStartOfDay().format(DATE_TIME_FORMATTER);
        String todayEnd = LocalDate.now().atTime(23, 59, 59).format(DATE_TIME_FORMATTER);
        return new DashboardSummary(
                customerMapper.selectCount(new LambdaQueryWrapper<Customer>()),
                petMapper.selectCount(new LambdaQueryWrapper<Pet>()),
                serviceItemMapper.selectCount(new LambdaQueryWrapper<ServiceItem>()),
                orderMapper.selectCount(new LambdaQueryWrapper<ServiceOrder>().eq(ServiceOrder::getStatus, PENDING)),
                orderMapper.selectCount(new LambdaQueryWrapper<ServiceOrder>()
                        .ge(ServiceOrder::getAppointmentTime, todayStart)
                        .le(ServiceOrder::getAppointmentTime, todayEnd))
        );
    }

    public List<DashboardTrendPoint> revenueTrend() {
        LocalDate startDate = LocalDate.now().minusDays(6);
        String start = startDate.atStartOfDay().format(DATE_TIME_FORMATTER);
        String end = LocalDate.now().atTime(23, 59, 59).format(DATE_TIME_FORMATTER);
        List<ServiceOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ServiceOrder>()
                .ge(ServiceOrder::getCreatedAt, start)
                .le(ServiceOrder::getCreatedAt, end)
                .notIn(ServiceOrder::getStatus, EXCLUDED_FINANCE_STATUSES));
        Map<String, List<ServiceOrder>> ordersByDate = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getCreatedAt().substring(0, 10)));

        return startDate.datesUntil(LocalDate.now().plusDays(1))
                .map(date -> {
                    List<ServiceOrder> dayOrders = ordersByDate.getOrDefault(date.format(DATE_FORMATTER), Collections.emptyList());
                    BigDecimal revenue = dayOrders.stream().map(order -> value(order.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal cost = dayOrders.stream().map(order -> value(order.getTotalCost())).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal profit = dayOrders.stream().map(order -> value(order.getTotalProfit())).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new DashboardTrendPoint(date.format(DATE_FORMATTER), revenue, cost, profit);
                })
                .toList();
    }

    public List<DashboardNameValue> serviceRevenue() {
        LocalDate startDate = LocalDate.now().minusDays(29);
        List<ServiceOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ServiceOrder>()
                .ge(ServiceOrder::getCreatedAt, startDate.atStartOfDay().format(DATE_TIME_FORMATTER))
                .le(ServiceOrder::getCreatedAt, LocalDate.now().atTime(23, 59, 59).format(DATE_TIME_FORMATTER))
                .notIn(ServiceOrder::getStatus, EXCLUDED_FINANCE_STATUSES));
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> orderIds = orders.stream().map(ServiceOrder::getId).toList();
        return orderItemMapper.selectList(new LambdaQueryWrapper<ServiceOrderItem>().in(ServiceOrderItem::getOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(ServiceOrderItem::getServiceName))
                .entrySet()
                .stream()
                .map(entry -> new DashboardNameValue(
                        entry.getKey(),
                        entry.getValue().stream().map(item -> value(item.getSubtotal())).reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .sorted((left, right) -> right.value().compareTo(left.value()))
                .toList();
    }

    public List<DashboardStatusStat> orderStatus() {
        return orderMapper.selectList(new LambdaQueryWrapper<ServiceOrder>())
                .stream()
                .collect(Collectors.groupingBy(ServiceOrder::getStatus, Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> new DashboardStatusStat(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(stat -> statusOrder(stat.status())))
                .toList();
    }

    public List<DashboardTodayActiveOrder> todayActiveOrders() {
        String todayStart = LocalDate.now().atStartOfDay().format(DATE_TIME_FORMATTER);
        String todayEnd = LocalDate.now().atTime(23, 59, 59).format(DATE_TIME_FORMATTER);
        List<ServiceOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ServiceOrder>()
                .ge(ServiceOrder::getAppointmentTime, todayStart)
                .le(ServiceOrder::getAppointmentTime, todayEnd)
                .in(ServiceOrder::getStatus, ACTIVE_STATUSES)
                .orderByAsc(ServiceOrder::getAppointmentTime));
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Customer> customers = customerMapper.selectByIds(orders.stream().map(ServiceOrder::getCustomerId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        Map<Long, Pet> pets = petMapper.selectByIds(orders.stream().map(ServiceOrder::getPetId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Pet::getId, Function.identity()));
        Map<Long, String> serviceNames = orderItemMapper.selectList(new LambdaQueryWrapper<ServiceOrderItem>()
                        .in(ServiceOrderItem::getOrderId, orders.stream().map(ServiceOrder::getId).toList()))
                .stream()
                .collect(Collectors.groupingBy(
                        ServiceOrderItem::getOrderId,
                        Collectors.mapping(ServiceOrderItem::getServiceName, Collectors.joining("、"))
                ));

        return orders.stream()
                .map(order -> new DashboardTodayActiveOrder(
                        order.getId(),
                        order.getOrderNo(),
                        customerName(customers.get(order.getCustomerId())),
                        petName(pets.get(order.getPetId())),
                        serviceNames.getOrDefault(order.getId(), "-"),
                        order.getAppointmentTime(),
                        order.getStatus(),
                        order.getTotalAmount()
                ))
                .toList();
    }

    private int statusOrder(String status) {
        return switch (status) {
            case "PENDING" -> 1;
            case "CONFIRMED" -> 2;
            case "IN_SERVICE" -> 3;
            case "COMPLETED" -> 4;
            case "REJECTED" -> 5;
            case "CANCELLED" -> 6;
            default -> 99;
        };
    }

    private String customerName(Customer customer) {
        return customer == null ? "-" : customer.getName();
    }

    private String petName(Pet pet) {
        return pet == null ? "-" : pet.getName();
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
