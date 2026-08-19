package com.example.petcare.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.petcare.account.Account;
import com.example.petcare.account.AccountMapper;
import com.example.petcare.account.AccountPrincipal;
import com.example.petcare.account.AccountService;
import com.example.petcare.boarding.BoardingOrder;
import com.example.petcare.boarding.BoardingOrderMapper;
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
import java.util.stream.Stream;

@Service
public class OrderService {

    private static final String ENABLED = "ENABLED";
    private static final String PENDING = "PENDING";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String REJECTED = "REJECTED";
    private static final String CANCELLED = "CANCELLED";
    private static final String IN_SERVICE = "IN_SERVICE";
    private static final String COMPLETED = "COMPLETED";
    private static final String UNPAID = "UNPAID";
    private static final String PAID = "PAID";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ServiceOrderMapper orderMapper;
    private final ServiceOrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final CustomerMapper customerMapper;
    private final PetMapper petMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final BoardingOrderMapper boardingOrderMapper;
    private final PaymentRecordMapper paymentRecordMapper;

    public OrderService(
            ServiceOrderMapper orderMapper,
            ServiceOrderItemMapper orderItemMapper,
            OrderStatusLogMapper statusLogMapper,
            CustomerMapper customerMapper,
            PetMapper petMapper,
            ServiceItemMapper serviceItemMapper,
            AccountService accountService,
            AccountMapper accountMapper,
            BoardingOrderMapper boardingOrderMapper,
            PaymentRecordMapper paymentRecordMapper
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.statusLogMapper = statusLogMapper;
        this.customerMapper = customerMapper;
        this.petMapper = petMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.accountService = accountService;
        this.accountMapper = accountMapper;
        this.boardingOrderMapper = boardingOrderMapper;
        this.paymentRecordMapper = paymentRecordMapper;
    }

    public PageResult<OrderListView> list(OrderQuery query) {
        LambdaQueryWrapper<ServiceOrder> wrapper = new LambdaQueryWrapper<ServiceOrder>()
                .like(StringUtils.hasText(query.getOrderNo()), ServiceOrder::getOrderNo, query.getOrderNo())
                .eq(StringUtils.hasText(query.getStatus()), ServiceOrder::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getPaymentStatus()), ServiceOrder::getPaymentStatus, query.getPaymentStatus())
                .ge(StringUtils.hasText(query.getAppointmentStart()), ServiceOrder::getAppointmentTime, normalizeStart(query.getAppointmentStart()))
                .le(StringUtils.hasText(query.getAppointmentEnd()), ServiceOrder::getAppointmentTime, normalizeEnd(query.getAppointmentEnd()))
                .orderByDesc(ServiceOrder::getCreatedAt);

        applyCustomerNameFilter(wrapper, query.getCustomerName());
        applyPetNameFilter(wrapper, query.getPetName());

