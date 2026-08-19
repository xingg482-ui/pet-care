package com.example.petcare.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcare.boarding.BoardingCareTask;
import com.example.petcare.boarding.BoardingCareTaskMapper;
import com.example.petcare.customer.Customer;
import com.example.petcare.customer.CustomerMapper;
import com.example.petcare.boarding.BoardingLocation;
import com.example.petcare.boarding.BoardingLocationMapper;
import com.example.petcare.boarding.BoardingOrder;
import com.example.petcare.boarding.BoardingOrderMapper;
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
import java.time.LocalDateTime;
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
    private static final String PAID = "PAID";
    private static final String UNPAID = "UNPAID";
    private static final Set<String> ACTIVE_STATUSES = Set.of("PENDING", "CONFIRMED", "IN_SERVICE");
    private static final Set<String> EXCLUDED_FINANCE_STATUSES = Set.of("REJECTED", "CANCELLED");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CustomerMapper customerMapper;
    private final PetMapper petMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final ServiceOrderMapper orderMapper;
    private final ServiceOrderItemMapper orderItemMapper;
    private final BoardingLocationMapper boardingLocationMapper;
    private final BoardingOrderMapper boardingOrderMapper;
    private final BoardingCareTaskMapper boardingCareTaskMapper;

    public DashboardService(
            CustomerMapper customerMapper,
            PetMapper petMapper,
            ServiceItemMapper serviceItemMapper,
            ServiceOrderMapper orderMapper,
            ServiceOrderItemMapper orderItemMapper,
            BoardingLocationMapper boardingLocationMapper,
            BoardingOrderMapper boardingOrderMapper,
            BoardingCareTaskMapper boardingCareTaskMapper
    ) {
        this.customerMapper = customerMapper;
        this.petMapper = petMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.boardingLocationMapper = boardingLocationMapper;
        this.boardingOrderMapper = boardingOrderMapper;
        this.boardingCareTaskMapper = boardingCareTaskMapper;
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
                        .le(ServiceOrder::getAppointmentTime, todayEnd)),
                boardingLocationMapper.selectList(new LambdaQueryWrapper<BoardingLocation>()
                        .eq(BoardingLocation::getStatus, "ENABLED")
                        .eq(BoardingLocation::getCleanStatus, "CLEAN"))
                        .stream()
                        .map(BoardingLocation::getCapacity)
                        .filter(capacity -> capacity != null)
                        .mapToLong(Integer::longValue)
                        .sum()
        );
    }

    public List<DashboardTrendPoint> revenueTrend() {
        LocalDate startDate = LocalDate.now().minusDays(6);
        String start = startDate.atStartOfDay().format(DATE_TIME_FORMATTER);
        String end = LocalDate.now().atTime(23, 59, 59).format(DATE_TIME_FORMATTER);
        List<ServiceOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ServiceOrder>()
                .ge(ServiceOrder::getCreatedAt, start)
                .le(ServiceOrder::getCreatedAt, end)
                .eq(ServiceOrder::getPaymentStatus, PAID)
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
        String start = startDate.atStartOfDay().format(DATE_TIME_FORMATTER);
        String end = LocalDate.now().atTime(23, 59, 59).format(DATE_TIME_FORMATTER);
        List<ServiceOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ServiceOrder>()
                .ge(ServiceOrder::getCreatedAt, start)
                .le(ServiceOrder::getCreatedAt, end)
                .eq(ServiceOrder::getPaymentStatus, PAID)
                .notIn(ServiceOrder::getStatus, EXCLUDED_FINANCE_STATUSES));
        List<DashboardNameValue> stats = new java.util.ArrayList<>();
        if (!orders.isEmpty()) {
            List<Long> orderIds = orders.stream().map(ServiceOrder::getId).toList();
            stats.addAll(orderItemMapper.selectList(new LambdaQueryWrapper<ServiceOrderItem>().in(ServiceOrderItem::getOrderId, orderIds))
                    .stream()
                    .collect(Collectors.groupingBy(ServiceOrderItem::getServiceName))
                    .entrySet()
                    .stream()
                    .map(entry -> new DashboardNameValue(
                            entry.getKey(),
                            entry.getValue().stream().map(item -> value(item.getSubtotal())).reduce(BigDecimal.ZERO, BigDecimal::add)
                    ))
                    .toList());
        }

        return stats.stream()
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
                .notIn(ServiceOrder::getStatus, EXCLUDED_FINANCE_STATUSES)
                .orderByAsc(ServiceOrder::getAppointmentTime))
                .stream()
                .filter(this::isTodayUnfinishedServiceOrder)
                .toList();
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

    public List<DashboardTodayTask> todayTasks() {
        List<DashboardTodayTask> tasks = new java.util.ArrayList<>();
        tasks.addAll(todayServiceTasks());
        tasks.addAll(todayBoardingCheckInTasks());
        tasks.addAll(todayBoardingPickUpTasks());
        tasks.addAll(unpaidServicePaymentTasks());
        tasks.addAll(todayBoardingPaymentTasks());
        tasks.addAll(todayCareTasks());
        return tasks.stream()
                .sorted(this::compareDashboardTasks)
                .toList();
    }

    private List<DashboardTodayTask> todayServiceTasks() {
        String todayStart = LocalDate.now().atStartOfDay().format(DATE_TIME_FORMATTER);
        String todayEnd = LocalDate.now().atTime(23, 59, 59).format(DATE_TIME_FORMATTER);
        List<ServiceOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ServiceOrder>()
                .ge(ServiceOrder::getAppointmentTime, todayStart)
                .le(ServiceOrder::getAppointmentTime, todayEnd)
                .notIn(ServiceOrder::getStatus, EXCLUDED_FINANCE_STATUSES)
                .orderByAsc(ServiceOrder::getAppointmentTime))
                .stream()
                .filter(order -> ACTIVE_STATUSES.contains(order.getStatus()))
                .toList();
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
                .map(order -> {
                    Customer customer = customers.get(order.getCustomerId());
                    Pet pet = pets.get(order.getPetId());
                    return new DashboardTodayTask(
                            "SERVICE_ORDER",
                            order.getId(),
                            order.getOrderNo(),
                            customerName(customer),
                            customer == null ? "" : customer.getPhone(),
                            petName(pet),
                            serviceNames.getOrDefault(order.getId(), "-"),
                            order.getAppointmentTime(),
                            order.getStatus(),
                            order.getPaymentStatus(),
                            order.getTotalAmount(),
                            "/orders/" + order.getId()
                    );
                })
                .toList();
    }

    private List<DashboardTodayTask> todayBoardingCheckInTasks() {
        String todayEnd = LocalDate.now().atTime(23, 59, 59).format(DATE_TIME_FORMATTER);
        List<BoardingOrder> orders = boardingOrderMapper.selectList(new LambdaQueryWrapper<BoardingOrder>()
                .eq(BoardingOrder::getStatus, "RESERVED")
                .le(BoardingOrder::getPlannedCheckInTime, todayEnd)
                .orderByAsc(BoardingOrder::getPlannedCheckInTime));
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Customer> customers = customerMapper.selectByIds(orders.stream().map(BoardingOrder::getCustomerId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        Map<Long, Pet> pets = petMapper.selectByIds(orders.stream().map(BoardingOrder::getPetId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Pet::getId, Function.identity()));
        return orders.stream()
                .map(order -> {
                    Customer customer = customers.get(order.getCustomerId());
                    Pet pet = pets.get(order.getPetId());
                    return new DashboardTodayTask(
                            "BOARDING_CHECK_IN",
                            order.getId(),
                            order.getBoardingNo(),
                            customerName(customer),
                            customer == null ? "" : customer.getPhone(),
                            petName(pet),
                            "宠物托管开始服务",
                            order.getPlannedCheckInTime(),
                            order.getStatus(),
                            order.getPaymentStatus(),
                            order.getTotalAmount(),
                            "/boarding"
                    );
                })
                .toList();
    }

    private List<DashboardTodayTask> todayBoardingPickUpTasks() {
        String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        List<BoardingOrder> orders = boardingOrderMapper.selectList(new LambdaQueryWrapper<BoardingOrder>()
                .eq(BoardingOrder::getStatus, "CHECKED_IN")
                .le(BoardingOrder::getPlannedCheckOutTime, now)
                .orderByAsc(BoardingOrder::getPlannedCheckOutTime));
        return toBoardingTasks(orders, "BOARDING_PICK_UP", "确认客户已接回", false);
    }

    private List<DashboardTodayTask> todayBoardingPaymentTasks() {
        List<BoardingOrder> orders = boardingOrderMapper.selectList(new LambdaQueryWrapper<BoardingOrder>()
                .eq(BoardingOrder::getStatus, "COMPLETED")
                .and(wrapper -> wrapper.eq(BoardingOrder::getPaymentStatus, UNPAID).or().isNull(BoardingOrder::getPaymentStatus))
                .orderByAsc(BoardingOrder::getActualCheckOutTime)
                .orderByAsc(BoardingOrder::getPlannedCheckOutTime));
        return toBoardingTasks(orders, "BOARDING_PAYMENT", "托管待确认支付", true);
    }

    private List<DashboardTodayTask> unpaidServicePaymentTasks() {
        List<ServiceOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getStatus, "COMPLETED")
                .and(wrapper -> wrapper.eq(ServiceOrder::getPaymentStatus, UNPAID).or().isNull(ServiceOrder::getPaymentStatus))
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
                .map(order -> {
                    Customer customer = customers.get(order.getCustomerId());
                    Pet pet = pets.get(order.getPetId());
                    return new DashboardTodayTask(
                            "SERVICE_PAYMENT",
                            order.getId(),
                            order.getOrderNo(),
                            customerName(customer),
                            customer == null ? "" : customer.getPhone(),
                            petName(pet),
                            serviceNames.getOrDefault(order.getId(), "服务订单待确认支付"),
                            order.getUpdatedAt(),
                            order.getStatus(),
                            order.getPaymentStatus(),
                            order.getTotalAmount(),
                            "/orders/" + order.getId()
                    );
                })
                .toList();
    }

    private int compareDashboardTasks(DashboardTodayTask left, DashboardTodayTask right) {
        int priorityCompare = Integer.compare(taskPriority(left), taskPriority(right));
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        if (isPaymentTask(left)) {
            int timeCompare = safeText(right.taskTime()).compareTo(safeText(left.taskTime()));
            if (timeCompare != 0) {
                return timeCompare;
            }
        } else {
            int timeCompare = safeText(left.taskTime()).compareTo(safeText(right.taskTime()));
            if (timeCompare != 0) {
                return timeCompare;
            }
        }
        return safeText(left.taskNo()).compareTo(safeText(right.taskNo()));
    }

    private int taskPriority(DashboardTodayTask task) {
        if (isPaymentTask(task)) {
            return 1;
        }
        if ("SERVICE_ORDER".equals(task.taskType()) || "BOARDING_CHECK_IN".equals(task.taskType()) || "BOARDING_PICK_UP".equals(task.taskType())) {
            return 2;
        }
        if ("BOARDING_CARE".equals(task.taskType())) {
            return 3;
        }
        return 9;
    }

    private boolean isPaymentTask(DashboardTodayTask task) {
        return "SERVICE_PAYMENT".equals(task.taskType()) || "BOARDING_PAYMENT".equals(task.taskType());
    }

    private List<DashboardTodayTask> toBoardingTasks(List<BoardingOrder> orders, String taskType, String subject, boolean useActualCheckOutTime) {
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Customer> customers = customerMapper.selectByIds(orders.stream().map(BoardingOrder::getCustomerId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        Map<Long, Pet> pets = petMapper.selectByIds(orders.stream().map(BoardingOrder::getPetId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Pet::getId, Function.identity()));
        return orders.stream()
                .map(order -> {
                    Customer customer = customers.get(order.getCustomerId());
                    Pet pet = pets.get(order.getPetId());
                    String taskTime = useActualCheckOutTime && order.getActualCheckOutTime() != null
                            ? order.getActualCheckOutTime()
                            : order.getPlannedCheckOutTime();
                    return new DashboardTodayTask(
                            taskType,
                            order.getId(),
                            order.getBoardingNo(),
                            customerName(customer),
                            customer == null ? "" : customer.getPhone(),
                            petName(pet),
                            subject,
                            taskTime,
                            order.getStatus(),
                            order.getPaymentStatus(),
                            order.getTotalAmount(),
                            "/boarding"
                    );
                })
                .toList();
    }

    private List<DashboardTodayTask> todayCareTasks() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        List<BoardingCareTask> careTasks = boardingCareTaskMapper.selectList(new LambdaQueryWrapper<BoardingCareTask>()
                .eq(BoardingCareTask::getTaskDate, today)
                .eq(BoardingCareTask::getStatus, "PENDING")
                .orderByAsc(BoardingCareTask::getTaskTime));
        if (careTasks.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, BoardingOrder> boardingOrders = boardingOrderMapper.selectByIds(careTasks.stream().map(BoardingCareTask::getBoardingOrderId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(BoardingOrder::getId, Function.identity()));
        Map<Long, Customer> customers = customerMapper.selectByIds(boardingOrders.values().stream().map(BoardingOrder::getCustomerId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        Map<Long, Pet> pets = petMapper.selectByIds(boardingOrders.values().stream().map(BoardingOrder::getPetId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Pet::getId, Function.identity()));
        return careTasks.stream()
                .map(task -> {
                    BoardingOrder order = boardingOrders.get(task.getBoardingOrderId());
                    Customer customer = order == null ? null : customers.get(order.getCustomerId());
                    Pet pet = order == null ? null : pets.get(order.getPetId());
                    return new DashboardTodayTask(
                            "BOARDING_CARE",
                            task.getId(),
                            order == null ? "-" : order.getBoardingNo(),
                            customerName(customer),
                            customer == null ? "" : customer.getPhone(),
                            petName(pet),
                            task.getTaskName(),
                            task.getTaskDate() + " " + task.getTaskTime() + ":00",
                            task.getStatus(),
                            null,
                            BigDecimal.ZERO,
                            "/boarding?tab=care"
                    );
                })
                .toList();
    }

    private boolean isTodayUnfinishedServiceOrder(ServiceOrder order) {
        if (ACTIVE_STATUSES.contains(order.getStatus())) {
            return true;
        }
        String paymentStatus = order.getPaymentStatus() == null ? UNPAID : order.getPaymentStatus();
        return "COMPLETED".equals(order.getStatus()) && !PAID.equals(paymentStatus);
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

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
