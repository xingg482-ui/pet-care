package com.example.petcare.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.petcare.common.PageResult;
import com.example.petcare.customer.Customer;
import com.example.petcare.customer.CustomerMapper;
import com.example.petcare.pet.Pet;
import com.example.petcare.pet.PetMapper;
import com.example.petcare.serviceitem.ServiceItem;
import com.example.petcare.serviceitem.ServiceItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final String ENABLED = "ENABLED";
    private static final String PENDING = "PENDING";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String REJECTED = "REJECTED";
    private static final String CANCELLED = "CANCELLED";
    private static final String IN_SERVICE = "IN_SERVICE";
    private static final String COMPLETED = "COMPLETED";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ServiceOrderMapper orderMapper;
    private final ServiceOrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final CustomerMapper customerMapper;
    private final PetMapper petMapper;
    private final ServiceItemMapper serviceItemMapper;

    public OrderService(
            ServiceOrderMapper orderMapper,
            ServiceOrderItemMapper orderItemMapper,
            OrderStatusLogMapper statusLogMapper,
            CustomerMapper customerMapper,
            PetMapper petMapper,
            ServiceItemMapper serviceItemMapper
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.statusLogMapper = statusLogMapper;
        this.customerMapper = customerMapper;
        this.petMapper = petMapper;
        this.serviceItemMapper = serviceItemMapper;
    }

    public PageResult<OrderListView> list(OrderQuery query) {
        LambdaQueryWrapper<ServiceOrder> wrapper = new LambdaQueryWrapper<ServiceOrder>()
                .like(StringUtils.hasText(query.getOrderNo()), ServiceOrder::getOrderNo, query.getOrderNo())
                .eq(StringUtils.hasText(query.getStatus()), ServiceOrder::getStatus, query.getStatus())
                .ge(StringUtils.hasText(query.getAppointmentStart()), ServiceOrder::getAppointmentTime, normalizeStart(query.getAppointmentStart()))
                .le(StringUtils.hasText(query.getAppointmentEnd()), ServiceOrder::getAppointmentTime, normalizeEnd(query.getAppointmentEnd()))
                .orderByDesc(ServiceOrder::getCreatedAt);

        applyCustomerNameFilter(wrapper, query.getCustomerName());
        applyPetNameFilter(wrapper, query.getPetName());

        Page<ServiceOrder> page = orderMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        Map<Long, Customer> customers = loadCustomers(page.getRecords().stream().map(ServiceOrder::getCustomerId).collect(Collectors.toSet()));
        Map<Long, Pet> pets = loadPets(page.getRecords().stream().map(ServiceOrder::getPetId).collect(Collectors.toSet()));
        return new PageResult<>(
                page.getRecords().stream()
                        .map(order -> OrderListView.of(order, customerName(customers.get(order.getCustomerId())), petName(pets.get(order.getPetId()))))
                        .toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    public OrderDetailView detail(Long id) {
        ServiceOrder order = getOrderOrThrow(id);
        Customer customer = customerMapper.selectById(order.getCustomerId());
        Pet pet = petMapper.selectById(order.getPetId());
        List<ServiceOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<ServiceOrderItem>()
                .eq(ServiceOrderItem::getOrderId, id)
                .orderByAsc(ServiceOrderItem::getId));
        List<OrderStatusLog> logs = statusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, id)
                .orderByAsc(OrderStatusLog::getCreatedAt));
        return new OrderDetailView(OrderListView.of(order, customerName(customer), petName(pet)), customer, pet, items, logs);
    }

    public List<OrderStatusLog> statusLogs(Long id) {
        getOrderOrThrow(id);
        return statusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, id)
                .orderByAsc(OrderStatusLog::getCreatedAt));
    }

    @Transactional
    public OrderDetailView create(OrderCreateRequest request) {
        Customer customer = requireEnabledCustomer(request.customerId());
        Pet pet = requireEnabledPet(request.petId());
        if (!customer.getId().equals(pet.getCustomerId())) {
            throw new IllegalArgumentException("宠物不属于所选客户");
        }
        LocalDateTime appointmentTime = parseFutureAppointmentTime(request.appointmentTime());
        List<ServiceItem> serviceItems = loadEnabledServiceItems(request.serviceItemIds());
        BigDecimal totalAmount = serviceItems.stream()
                .map(ServiceItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = serviceItems.stream()
                .map(this::serviceCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = totalAmount.subtract(totalCost);

        ServiceOrder order = new ServiceOrder();
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(customer.getId());
        order.setPetId(pet.getId());
        order.setAppointmentTime(format(appointmentTime));
        order.setStatus(PENDING);
        order.setTotalAmount(totalAmount);
        order.setTotalCost(totalCost);
        order.setTotalProfit(totalProfit);
        order.setRemark(request.remark());
        order.setCreatedAt(now());
        order.setUpdatedAt(now());
        orderMapper.insert(order);

        for (ServiceItem serviceItem : serviceItems) {
            ServiceOrderItem item = new ServiceOrderItem();
            item.setOrderId(order.getId());
            item.setServiceItemId(serviceItem.getId());
            item.setServiceName(serviceItem.getName());
            item.setUnitPrice(serviceItem.getPrice());
            item.setUnitCost(serviceCost(serviceItem));
            item.setQuantity(1);
            item.setSubtotal(serviceItem.getPrice());
            item.setCostSubtotal(serviceCost(serviceItem));
            item.setProfit(serviceItem.getPrice().subtract(serviceCost(serviceItem)));
            orderItemMapper.insert(item);
        }
        insertStatusLog(order.getId(), null, PENDING, "创建订单");
        return detail(order.getId());
    }

    @Transactional
    public OrderDetailView updateStatus(Long id, OrderStatusRequest request) {
        ServiceOrder order = getOrderOrThrow(id);
        String nextStatus = request.status();
        ensureValidTransition(order.getStatus(), nextStatus);
        orderMapper.update(new LambdaUpdateWrapper<ServiceOrder>()
                .eq(ServiceOrder::getId, id)
                .set(ServiceOrder::getStatus, nextStatus)
                .set(ServiceOrder::getUpdatedAt, now()));
        insertStatusLog(id, order.getStatus(), nextStatus, request.remark());
        return detail(id);
    }

    public OrderDetailView updateAppointmentTime(Long id, AppointmentTimeRequest request) {
        ServiceOrder order = getOrderOrThrow(id);
        if (!PENDING.equals(order.getStatus()) && !CONFIRMED.equals(order.getStatus())) {
            throw new IllegalArgumentException("当前状态不能修改预约时间");
        }
        LocalDateTime appointmentTime = parseFutureAppointmentTime(request.appointmentTime());
        orderMapper.update(new LambdaUpdateWrapper<ServiceOrder>()
                .eq(ServiceOrder::getId, id)
                .set(ServiceOrder::getAppointmentTime, format(appointmentTime))
                .set(ServiceOrder::getUpdatedAt, now()));
        return detail(id);
    }

    private void applyCustomerNameFilter(LambdaQueryWrapper<ServiceOrder> wrapper, String customerName) {
        if (!StringUtils.hasText(customerName)) {
            return;
        }
        List<Long> ids = customerMapper.selectList(new LambdaQueryWrapper<Customer>().like(Customer::getName, customerName))
                .stream().map(Customer::getId).toList();
        wrapper.in(!ids.isEmpty(), ServiceOrder::getCustomerId, ids);
        wrapper.eq(ids.isEmpty(), ServiceOrder::getCustomerId, -1L);
    }

    private void applyPetNameFilter(LambdaQueryWrapper<ServiceOrder> wrapper, String petName) {
        if (!StringUtils.hasText(petName)) {
            return;
        }
        List<Long> ids = petMapper.selectList(new LambdaQueryWrapper<Pet>().like(Pet::getName, petName))
                .stream().map(Pet::getId).toList();
        wrapper.in(!ids.isEmpty(), ServiceOrder::getPetId, ids);
        wrapper.eq(ids.isEmpty(), ServiceOrder::getPetId, -1L);
    }

    private Customer requireEnabledCustomer(Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        if (!ENABLED.equals(customer.getStatus())) {
            throw new IllegalArgumentException("停用客户不能创建订单");
        }
        return customer;
    }

    private Pet requireEnabledPet(Long petId) {
        Pet pet = petMapper.selectById(petId);
        if (pet == null) {
            throw new IllegalArgumentException("宠物不存在");
        }
        if (!ENABLED.equals(pet.getStatus())) {
            throw new IllegalArgumentException("停用宠物不能创建订单");
        }
        return pet;
    }

    private List<ServiceItem> loadEnabledServiceItems(List<Long> serviceItemIds) {
        List<Long> uniqueIds = serviceItemIds.stream().distinct().toList();
        List<ServiceItem> items = serviceItemMapper.selectByIds(uniqueIds);
        if (items.size() != uniqueIds.size()) {
            throw new IllegalArgumentException("服务项目不存在");
        }
        if (items.stream().anyMatch(item -> !ENABLED.equals(item.getStatus()))) {
            throw new IllegalArgumentException("停用服务项目不能创建订单");
        }
        return items;
    }

    private ServiceOrder getOrderOrThrow(Long id) {
        ServiceOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return order;
    }

    private void ensureValidTransition(String oldStatus, String nextStatus) {
        boolean valid = switch (oldStatus) {
            case PENDING -> CONFIRMED.equals(nextStatus) || REJECTED.equals(nextStatus) || CANCELLED.equals(nextStatus);
            case CONFIRMED -> IN_SERVICE.equals(nextStatus) || CANCELLED.equals(nextStatus);
            case IN_SERVICE -> COMPLETED.equals(nextStatus);
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("订单状态不允许这样流转");
        }
    }

    private void insertStatusLog(Long orderId, String oldStatus, String newStatus, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setOperator("admin");
        log.setRemark(remark);
        log.setCreatedAt(now());
        statusLogMapper.insert(log);
    }

    private Map<Long, Customer> loadCustomers(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return customerMapper.selectByIds(ids).stream().collect(Collectors.toMap(Customer::getId, Function.identity()));
    }

    private Map<Long, Pet> loadPets(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return petMapper.selectByIds(ids).stream().collect(Collectors.toMap(Pet::getId, Function.identity()));
    }

    private LocalDateTime parseFutureAppointmentTime(String value) {
        LocalDateTime time = LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        if (!time.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("预约时间必须晚于当前时间");
        }
        return time;
    }

    private String normalizeStart(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() == 10 ? LocalDate.parse(value).atStartOfDay().format(DATE_TIME_FORMATTER) : value;
    }

    private String normalizeEnd(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() == 10 ? LocalDate.parse(value).atTime(23, 59, 59).format(DATE_TIME_FORMATTER) : value;
    }

    private String generateOrderNo() {
        return "SO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String customerName(Customer customer) {
        return customer == null ? "-" : customer.getName();
    }

    private String petName(Pet pet) {
        return pet == null ? "-" : pet.getName();
    }

    private BigDecimal serviceCost(ServiceItem serviceItem) {
        return serviceItem.getCost() == null ? BigDecimal.ZERO : serviceItem.getCost();
    }

    private String format(LocalDateTime time) {
        return time.format(DATE_TIME_FORMATTER);
    }

    private String now() {
        return format(LocalDateTime.now());
    }
}