        List<ServiceOrder> serviceOrders = orderMapper.selectList(wrapper);
        List<BoardingOrder> boardingOrders = loadBoardingOrders(query);
        Map<Long, Customer> customers = loadCustomers(Stream.concat(
                        serviceOrders.stream().map(ServiceOrder::getCustomerId),
                        boardingOrders.stream().map(BoardingOrder::getCustomerId)
                )
                .collect(Collectors.toSet()));
        Map<Long, Pet> pets = loadPets(Stream.concat(
                        serviceOrders.stream().map(ServiceOrder::getPetId),
                        boardingOrders.stream().map(BoardingOrder::getPetId)
                )
                .collect(Collectors.toSet()));
        Map<Long, List<String>> serviceNames = loadServiceNames(serviceOrders.stream().map(ServiceOrder::getId).collect(Collectors.toSet()));
        Map<Long, String> paymentConfirmers = loadPaymentConfirmers("SERVICE", serviceOrders.stream().map(ServiceOrder::getId).collect(Collectors.toSet()));
        Map<Long, String> boardingPaymentConfirmers = loadPaymentConfirmers("BOARDING", boardingOrders.stream().map(BoardingOrder::getId).collect(Collectors.toSet()));
        List<OrderListView> mergedRecords = Stream.concat(
                        serviceOrders.stream().map(order -> OrderListView.of(
                                order,
                                customerName(customers.get(order.getCustomerId())),
                                petName(pets.get(order.getPetId())),
                                serviceNames.getOrDefault(order.getId(), List.of()),
                                paymentConfirmers.get(order.getId())
                        )),
                        boardingOrders.stream().map(order -> OrderListView.ofBoarding(
                                order.getId(),
                                order.getBoardingNo(),
                                order.getCustomerId(),
                                customerName(customers.get(order.getCustomerId())),
                                order.getPetId(),
                                petName(pets.get(order.getPetId())),
                                boardingTimeRange(order),
                                order.getPlannedCheckOutTime(),
                                order.getStatus(),
                                order.getTotalAmount(),
                                order.getTotalCost(),
                                order.getTotalProfit(),
                                order.getChargeDays(),
                                order.getPaymentStatus(),
                                order.getPaidAmount(),
                                order.getPaidAt(),
                                order.getPaymentMethod(),
                                order.getPaymentNo(),
                                boardingPaymentConfirmers.get(order.getId()),
                                order.getRemark(),
                                order.getCreatedAt(),
                                order.getUpdatedAt()
                        ))
                )
                .sorted((left, right) -> safeText(right.createdAt()).compareTo(safeText(left.createdAt())))
                .toList();
        long total = mergedRecords.size();
        long fromIndex = Math.min((query.getPage() - 1) * query.getPageSize(), total);
        long toIndex = Math.min(fromIndex + query.getPageSize(), total);
        return new PageResult<>(
                mergedRecords.subList((int) fromIndex, (int) toIndex),
                total,
                query.getPage(),
                query.getPageSize()
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
        List<PaymentRecord> paymentRecords = paymentRecordMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderType, "SERVICE")
                .eq(PaymentRecord::getOrderId, id)
                .orderByDesc(PaymentRecord::getPaidAt)
                .orderByDesc(PaymentRecord::getCreatedAt));
        return new OrderDetailView(OrderListView.of(order, customerName(customer), petName(pet)), customer, pet, items, paymentRecords, logs);
    }

    public PageResult<OrderListView> listMyOrders(OrderQuery query, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        LambdaQueryWrapper<ServiceOrder> wrapper = new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getCustomerId, principal.customerId())
                .like(StringUtils.hasText(query.getOrderNo()), ServiceOrder::getOrderNo, query.getOrderNo())
                .eq(StringUtils.hasText(query.getStatus()), ServiceOrder::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getPaymentStatus()), ServiceOrder::getPaymentStatus, query.getPaymentStatus())
                .ge(StringUtils.hasText(query.getAppointmentStart()), ServiceOrder::getAppointmentTime, normalizeStart(query.getAppointmentStart()))
                .le(StringUtils.hasText(query.getAppointmentEnd()), ServiceOrder::getAppointmentTime, normalizeEnd(query.getAppointmentEnd()))
                .orderByDesc(ServiceOrder::getCreatedAt);
        applyPetNameFilter(wrapper, query.getPetName());
        List<ServiceOrder> serviceOrders = orderMapper.selectList(wrapper);
        List<BoardingOrder> boardingOrders = loadBoardingOrders(query, principal.customerId());
        Map<Long, Pet> pets = loadPets(Stream.concat(
                        serviceOrders.stream().map(ServiceOrder::getPetId),
                        boardingOrders.stream().map(BoardingOrder::getPetId)
                )
                .collect(Collectors.toSet()));
        Map<Long, List<String>> serviceNames = loadServiceNames(serviceOrders.stream().map(ServiceOrder::getId).collect(Collectors.toSet()));
        Map<Long, String> paymentConfirmers = loadPaymentConfirmers("SERVICE", serviceOrders.stream().map(ServiceOrder::getId).collect(Collectors.toSet()));
        Map<Long, String> boardingPaymentConfirmers = loadPaymentConfirmers("BOARDING", boardingOrders.stream().map(BoardingOrder::getId).collect(Collectors.toSet()));
        Customer customer = customerMapper.selectById(principal.customerId());
        List<OrderListView> mergedRecords = Stream.concat(
                        serviceOrders.stream().map(order -> OrderListView.of(
                                order,
                                customerName(customer),
                                petName(pets.get(order.getPetId())),
                                serviceNames.getOrDefault(order.getId(), List.of()),
                                paymentConfirmers.get(order.getId())
                        )),
                        boardingOrders.stream().map(order -> OrderListView.ofBoarding(
                                order.getId(),
                                order.getBoardingNo(),
                                order.getCustomerId(),
                                customerName(customer),
                                order.getPetId(),
                                petName(pets.get(order.getPetId())),
                                boardingTimeRange(order),
                                order.getPlannedCheckOutTime(),
                                order.getStatus(),
                                order.getTotalAmount(),
                                order.getTotalCost(),
                                order.getTotalProfit(),
                                order.getChargeDays(),
                                order.getPaymentStatus(),
                                order.getPaidAmount(),
                                order.getPaidAt(),
                                order.getPaymentMethod(),
                                order.getPaymentNo(),
                                boardingPaymentConfirmers.get(order.getId()),
                                order.getRemark(),
                                order.getCreatedAt(),
                                order.getUpdatedAt()
                        ))
                )
                .sorted((left, right) -> safeText(right.createdAt()).compareTo(safeText(left.createdAt())))
                .toList();
        long total = mergedRecords.size();
        long fromIndex = Math.min((query.getPage() - 1) * query.getPageSize(), total);
        long toIndex = Math.min(fromIndex + query.getPageSize(), total);
        return new PageResult<>(
                mergedRecords.subList((int) fromIndex, (int) toIndex),
                total,
                query.getPage(),
                query.getPageSize()
        );
    }

    public OrderDetailView myDetail(Long id, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        ServiceOrder order = getOrderOrThrow(id);
        if (!principal.customerId().equals(order.getCustomerId())) {
            throw new IllegalArgumentException("不能查看其他客户的预约");
        }
        return detail(id);
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
        order.setPaymentStatus(UNPAID);
        order.setPaidAmount(BigDecimal.ZERO);
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
    public OrderDetailView createMyOrder(OrderCreateRequest request, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        OrderCreateRequest scopedRequest = new OrderCreateRequest(
                principal.customerId(),
                request.petId(),
                request.serviceItemIds(),
                request.appointmentTime(),
                request.remark()
        );
        return create(scopedRequest);
    }

    @Transactional
    public OrderDetailView cancelMyOrder(Long id, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        ServiceOrder order = getOrderOrThrow(id);
        if (!principal.customerId().equals(order.getCustomerId())) {
            throw new IllegalArgumentException("不能取消其他客户的预约");
        }
        if (!PENDING.equals(order.getStatus()) && !CONFIRMED.equals(order.getStatus())) {
            throw new IllegalArgumentException("当前状态不能取消预约");
        }
        orderMapper.update(new LambdaUpdateWrapper<ServiceOrder>()
                .eq(ServiceOrder::getId, id)
                .set(ServiceOrder::getStatus, CANCELLED)
                .set(ServiceOrder::getUpdatedAt, now()));
        insertStatusLog(id, order.getStatus(), CANCELLED, "客户取消预约");
        return detail(id);
    }

    @Transactional
    public OrderPayResult payMyOrder(Long id, OrderPayRequest request, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        ServiceOrder order = getOrderOrThrow(id);
        if (!principal.customerId().equals(order.getCustomerId())) {
            throw new IllegalArgumentException("无权限支付该订单");
        }
        if (CANCELLED.equals(order.getStatus()) || REJECTED.equals(order.getStatus())) {
            throw new IllegalArgumentException("该订单不可支付");
        }
        if (!COMPLETED.equals(order.getStatus())) {
            throw new IllegalArgumentException("订单完成后才可支付");
        }
        if (PAID.equals(order.getPaymentStatus())) {
            throw new IllegalArgumentException("订单已支付");
        }

        return markOrderPaid(id, request, principal.accountId(), "MOCK");
    }

    @Transactional
    public OrderPayResult confirmPayment(Long id, OrderPayRequest request, String authorization) {
        AccountPrincipal principal = accountService.requireStaff(authorization);
        ServiceOrder order = getOrderOrThrow(id);
        if (CANCELLED.equals(order.getStatus()) || REJECTED.equals(order.getStatus())) {
            throw new IllegalArgumentException("该订单不可确认支付");
        }
        if (!COMPLETED.equals(order.getStatus())) {
            throw new IllegalArgumentException("订单完成后才可确认支付");
        }
        if (PAID.equals(order.getPaymentStatus())) {
            throw new IllegalArgumentException("订单已支付");
        }
        return markOrderPaid(id, request, principal.accountId(), "MANUAL");
    }

    private OrderPayResult markOrderPaid(Long id, OrderPayRequest request, Long paidByAccountId, String defaultPaymentMethod) {
        ServiceOrder order = getOrderOrThrow(id);
        String paymentMethod = normalizePaymentMethod(request == null ? null : request.paymentMethod(), defaultPaymentMethod);
        String paidAt = now();
        String paymentNo = generatePaymentNo();
        BigDecimal paidAmount = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        int updated = orderMapper.update(new LambdaUpdateWrapper<ServiceOrder>()
                .eq(ServiceOrder::getId, id)
                .eq(ServiceOrder::getPaymentStatus, UNPAID)
                .set(ServiceOrder::getPaymentStatus, PAID)
                .set(ServiceOrder::getPaidAmount, paidAmount)
                .set(ServiceOrder::getPaidAt, paidAt)
                .set(ServiceOrder::getPaymentMethod, paymentMethod)
                .set(ServiceOrder::getPaymentNo, paymentNo)
                .set(ServiceOrder::getUpdatedAt, paidAt));
        if (updated == 0) {
            throw new IllegalArgumentException("订单已支付");
        }

        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setOrderType("SERVICE");
        paymentRecord.setOrderId(id);
        paymentRecord.setPaymentNo(paymentNo);
        paymentRecord.setAmount(paidAmount);
        paymentRecord.setPaymentMethod(paymentMethod);
        paymentRecord.setPaymentStatus(PAID);
        paymentRecord.setPaidByAccountId(paidByAccountId);
        paymentRecord.setPaidAt(paidAt);
        paymentRecord.setCreatedAt(paidAt);
        paymentRecord.setUpdatedAt(paidAt);
        paymentRecordMapper.insert(paymentRecord);

        ServiceOrder paidOrder = getOrderOrThrow(id);
        if (!PAID.equals(paidOrder.getPaymentStatus())) {
            throw new IllegalArgumentException("订单已支付");
        }
        return OrderPayResult.from(paidOrder);
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

    private List<BoardingOrder> loadBoardingOrders(OrderQuery query) {
        return loadBoardingOrders(query, null);
    }

    private List<BoardingOrder> loadBoardingOrders(OrderQuery query, Long customerId) {
        String queryStart = StringUtils.hasText(query.getAppointmentStart()) ? normalizeStart(query.getAppointmentStart()) : null;
        String queryEnd = StringUtils.hasText(query.getAppointmentEnd()) ? normalizeEnd(query.getAppointmentEnd()) : null;
        LambdaQueryWrapper<BoardingOrder> wrapper = new LambdaQueryWrapper<BoardingOrder>()
                .eq(customerId != null, BoardingOrder::getCustomerId, customerId)
                .like(StringUtils.hasText(query.getOrderNo()), BoardingOrder::getBoardingNo, query.getOrderNo())
                .eq(StringUtils.hasText(query.getStatus()), BoardingOrder::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getPaymentStatus()), BoardingOrder::getPaymentStatus, query.getPaymentStatus())
                .lt(queryEnd != null, BoardingOrder::getPlannedCheckInTime, queryEnd)
                .gt(queryStart != null, BoardingOrder::getPlannedCheckOutTime, queryStart)
                .orderByDesc(BoardingOrder::getCreatedAt);
        applyBoardingCustomerNameFilter(wrapper, query.getCustomerName());
        applyBoardingPetNameFilter(wrapper, query.getPetName());
        return boardingOrderMapper.selectList(wrapper);
    }

    private void applyBoardingCustomerNameFilter(LambdaQueryWrapper<BoardingOrder> wrapper, String customerName) {
        if (!StringUtils.hasText(customerName)) {
            return;
        }
        List<Long> ids = customerMapper.selectList(new LambdaQueryWrapper<Customer>().like(Customer::getName, customerName))
                .stream().map(Customer::getId).toList();
        wrapper.in(!ids.isEmpty(), BoardingOrder::getCustomerId, ids);
        wrapper.eq(ids.isEmpty(), BoardingOrder::getCustomerId, -1L);
    }

    private void applyBoardingPetNameFilter(LambdaQueryWrapper<BoardingOrder> wrapper, String petName) {
        if (!StringUtils.hasText(petName)) {
            return;
        }
        List<Long> ids = petMapper.selectList(new LambdaQueryWrapper<Pet>().like(Pet::getName, petName))
                .stream().map(Pet::getId).toList();
        wrapper.in(!ids.isEmpty(), BoardingOrder::getPetId, ids);
        wrapper.eq(ids.isEmpty(), BoardingOrder::getPetId, -1L);
    }

    private String boardingTimeRange(BoardingOrder order) {
        return safeText(order.getPlannedCheckInTime()) + " - " + safeText(order.getPlannedCheckOutTime());
    }

    private String safeText(String value) {
        return value == null ? "" : value;
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

    private Map<Long, List<String>> loadServiceNames(Set<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderItemMapper.selectList(new LambdaQueryWrapper<ServiceOrderItem>()
                        .in(ServiceOrderItem::getOrderId, orderIds)
                        .orderByAsc(ServiceOrderItem::getId))
                .stream()
                .collect(Collectors.groupingBy(
                        ServiceOrderItem::getOrderId,
                        Collectors.mapping(ServiceOrderItem::getServiceName, Collectors.toList())
                ));
    }

    private Map<Long, String> loadPaymentConfirmers(String orderType, Set<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PaymentRecord> paymentRecords = paymentRecordMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderType, orderType)
                .eq(PaymentRecord::getPaymentStatus, PAID)
                .in(PaymentRecord::getOrderId, orderIds)
                .orderByDesc(PaymentRecord::getPaidAt)
                .orderByDesc(PaymentRecord::getCreatedAt));
        if (paymentRecords.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, PaymentRecord> latestRecords = paymentRecords.stream()
                .filter(record -> record.getPaidByAccountId() != null)
                .collect(Collectors.toMap(
                        PaymentRecord::getOrderId,
                        Function.identity(),
                        (first, ignored) -> first
                ));
        if (latestRecords.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Account> accounts = accountMapper.selectByIds(latestRecords.values().stream()
                        .map(PaymentRecord::getPaidByAccountId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
        return latestRecords.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> accountDisplayName(accounts.get(entry.getValue().getPaidByAccountId()))
                ));
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

    private String generatePaymentNo() {
        return "PAY" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String normalizePaymentMethod(String paymentMethod, String defaultPaymentMethod) {
        return StringUtils.hasText(paymentMethod) ? paymentMethod.trim().toUpperCase() : defaultPaymentMethod;
    }

    private String customerName(Customer customer) {
        return customer == null ? "-" : customer.getName();
    }

    private String petName(Pet pet) {
        return pet == null ? "-" : pet.getName();
    }

    private String accountDisplayName(Account account) {
        if (account == null) {
            return "-";
        }
        return StringUtils.hasText(account.getDisplayName()) ? account.getDisplayName() : account.getUsername();
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
