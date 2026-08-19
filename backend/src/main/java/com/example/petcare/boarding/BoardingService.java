package com.example.petcare.boarding;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.petcare.account.AccountPrincipal;
import com.example.petcare.account.AccountService;
import com.example.petcare.common.PageResult;
import com.example.petcare.customer.Customer;
import com.example.petcare.customer.CustomerMapper;
import com.example.petcare.order.OrderPayRequest;
import com.example.petcare.order.OrderPayResult;
import com.example.petcare.order.PaymentRecord;
import com.example.petcare.order.PaymentRecordMapper;
import com.example.petcare.pet.Pet;
import com.example.petcare.pet.PetMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BoardingService {

    private static final String ENABLED = "ENABLED";
    private static final String DISABLED = "DISABLED";
    private static final String MAINTENANCE = "MAINTENANCE";
    private static final String CLEAN = "CLEAN";
    private static final String DIRTY = "DIRTY";
    private static final String CLEANING = "CLEANING";
    private static final String RESERVED = "RESERVED";
    private static final String CHECKED_IN = "CHECKED_IN";
    private static final String CANCELLED = "CANCELLED";
    private static final String COMPLETED = "COMPLETED";
    private static final String PENDING = "PENDING";
    private static final String DONE = "DONE";
    private static final String UNPAID = "UNPAID";
    private static final String PAID = "PAID";
    private static final Set<String> OCCUPYING_STATUSES = Set.of(RESERVED, CHECKED_IN);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BoardingAreaMapper areaMapper;
    private final BoardingLocationMapper locationMapper;
    private final BoardingOrderMapper orderMapper;
    private final BoardingCareTaskMapper careTaskMapper;
    private final CustomerMapper customerMapper;
    private final PetMapper petMapper;
    private final AccountService accountService;
    private final PaymentRecordMapper paymentRecordMapper;

    public BoardingService(
            BoardingAreaMapper areaMapper,
            BoardingLocationMapper locationMapper,
            BoardingOrderMapper orderMapper,
            BoardingCareTaskMapper careTaskMapper,
            CustomerMapper customerMapper,
            PetMapper petMapper,
            AccountService accountService,
            PaymentRecordMapper paymentRecordMapper
    ) {
        this.areaMapper = areaMapper;
        this.locationMapper = locationMapper;
        this.orderMapper = orderMapper;
        this.careTaskMapper = careTaskMapper;
        this.customerMapper = customerMapper;
        this.petMapper = petMapper;
        this.accountService = accountService;
        this.paymentRecordMapper = paymentRecordMapper;
    }

    public List<BoardingArea> listAreas() {
        return areaMapper.selectList(new LambdaQueryWrapper<BoardingArea>()
                .orderByAsc(BoardingArea::getSortOrder)
                .orderByAsc(BoardingArea::getId));
    }

    public BoardingArea getAreaByIdOrThrow(Long id) {
        BoardingArea area = areaMapper.selectById(id);
        if (area == null) {
            throw new IllegalArgumentException("托管区域不存在");
        }
        return area;
    }

    public BoardingArea createArea(BoardingAreaRequest request) {
        BoardingArea area = new BoardingArea();
        applyAreaRequest(area, request);
        area.setStatus(ENABLED);
        area.setCreatedAt(now());
        area.setUpdatedAt(now());
        areaMapper.insert(area);
        return getAreaByIdOrThrow(area.getId());
    }

    public BoardingArea updateArea(Long id, BoardingAreaRequest request) {
        BoardingArea area = getAreaByIdOrThrow(id);
        applyAreaRequest(area, request);
        area.setUpdatedAt(now());
        areaMapper.updateById(area);
        return getAreaByIdOrThrow(id);
    }

    public BoardingArea updateAreaStatus(Long id, String status) {
        getAreaByIdOrThrow(id);
        validateEnabledStatus(status);
        areaMapper.update(new LambdaUpdateWrapper<BoardingArea>()
                .eq(BoardingArea::getId, id)
                .set(BoardingArea::getStatus, status)
                .set(BoardingArea::getUpdatedAt, now()));
        return getAreaByIdOrThrow(id);
    }

    public PageResult<BoardingLocation> listLocations(BoardingLocationQuery query) {
        LambdaQueryWrapper<BoardingLocation> wrapper = new LambdaQueryWrapper<BoardingLocation>()
                .eq(query.getAreaId() != null, BoardingLocation::getAreaId, query.getAreaId())
                .like(StringUtils.hasText(query.getCode()), BoardingLocation::getCode, query.getCode())
                .eq(StringUtils.hasText(query.getLocationType()), BoardingLocation::getLocationType, query.getLocationType())
                .eq(StringUtils.hasText(query.getPetSpecies()), BoardingLocation::getPetSpecies, query.getPetSpecies())
                .eq(StringUtils.hasText(query.getPetSize()), BoardingLocation::getPetSize, query.getPetSize())
                .eq(StringUtils.hasText(query.getStatus()), BoardingLocation::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getCleanStatus()), BoardingLocation::getCleanStatus, query.getCleanStatus())
                .orderByAsc(BoardingLocation::getCode);
        Page<BoardingLocation> page = locationMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public BoardingLocation getLocationByIdOrThrow(Long id) {
        BoardingLocation location = locationMapper.selectById(id);
        if (location == null) {
            throw new IllegalArgumentException("托管位置不存在");
        }
        return location;
    }

    public BoardingLocation createLocation(BoardingLocationRequest request) {
        BoardingLocation location = new BoardingLocation();
        applyLocationRequest(location, request);
        ensureUniqueCode(null, request.code());
        location.setStatus(ENABLED);
        location.setCleanStatus(CLEAN);
        location.setCreatedAt(now());
        location.setUpdatedAt(now());
        locationMapper.insert(location);
        return getLocationByIdOrThrow(location.getId());
    }

    public BoardingLocation updateLocation(Long id, BoardingLocationRequest request) {
        BoardingLocation location = getLocationByIdOrThrow(id);
        ensureUniqueCode(id, request.code());
        applyLocationRequest(location, request);
        location.setUpdatedAt(now());
        locationMapper.updateById(location);
        return getLocationByIdOrThrow(id);
    }

    public BoardingLocation updateLocationStatus(Long id, String status) {
        getLocationByIdOrThrow(id);
        if (!ENABLED.equals(status) && !DISABLED.equals(status) && !MAINTENANCE.equals(status)) {
            throw new IllegalArgumentException("托管位置状态不合法");
        }
        locationMapper.update(new LambdaUpdateWrapper<BoardingLocation>()
                .eq(BoardingLocation::getId, id)
                .set(BoardingLocation::getStatus, status)
                .set(BoardingLocation::getUpdatedAt, now()));
        return getLocationByIdOrThrow(id);
    }

    public BoardingLocation updateCleanStatus(Long id, String cleanStatus) {
        getLocationByIdOrThrow(id);
        if (!CLEAN.equals(cleanStatus) && !DIRTY.equals(cleanStatus) && !CLEANING.equals(cleanStatus)) {
            throw new IllegalArgumentException("清洁状态不合法");
        }
        locationMapper.update(new LambdaUpdateWrapper<BoardingLocation>()
                .eq(BoardingLocation::getId, id)
                .set(BoardingLocation::getCleanStatus, cleanStatus)
                .set(BoardingLocation::getUpdatedAt, now()));
        return getLocationByIdOrThrow(id);
    }

    public PageResult<BoardingOrderView> listOrders(BoardingOrderQuery query) {
        String queryStart = StringUtils.hasText(query.getStartDate()) ? dateStart(query.getStartDate()) : null;
        String queryEnd = StringUtils.hasText(query.getEndDate()) ? dateEnd(query.getEndDate()) : null;
        LambdaQueryWrapper<BoardingOrder> wrapper = new LambdaQueryWrapper<BoardingOrder>()
                .eq(query.getLocationId() != null, BoardingOrder::getLocationId, query.getLocationId())
                .eq(StringUtils.hasText(query.getStatus()), BoardingOrder::getStatus, query.getStatus())
                .lt(queryEnd != null, BoardingOrder::getPlannedCheckInTime, queryEnd)
                .gt(queryStart != null, BoardingOrder::getPlannedCheckOutTime, queryStart)
                .orderByDesc(BoardingOrder::getCreatedAt);
        applyOrderAreaFilter(wrapper, query.getAreaId());
        applyOrderKeywordFilter(wrapper, query.getKeyword());
        Page<BoardingOrder> page = orderMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        return new PageResult<>(
                toOrderViews(page.getRecords()),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    public PageResult<BoardingOrderView> listMyOrders(BoardingOrderQuery query, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        String queryStart = StringUtils.hasText(query.getStartDate()) ? dateStart(query.getStartDate()) : null;
        String queryEnd = StringUtils.hasText(query.getEndDate()) ? dateEnd(query.getEndDate()) : null;
        LambdaQueryWrapper<BoardingOrder> wrapper = new LambdaQueryWrapper<BoardingOrder>()
                .eq(BoardingOrder::getCustomerId, principal.customerId())
                .eq(query.getLocationId() != null, BoardingOrder::getLocationId, query.getLocationId())
                .eq(StringUtils.hasText(query.getStatus()), BoardingOrder::getStatus, query.getStatus())
                .lt(queryEnd != null, BoardingOrder::getPlannedCheckInTime, queryEnd)
                .gt(queryStart != null, BoardingOrder::getPlannedCheckOutTime, queryStart)
                .orderByDesc(BoardingOrder::getCreatedAt);
        Page<BoardingOrder> page = orderMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        return new PageResult<>(
                toOrderViews(page.getRecords()),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    @Transactional
    public BoardingOrderView createOrder(BoardingOrderRequest request) {
        Customer customer = requireEnabledCustomer(request.customerId());
        Pet pet = requireEnabledPet(request.petId());
        if (!customer.getId().equals(pet.getCustomerId())) {
            throw new IllegalArgumentException("宠物不属于所选客户");
        }
        BoardingLocation location = getLocationByIdOrThrow(request.locationId());
        if (!ENABLED.equals(location.getStatus())) {
            throw new IllegalArgumentException("停用或维护中的位置不能创建预约");
        }
        if (!CLEAN.equals(location.getCleanStatus())) {
            throw new IllegalArgumentException("未清洁完成的位置不能创建预约");
        }
        LocalDateTime checkIn = parseDateTime(request.plannedCheckInTime());
        LocalDateTime checkOut = parseDateTime(request.plannedCheckOutTime());
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("计划退房时间必须晚于入住时间");
        }
        ensureNoConflict(null, location.getId(), pet.getId(), checkIn, checkOut);

        BoardingOrder order = new BoardingOrder();
        order.setBoardingNo(generateBoardingNo());
        order.setCustomerId(customer.getId());
        order.setPetId(pet.getId());
        order.setLocationId(location.getId());
        order.setPlannedCheckInTime(format(checkIn));
        order.setPlannedCheckOutTime(format(checkOut));
        order.setStatus(RESERVED);
        BoardingFee fee = calculateFee(location, checkIn, checkOut);
        order.setUnitPrice(fee.unitPrice());
        order.setUnitCost(fee.unitCost());
        order.setChargeDays(fee.chargeDays());
        order.setTotalAmount(request.totalAmount() == null ? fee.totalAmount() : request.totalAmount());
        order.setTotalCost(fee.totalCost());
        order.setTotalProfit(value(order.getTotalAmount()).subtract(fee.totalCost()));
        order.setPaymentStatus(UNPAID);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setRemark(request.remark());
        order.setCreatedAt(now());
        order.setUpdatedAt(now());
        orderMapper.insert(order);
        return toOrderViews(List.of(getOrderByIdOrThrow(order.getId()))).get(0);
    }

    @Transactional
    public BoardingOrderView createMyOrder(BoardingOrderRequest request, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        BoardingOrderRequest scopedRequest = new BoardingOrderRequest(
                principal.customerId(),
                request.petId(),
                request.locationId(),
                request.plannedCheckInTime(),
                request.plannedCheckOutTime(),
                null,
                request.remark()
        );
        return createOrder(scopedRequest);
    }

    @Transactional
    public BoardingOrderView checkIn(Long id) {
        BoardingOrder order = getOrderByIdOrThrow(id);
        if (!RESERVED.equals(order.getStatus())) {
            throw new IllegalArgumentException("只有已预约的托管单可以办理入住");
        }
        BoardingLocation location = getLocationByIdOrThrow(order.getLocationId());
        if (!ENABLED.equals(location.getStatus())) {
            throw new IllegalArgumentException("当前位置不可用，不能办理入住");
        }
        if (!CLEAN.equals(location.getCleanStatus())) {
            throw new IllegalArgumentException("当前位置未清洁完成，不能办理入住");
        }
        orderMapper.update(new LambdaUpdateWrapper<BoardingOrder>()
                .eq(BoardingOrder::getId, id)
                .set(BoardingOrder::getStatus, CHECKED_IN)
                .set(BoardingOrder::getActualCheckInTime, now())
                .set(BoardingOrder::getUpdatedAt, now()));
        return toOrderViews(List.of(getOrderByIdOrThrow(id))).get(0);
    }

    @Transactional
    public BoardingOrderView checkOut(Long id) {
        return completeBoardingStay(id);
    }

    @Transactional
    public BoardingOrderView confirmPickedUp(Long id) {
        return completeBoardingStay(id);
    }

    private BoardingOrderView completeBoardingStay(Long id) {
        BoardingOrder order = getOrderByIdOrThrow(id);
        if (!CHECKED_IN.equals(order.getStatus())) {
            throw new IllegalArgumentException("只有已入住的托管单可以确认接回");
        }
        String now = now();
        LocalDateTime actualCheckIn = StringUtils.hasText(order.getActualCheckInTime())
                ? parseDateTime(order.getActualCheckInTime())
                : parseDateTime(order.getPlannedCheckInTime());
        BoardingFee fee = calculateSnapshotFee(order, actualCheckIn, parseDateTime(now));
        orderMapper.update(new LambdaUpdateWrapper<BoardingOrder>()
                .eq(BoardingOrder::getId, id)
                .set(BoardingOrder::getStatus, COMPLETED)
                .set(BoardingOrder::getActualCheckOutTime, now)
                .set(BoardingOrder::getChargeDays, fee.chargeDays())
                .set(BoardingOrder::getTotalAmount, fee.totalAmount())
                .set(BoardingOrder::getTotalCost, fee.totalCost())
                .set(BoardingOrder::getTotalProfit, fee.totalProfit())
                .set(BoardingOrder::getUpdatedAt, now));
        locationMapper.update(new LambdaUpdateWrapper<BoardingLocation>()
                .eq(BoardingLocation::getId, order.getLocationId())
                .set(BoardingLocation::getCleanStatus, DIRTY)
                .set(BoardingLocation::getUpdatedAt, now));
        return toOrderViews(List.of(getOrderByIdOrThrow(id))).get(0);
    }

    public BoardingOrderView cancelOrder(Long id) {
        BoardingOrder order = getOrderByIdOrThrow(id);
        if (!RESERVED.equals(order.getStatus())) {
            throw new IllegalArgumentException("只有已预约的托管单可以取消");
        }
        orderMapper.update(new LambdaUpdateWrapper<BoardingOrder>()
                .eq(BoardingOrder::getId, id)
                .set(BoardingOrder::getStatus, CANCELLED)
                .set(BoardingOrder::getUpdatedAt, now()));
        return toOrderViews(List.of(getOrderByIdOrThrow(id))).get(0);
    }

    public BoardingOrderView cancelMyOrder(Long id, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        BoardingOrder order = getOrderByIdOrThrow(id);
        if (!principal.customerId().equals(order.getCustomerId())) {
            throw new IllegalArgumentException("不能取消其他客户的托管预约");
        }
        return cancelOrder(id);
    }

    public BoardingOrderView updateMySchedule(Long id, BoardingScheduleRequest request, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        BoardingOrder order = getOrderByIdOrThrow(id);
        if (!principal.customerId().equals(order.getCustomerId())) {
            throw new IllegalArgumentException("不能调整其他客户的托管预约");
        }
        if (!RESERVED.equals(order.getStatus())) {
            throw new IllegalArgumentException("只有已预约的托管单可以调整时间或房间");
        }
        return updateSchedule(id, request);
    }

    @Transactional
    public OrderPayResult payMyOrder(Long id, OrderPayRequest request, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        BoardingOrder order = getOrderByIdOrThrow(id);
        if (!principal.customerId().equals(order.getCustomerId())) {
            throw new IllegalArgumentException("不能支付其他客户的托管预约");
        }
        return markBoardingOrderPaid(id, request, principal.accountId(), "MOCK");
    }

    @Transactional
    public OrderPayResult confirmPayment(Long id, OrderPayRequest request, String authorization) {
        AccountPrincipal principal = accountService.requireStaff(authorization);
        return markBoardingOrderPaid(id, request, principal.accountId(), "MANUAL");
    }

    private OrderPayResult markBoardingOrderPaid(Long id, OrderPayRequest request, Long paidByAccountId, String defaultPaymentMethod) {
        BoardingOrder order = getOrderByIdOrThrow(id);
        if (!COMPLETED.equals(order.getStatus())) {
            throw new IllegalArgumentException("托管完成后才可支付");
        }
        if (PAID.equals(order.getPaymentStatus())) {
            throw new IllegalArgumentException("托管订单已支付");
        }
        String paidAt = now();
        String paymentMethod = normalizePaymentMethod(request == null ? null : request.paymentMethod(), "MOCK");
        String paymentNo = generatePaymentNo();
        BigDecimal paidAmount = value(order.getTotalAmount());
        int updated = orderMapper.update(new LambdaUpdateWrapper<BoardingOrder>()
                .eq(BoardingOrder::getId, id)
                .eq(BoardingOrder::getPaymentStatus, UNPAID)
                .set(BoardingOrder::getPaymentStatus, PAID)
                .set(BoardingOrder::getPaidAmount, paidAmount)
                .set(BoardingOrder::getPaidAt, paidAt)
                .set(BoardingOrder::getPaymentMethod, paymentMethod)
                .set(BoardingOrder::getPaymentNo, paymentNo)
                .set(BoardingOrder::getUpdatedAt, paidAt));
        if (updated == 0) {
            throw new IllegalArgumentException("托管订单已支付");
        }
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setOrderType("BOARDING");
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
        BoardingOrder paidOrder = getOrderByIdOrThrow(id);
        return new OrderPayResult(
                paidOrder.getId(),
                paidOrder.getBoardingNo(),
                paidOrder.getPaymentStatus(),
                paidOrder.getPaidAmount(),
                paidOrder.getPaymentMethod(),
                paidOrder.getPaymentNo(),
                paidOrder.getPaidAt()
        );
    }

    public BoardingOrderView updatePlannedCheckOutTime(Long id, BoardingCheckOutTimeRequest request) {
        BoardingOrder order = getOrderByIdOrThrow(id);
        if (!RESERVED.equals(order.getStatus()) && !CHECKED_IN.equals(order.getStatus())) {
            throw new IllegalArgumentException("当前状态不能修改预计退房时间");
        }
        LocalDateTime checkIn = parseDateTime(order.getPlannedCheckInTime());
        LocalDateTime checkOut = parseDateTime(request.plannedCheckOutTime());
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("预计退房时间必须晚于入住时间");
        }
        ensureNoConflict(order.getId(), order.getLocationId(), order.getPetId(), checkIn, checkOut);
        BoardingFee fee = calculateSnapshotFee(order, checkIn, checkOut);
        orderMapper.update(new LambdaUpdateWrapper<BoardingOrder>()
                .eq(BoardingOrder::getId, id)
                .set(BoardingOrder::getPlannedCheckOutTime, format(checkOut))
                .set(BoardingOrder::getChargeDays, fee.chargeDays())
                .set(BoardingOrder::getTotalAmount, fee.totalAmount())
                .set(BoardingOrder::getTotalCost, fee.totalCost())
                .set(BoardingOrder::getTotalProfit, fee.totalProfit())
                .set(BoardingOrder::getUpdatedAt, now()));
        return toOrderViews(List.of(getOrderByIdOrThrow(id))).get(0);
    }

    public BoardingOrderView changeLocation(Long id, BoardingChangeLocationRequest request) {
        BoardingOrder order = getOrderByIdOrThrow(id);
        if (!RESERVED.equals(order.getStatus()) && !CHECKED_IN.equals(order.getStatus())) {
            throw new IllegalArgumentException("当前状态不能换房");
        }
        BoardingLocation location = getLocationByIdOrThrow(request.locationId());
        if (!ENABLED.equals(location.getStatus())) {
            throw new IllegalArgumentException("目标位置不可用");
        }
        if (!CLEAN.equals(location.getCleanStatus())) {
            throw new IllegalArgumentException("目标位置未清洁完成");
        }
        ensureNoConflict(order.getId(), location.getId(), order.getPetId(), parseDateTime(order.getPlannedCheckInTime()), parseDateTime(order.getPlannedCheckOutTime()));
        String now = now();
        LambdaUpdateWrapper<BoardingOrder> updateWrapper = new LambdaUpdateWrapper<BoardingOrder>()
                .eq(BoardingOrder::getId, id)
                .set(BoardingOrder::getLocationId, location.getId())
                .set(BoardingOrder::getUpdatedAt, now);
        if (RESERVED.equals(order.getStatus())) {
            BoardingFee fee = calculateFee(location, parseDateTime(order.getPlannedCheckInTime()), parseDateTime(order.getPlannedCheckOutTime()));
            updateWrapper
                    .set(BoardingOrder::getUnitPrice, fee.unitPrice())
                    .set(BoardingOrder::getUnitCost, fee.unitCost())
                    .set(BoardingOrder::getChargeDays, fee.chargeDays())
                    .set(BoardingOrder::getTotalAmount, fee.totalAmount())
                    .set(BoardingOrder::getTotalCost, fee.totalCost())
                    .set(BoardingOrder::getTotalProfit, fee.totalProfit());
        }
        orderMapper.update(updateWrapper);
        if (CHECKED_IN.equals(order.getStatus()) && !order.getLocationId().equals(location.getId())) {
            locationMapper.update(new LambdaUpdateWrapper<BoardingLocation>()
                    .eq(BoardingLocation::getId, order.getLocationId())
                    .set(BoardingLocation::getCleanStatus, DIRTY)
                    .set(BoardingLocation::getUpdatedAt, now));
        }
        return toOrderViews(List.of(getOrderByIdOrThrow(id))).get(0);
    }

    public BoardingOrderView updateSchedule(Long id, BoardingScheduleRequest request) {
        BoardingOrder order = getOrderByIdOrThrow(id);
        if (!RESERVED.equals(order.getStatus()) && !CHECKED_IN.equals(order.getStatus())) {
            throw new IllegalArgumentException("当前状态不能拖动改期");
        }
        BoardingLocation location = getLocationByIdOrThrow(request.locationId());
        if (!ENABLED.equals(location.getStatus())) {
            throw new IllegalArgumentException("目标位置不可用");
        }
        if (!CLEAN.equals(location.getCleanStatus()) && !location.getId().equals(order.getLocationId())) {
            throw new IllegalArgumentException("目标位置未清洁完成");
        }
        LocalDateTime checkIn = parseDateTime(request.plannedCheckInTime());
        LocalDateTime checkOut = parseDateTime(request.plannedCheckOutTime());
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("计划退房时间必须晚于入住时间");
        }
        ensureNoConflict(order.getId(), location.getId(), order.getPetId(), checkIn, checkOut);
        BoardingFee fee = calculateFee(location, checkIn, checkOut);
        orderMapper.update(new LambdaUpdateWrapper<BoardingOrder>()
                .eq(BoardingOrder::getId, id)
                .set(BoardingOrder::getLocationId, location.getId())
                .set(BoardingOrder::getPlannedCheckInTime, format(checkIn))
                .set(BoardingOrder::getPlannedCheckOutTime, format(checkOut))
                .set(BoardingOrder::getUnitPrice, fee.unitPrice())
                .set(BoardingOrder::getUnitCost, fee.unitCost())
                .set(BoardingOrder::getChargeDays, fee.chargeDays())
                .set(BoardingOrder::getTotalAmount, fee.totalAmount())
                .set(BoardingOrder::getTotalCost, fee.totalCost())
                .set(BoardingOrder::getTotalProfit, fee.totalProfit())
                .set(BoardingOrder::getUpdatedAt, now()));
        if (CHECKED_IN.equals(order.getStatus()) && !order.getLocationId().equals(location.getId())) {
            locationMapper.update(new LambdaUpdateWrapper<BoardingLocation>()
                    .eq(BoardingLocation::getId, order.getLocationId())
                    .set(BoardingLocation::getCleanStatus, DIRTY)
                    .set(BoardingLocation::getUpdatedAt, now()));
        }
        return toOrderViews(List.of(getOrderByIdOrThrow(id))).get(0);
    }

    public BoardingRoomStatusView roomStatus(BoardingRoomStatusQuery query) {
        LocalDate start = StringUtils.hasText(query.getStartDate()) ? LocalDate.parse(query.getStartDate(), DATE_FORMATTER) : LocalDate.now();
        int days = query.getDays();
        LocalDate end = start.plusDays(days - 1L);
        List<String> dates = start.datesUntil(end.plusDays(1)).map(DATE_FORMATTER::format).toList();

        List<BoardingLocation> locations = locationMapper.selectList(new LambdaQueryWrapper<BoardingLocation>()
                .eq(query.getAreaId() != null, BoardingLocation::getAreaId, query.getAreaId())
                .and(StringUtils.hasText(query.getKeyword()), wrapper -> wrapper
                        .like(BoardingLocation::getCode, query.getKeyword())
                        .or()
                        .like(BoardingLocation::getName, query.getKeyword()))
                .orderByAsc(BoardingLocation::getCode));
        List<BoardingOrderView> orderViews = toOrderViews(loadRangeOrders(start, end));
        Map<Long, List<BoardingOrderView>> ordersByLocation = orderViews.stream().collect(Collectors.groupingBy(BoardingOrderView::locationId));
        Map<Long, BoardingArea> areaMap = loadAreas(locations.stream().map(BoardingLocation::getAreaId).collect(Collectors.toSet()));

        List<BoardingRoomRow> rows = locations.stream()
                .map(location -> toRoomRow(location, areaMap.get(location.getAreaId()), dates, ordersByLocation.getOrDefault(location.getId(), Collections.emptyList())))
                .filter(row -> matchMatrixStatus(row, query.getStatus()))
                .toList();
        return new BoardingRoomStatusView(start.format(DATE_FORMATTER), end.format(DATE_FORMATTER), days, dates, summary(start, end, orderViews, rows), rows);
    }

    @Transactional
    public List<BoardingInHousePetView> inHousePets(String date, Long areaId, String keyword) {
        return inHousePets(date, areaId, keyword, null);
    }

    @Transactional
    public List<BoardingInHousePetView> myCareUpdates(String date, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        return inHousePets(date, null, null, principal.customerId());
    }

    private List<BoardingInHousePetView> inHousePets(String date, Long areaId, String keyword, Long customerId) {
        LocalDate taskDate = StringUtils.hasText(date) ? LocalDate.parse(date, DATE_FORMATTER) : LocalDate.now();
        LambdaQueryWrapper<BoardingOrder> wrapper = new LambdaQueryWrapper<BoardingOrder>()
                .eq(customerId != null, BoardingOrder::getCustomerId, customerId)
                .in(BoardingOrder::getStatus, OCCUPYING_STATUSES)
                .lt(BoardingOrder::getPlannedCheckInTime, taskDate.plusDays(1).atStartOfDay().format(DATE_TIME_FORMATTER))
                .gt(BoardingOrder::getPlannedCheckOutTime, taskDate.atStartOfDay().format(DATE_TIME_FORMATTER))
                .orderByAsc(BoardingOrder::getPlannedCheckOutTime);
        applyOrderAreaFilter(wrapper, areaId);
        applyOrderKeywordFilter(wrapper, keyword);
        List<BoardingOrder> orders = orderMapper.selectList(wrapper);
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        ensureDailyCareTasks(orders, taskDate);
        List<BoardingOrderView> orderViews = toOrderViews(orders);
        Map<Long, List<BoardingCareTaskView>> tasksByOrder = careTaskMapper.selectList(new LambdaQueryWrapper<BoardingCareTask>()
                        .in(BoardingCareTask::getBoardingOrderId, orders.stream().map(BoardingOrder::getId).toList())
                        .eq(BoardingCareTask::getTaskDate, taskDate.format(DATE_FORMATTER))
                        .orderByAsc(BoardingCareTask::getTaskTime))
                .stream()
                .map(this::toCareTaskView)
                .collect(Collectors.groupingBy(BoardingCareTaskView::boardingOrderId));
        return orderViews.stream()
                .map(order -> toInHousePetView(order, tasksByOrder.getOrDefault(order.id(), Collections.emptyList()), taskDate))
                .toList();
    }

    public BoardingCareTaskView completeCareTask(Long id) {
        BoardingCareTask task = getCareTaskByIdOrThrow(id);
        String now = now();
        careTaskMapper.update(new LambdaUpdateWrapper<BoardingCareTask>()
                .eq(BoardingCareTask::getId, id)
                .set(BoardingCareTask::getStatus, DONE)
                .set(BoardingCareTask::getCompletedAt, now)
                .set(BoardingCareTask::getUpdatedAt, now));
        return toCareTaskView(getCareTaskByIdOrThrow(task.getId()));
    }

    public List<BoardingCareTaskView> createCareTasks(BoardingCareTaskCreateRequest request) {
        BoardingOrder order = getOrderByIdOrThrow(request.boardingOrderId());
        if (!OCCUPYING_STATUSES.contains(order.getStatus())) {
            throw new IllegalArgumentException("只有已预约或已入住的托管宠物可以新增照护任务");
        }
        LocalDate taskDate = LocalDate.parse(request.taskDate(), DATE_FORMATTER);
        LocalDate plannedCheckInDate = LocalDate.parse(order.getPlannedCheckInTime().substring(0, 10), DATE_FORMATTER);
        LocalDate plannedCheckOutDate = LocalDate.parse(order.getPlannedCheckOutTime().substring(0, 10), DATE_FORMATTER);
        if (taskDate.isBefore(plannedCheckInDate) || taskDate.isAfter(plannedCheckOutDate)) {
            throw new IllegalArgumentException("照护任务日期必须在托管周期内");
        }
        LocalTime startTime = LocalTime.parse(request.startTime(), DateTimeFormatter.ofPattern("HH:mm"));
        int repeatCount = request.repeatCount();
        int intervalHours = request.intervalHours() == null ? 0 : request.intervalHours();
        if (repeatCount > 1 && intervalHours <= 0) {
            throw new IllegalArgumentException("多次执行任务需要填写间隔小时");
        }
        List<BoardingCareTaskView> created = new ArrayList<>();
        for (int index = 0; index < repeatCount; index++) {
            LocalTime taskTime = startTime.plusHours((long) intervalHours * index);
            if (index > 0 && !taskTime.isAfter(startTime)) {
                continue;
            }
            BoardingCareTask task = createCareTaskIfAbsent(
                    order.getId(),
                    request.taskType(),
                    request.taskName(),
                    taskDate.format(DATE_FORMATTER),
                    taskTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    request.remark()
            );
            if (task != null) {
                created.add(toCareTaskView(task));
            }
        }
        return created;
    }

    public BoardingCareTaskView updateCareTaskRemark(Long id, BoardingCareTaskRemarkRequest request) {
        getCareTaskByIdOrThrow(id);
        careTaskMapper.update(new LambdaUpdateWrapper<BoardingCareTask>()
                .eq(BoardingCareTask::getId, id)
                .set(BoardingCareTask::getRemark, request.remark())
                .set(BoardingCareTask::getUpdatedAt, now()));
        return toCareTaskView(getCareTaskByIdOrThrow(id));
    }

    private BoardingRoomRow toRoomRow(BoardingLocation location, BoardingArea area, List<String> dates, List<BoardingOrderView> orders) {
        List<BoardingRoomCell> cells = new ArrayList<>();
        for (String date : dates) {
            BoardingOrderView order = orders.stream().filter(item -> occupiesDate(item, LocalDate.parse(date, DATE_FORMATTER))).findFirst().orElse(null);
            String status = cellStatus(location, order);
            cells.add(new BoardingRoomCell(date, status, order));
        }
        return new BoardingRoomRow(
                location.getId(),
                location.getCode(),
                location.getName(),
                location.getAreaId(),
                area == null ? "-" : area.getName(),
                location.getLocationType(),
                location.getPetSpecies(),
                location.getPetSize(),
                location.getCapacity(),
                location.getStatus(),
                location.getCleanStatus(),
                cells
        );
    }

    private String cellStatus(BoardingLocation location, BoardingOrderView order) {
        if (DISABLED.equals(location.getStatus()) || MAINTENANCE.equals(location.getStatus())) {
            return "UNAVAILABLE";
        }
        if (!CLEAN.equals(location.getCleanStatus())) {
            return "NEEDS_CLEANING";
        }
        if (order == null) {
            return "AVAILABLE";
        }
        return CHECKED_IN.equals(order.status()) ? "CHECKED_IN" : "RESERVED";
    }

    private void ensureDailyCareTasks(List<BoardingOrder> orders, LocalDate taskDate) {
        String dateText = taskDate.format(DATE_FORMATTER);
        Set<Long> orderIds = orders.stream().map(BoardingOrder::getId).collect(Collectors.toSet());
        Set<String> existingKeys = careTaskMapper.selectList(new LambdaQueryWrapper<BoardingCareTask>()
                        .in(BoardingCareTask::getBoardingOrderId, orderIds)
                        .eq(BoardingCareTask::getTaskDate, dateText))
                .stream()
                .map(task -> task.getBoardingOrderId() + ":" + task.getTaskType())
                .collect(Collectors.toSet());
        for (BoardingOrder order : orders) {
            for (CareTaskTemplate template : defaultCareTaskTemplates()) {
                String key = order.getId() + ":" + template.taskType();
                if (existingKeys.contains(key)) {
                    continue;
                }
                createCareTaskIfAbsent(order.getId(), template.taskType(), template.taskName(), dateText, template.taskTime(), null);
            }
        }
    }

    private BoardingCareTask createCareTaskIfAbsent(Long orderId, String taskType, String taskName, String taskDate, String taskTime, String remark) {
        BoardingCareTask existing = careTaskMapper.selectOne(new LambdaQueryWrapper<BoardingCareTask>()
                .eq(BoardingCareTask::getBoardingOrderId, orderId)
                .eq(BoardingCareTask::getTaskDate, taskDate)
                .eq(BoardingCareTask::getTaskType, taskType)
                .eq(BoardingCareTask::getTaskTime, taskTime)
                .last("limit 1"));
        if (existing != null) {
            return null;
        }
        BoardingCareTask task = new BoardingCareTask();
        task.setBoardingOrderId(orderId);
        task.setTaskType(taskType);
        task.setTaskName(taskName);
        task.setTaskDate(taskDate);
        task.setTaskTime(taskTime);
        task.setStatus(PENDING);
        task.setRemark(remark);
        task.setCreatedAt(now());
        task.setUpdatedAt(now());
        careTaskMapper.insert(task);
        return getCareTaskByIdOrThrow(task.getId());
    }

    private List<CareTaskTemplate> defaultCareTaskTemplates() {
        return List.of(
                new CareTaskTemplate("FEEDING", "喂食补水", "09:00"),
                new CareTaskTemplate("ACTIVITY", "活动陪伴", "11:00"),
                new CareTaskTemplate("CLEAN", "位置清洁", "15:00"),
                new CareTaskTemplate("OBSERVE", "健康观察", "20:00")
        );
    }

    private BoardingInHousePetView toInHousePetView(BoardingOrderView order, List<BoardingCareTaskView> tasks, LocalDate taskDate) {
        int completed = (int) tasks.stream().filter(task -> DONE.equals(task.status())).count();
        int total = tasks.size();
        LocalDate checkInDate = StringUtils.hasText(order.actualCheckInTime())
                ? LocalDate.parse(order.actualCheckInTime().substring(0, 10), DATE_FORMATTER)
                : LocalDate.parse(order.plannedCheckInTime().substring(0, 10), DATE_FORMATTER);
        int stayDays = Math.max(1, (int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, taskDate) + 1);
        return new BoardingInHousePetView(
                order.id(),
                order.boardingNo(),
                order.customerId(),
                order.customerName(),
                order.customerPhone(),
                order.petId(),
                order.petName(),
                order.petSpecies(),
                order.locationId(),
                order.locationCode(),
                order.locationName(),
                order.areaId(),
                order.areaName(),
                order.actualCheckInTime(),
                order.plannedCheckOutTime(),
                stayDays,
                total,
                completed,
                total - completed,
                tasks
        );
    }

    private BoardingCareTask getCareTaskByIdOrThrow(Long id) {
        BoardingCareTask task = careTaskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("照护任务不存在");
        }
        return task;
    }

    private BoardingCareTaskView toCareTaskView(BoardingCareTask task) {
        return new BoardingCareTaskView(
                task.getId(),
                task.getBoardingOrderId(),
                task.getTaskType(),
                task.getTaskName(),
                task.getTaskDate(),
                task.getTaskTime(),
                task.getStatus(),
                task.getRemark(),
                task.getCompletedAt()
        );
    }

    private BoardingRoomSummary summary(LocalDate start, LocalDate end, List<BoardingOrderView> orders, List<BoardingRoomRow> rows) {
        LocalDate today = LocalDate.now();
        long todayPendingCheckIn = orders.stream().filter(order -> RESERVED.equals(order.status()) && LocalDate.parse(order.plannedCheckInTime().substring(0, 10)).equals(today)).count();
        long todayPendingCheckOut = orders.stream().filter(order -> OCCUPYING_STATUSES.contains(order.status()) && LocalDate.parse(order.plannedCheckOutTime().substring(0, 10)).equals(today)).count();
        long todayOccupiedCapacity = orders.stream().filter(order -> occupiesDate(order, today)).count();
        long availableCapacity = rows.stream()
                .flatMap(row -> row.cells().stream())
                .filter(cell -> "AVAILABLE".equals(cell.status()))
                .count();
        return new BoardingRoomSummary(todayPendingCheckIn, todayPendingCheckOut, todayOccupiedCapacity, availableCapacity);
    }

    private boolean matchMatrixStatus(BoardingRoomRow row, String status) {
        if (!StringUtils.hasText(status) || "ALL".equals(status)) {
            return true;
        }
        if ("AVAILABLE".equals(status)) {
            return row.cells().stream().anyMatch(cell -> "AVAILABLE".equals(cell.status()));
        }
        if ("OCCUPIED".equals(status)) {
            return row.cells().stream().anyMatch(cell -> "RESERVED".equals(cell.status()) || "CHECKED_IN".equals(cell.status()));
        }
        return true;
    }

    private List<BoardingOrder> loadRangeOrders(LocalDate start, LocalDate end) {
        return orderMapper.selectList(new LambdaQueryWrapper<BoardingOrder>()
                .lt(BoardingOrder::getPlannedCheckInTime, end.plusDays(1).atStartOfDay().format(DATE_TIME_FORMATTER))
                .gt(BoardingOrder::getPlannedCheckOutTime, start.atStartOfDay().format(DATE_TIME_FORMATTER))
                .in(BoardingOrder::getStatus, OCCUPYING_STATUSES));
    }

    private void applyAreaRequest(BoardingArea area, BoardingAreaRequest request) {
        area.setName(request.name());
        area.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        area.setRemark(request.remark());
    }

    private void applyLocationRequest(BoardingLocation location, BoardingLocationRequest request) {
        BoardingArea area = getAreaByIdOrThrow(request.areaId());
        if (!ENABLED.equals(area.getStatus())) {
            throw new IllegalArgumentException("请选择启用中的托管区域");
        }
        location.setAreaId(request.areaId());
        location.setCode(request.code());
        location.setName(request.name());
        location.setLocationType(request.locationType());
        location.setPetSpecies(request.petSpecies());
        location.setPetSize(request.petSize());
        location.setCapacity(request.capacity());
        location.setPricePerDay(value(request.pricePerDay()));
        location.setCostPerDay(value(request.costPerDay()));
        location.setRemark(request.remark());
    }

    private BoardingOrder getOrderByIdOrThrow(Long id) {
        BoardingOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("托管预约不存在");
        }
        return order;
    }

    private void applyOrderAreaFilter(LambdaQueryWrapper<BoardingOrder> wrapper, Long areaId) {
        if (areaId == null) {
            return;
        }
        List<Long> locationIds = locationMapper.selectList(new LambdaQueryWrapper<BoardingLocation>().eq(BoardingLocation::getAreaId, areaId))
                .stream().map(BoardingLocation::getId).toList();
        wrapper.in(!locationIds.isEmpty(), BoardingOrder::getLocationId, locationIds);
        wrapper.eq(locationIds.isEmpty(), BoardingOrder::getLocationId, -1L);
    }

    private void applyOrderKeywordFilter(LambdaQueryWrapper<BoardingOrder> wrapper, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        List<Long> customerIds = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                        .like(Customer::getName, keyword)
                        .or()
                        .like(Customer::getPhone, keyword))
                .stream().map(Customer::getId).toList();
        List<Long> petIds = petMapper.selectList(new LambdaQueryWrapper<Pet>().like(Pet::getName, keyword))
                .stream().map(Pet::getId).toList();
        List<Long> locationIds = locationMapper.selectList(new LambdaQueryWrapper<BoardingLocation>().like(BoardingLocation::getCode, keyword))
                .stream().map(BoardingLocation::getId).toList();
        if (customerIds.isEmpty() && petIds.isEmpty() && locationIds.isEmpty()) {
            wrapper.eq(BoardingOrder::getId, -1L);
            return;
        }
        wrapper.and(nested -> nested
                .in(!customerIds.isEmpty(), BoardingOrder::getCustomerId, customerIds)
                .or()
                .in(!petIds.isEmpty(), BoardingOrder::getPetId, petIds)
                .or()
                .in(!locationIds.isEmpty(), BoardingOrder::getLocationId, locationIds));
    }

    private List<BoardingOrderView> toOrderViews(List<BoardingOrder> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Customer> customers = loadCustomers(orders.stream().map(BoardingOrder::getCustomerId).collect(Collectors.toSet()));
        Map<Long, Pet> pets = loadPets(orders.stream().map(BoardingOrder::getPetId).collect(Collectors.toSet()));
        Map<Long, BoardingLocation> locations = loadLocations(orders.stream().map(BoardingOrder::getLocationId).collect(Collectors.toSet()));
        Map<Long, BoardingArea> areas = loadAreas(locations.values().stream().map(BoardingLocation::getAreaId).collect(Collectors.toSet()));
        return orders.stream().map(order -> {
            Customer customer = customers.get(order.getCustomerId());
            Pet pet = pets.get(order.getPetId());
            BoardingLocation location = locations.get(order.getLocationId());
            BoardingArea area = location == null ? null : areas.get(location.getAreaId());
            return new BoardingOrderView(
                    order.getId(),
                    order.getBoardingNo(),
                    order.getCustomerId(),
                    customer == null ? "-" : customer.getName(),
                    customer == null ? "" : customer.getPhone(),
                    order.getPetId(),
                    pet == null ? "-" : pet.getName(),
                    pet == null ? "" : pet.getSpecies(),
                    order.getLocationId(),
                    location == null ? "-" : location.getCode(),
                    location == null ? "-" : location.getName(),
                    location == null ? null : location.getAreaId(),
                    area == null ? "-" : area.getName(),
                    order.getPlannedCheckInTime(),
                    order.getPlannedCheckOutTime(),
                    order.getActualCheckInTime(),
                    order.getActualCheckOutTime(),
                    order.getStatus(),
                    value(order.getUnitPrice()),
                    value(order.getUnitCost()),
                    order.getChargeDays() == null ? 0 : order.getChargeDays(),
                    order.getTotalAmount(),
                    value(order.getTotalCost()),
                    value(order.getTotalProfit()),
                    normalizePaymentStatus(order.getPaymentStatus()),
                    value(order.getPaidAmount()),
                    order.getPaidAt(),
                    order.getPaymentMethod(),
                    order.getPaymentNo(),
                    order.getRemark()
            );
        }).toList();
    }

    private Customer requireEnabledCustomer(Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        if (!ENABLED.equals(customer.getStatus())) {
            throw new IllegalArgumentException("停用客户不能创建托管预约");
        }
        return customer;
    }

    private Pet requireEnabledPet(Long petId) {
        Pet pet = petMapper.selectById(petId);
        if (pet == null) {
            throw new IllegalArgumentException("宠物不存在");
        }
        if (!ENABLED.equals(pet.getStatus())) {
            throw new IllegalArgumentException("停用宠物不能创建托管预约");
        }
        return pet;
    }

    private void ensureNoConflict(Long currentId, Long locationId, Long petId, LocalDateTime checkIn, LocalDateTime checkOut) {
        LambdaQueryWrapper<BoardingOrder> locationWrapper = conflictWrapper(currentId, checkIn, checkOut)
                .eq(BoardingOrder::getLocationId, locationId);
        if (orderMapper.selectCount(locationWrapper) > 0) {
            throw new IllegalArgumentException("该位置在所选时间段已有托管预约");
        }
        LambdaQueryWrapper<BoardingOrder> petWrapper = conflictWrapper(currentId, checkIn, checkOut)
                .eq(BoardingOrder::getPetId, petId);
        if (orderMapper.selectCount(petWrapper) > 0) {
            throw new IllegalArgumentException("该宠物在所选时间段已有托管预约");
        }
    }

    private LambdaQueryWrapper<BoardingOrder> conflictWrapper(Long currentId, LocalDateTime checkIn, LocalDateTime checkOut) {
        return new LambdaQueryWrapper<BoardingOrder>()
                .ne(currentId != null, BoardingOrder::getId, currentId)
                .in(BoardingOrder::getStatus, OCCUPYING_STATUSES)
                .lt(BoardingOrder::getPlannedCheckInTime, format(checkOut))
                .gt(BoardingOrder::getPlannedCheckOutTime, format(checkIn));
    }

    private boolean occupiesDate(BoardingOrderView order, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        return parseDateTime(order.plannedCheckInTime()).isBefore(dayEnd)
                && parseDateTime(order.plannedCheckOutTime()).isAfter(dayStart);
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

    private Map<Long, BoardingLocation> loadLocations(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return locationMapper.selectByIds(ids).stream().collect(Collectors.toMap(BoardingLocation::getId, Function.identity()));
    }

    private Map<Long, BoardingArea> loadAreas(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return areaMapper.selectByIds(ids).stream().collect(Collectors.toMap(BoardingArea::getId, Function.identity()));
    }

    private void ensureUniqueCode(Long currentId, String code) {
        BoardingLocation existing = locationMapper.selectOne(new LambdaQueryWrapper<BoardingLocation>()
                .eq(BoardingLocation::getCode, code)
                .last("limit 1"));
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new IllegalArgumentException("托管位置编号已存在");
        }
    }

    private void validateEnabledStatus(String status) {
        if (!ENABLED.equals(status) && !DISABLED.equals(status)) {
            throw new IllegalArgumentException("托管区域状态不合法");
        }
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    private LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    private String format(LocalDateTime time) {
        return time.format(DATE_TIME_FORMATTER);
    }

    private String dateStart(String value) {
        return LocalDate.parse(value, DATE_FORMATTER).atStartOfDay().format(DATE_TIME_FORMATTER);
    }

    private String dateEnd(String value) {
        return LocalDate.parse(value, DATE_FORMATTER).atTime(LocalTime.MAX).format(DATE_TIME_FORMATTER);
    }

    private String generateBoardingNo() {
        return "BO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private BoardingFee calculateFee(BoardingLocation location, LocalDateTime checkIn, LocalDateTime checkOut) {
        return calculateFee(value(location.getPricePerDay()), value(location.getCostPerDay()), checkIn, checkOut);
    }

    private BoardingFee calculateSnapshotFee(BoardingOrder order, LocalDateTime checkIn, LocalDateTime checkOut) {
        return calculateFee(value(order.getUnitPrice()), value(order.getUnitCost()), checkIn, checkOut);
    }

    private BoardingFee calculateFee(BigDecimal unitPrice, BigDecimal unitCost, LocalDateTime checkIn, LocalDateTime checkOut) {
        int chargeDays = chargeDays(checkIn, checkOut);
        BigDecimal days = BigDecimal.valueOf(chargeDays);
        BigDecimal totalAmount = unitPrice.multiply(days).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCost = unitCost.multiply(days).setScale(2, RoundingMode.HALF_UP);
        return new BoardingFee(unitPrice, unitCost, chargeDays, totalAmount, totalCost, totalAmount.subtract(totalCost));
    }

    private int chargeDays(LocalDateTime checkIn, LocalDateTime checkOut) {
        long minutes = Math.max(1, Duration.between(checkIn, checkOut).toMinutes());
        return Math.max(1, (int) Math.ceil(minutes / 1440.0));
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizePaymentStatus(String paymentStatus) {
        return StringUtils.hasText(paymentStatus) ? paymentStatus : UNPAID;
    }

    private String normalizePaymentMethod(String paymentMethod, String defaultPaymentMethod) {
        return StringUtils.hasText(paymentMethod) ? paymentMethod.trim().toUpperCase() : defaultPaymentMethod;
    }

    private String generatePaymentNo() {
        return "PAY" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private record BoardingFee(
            BigDecimal unitPrice,
            BigDecimal unitCost,
            Integer chargeDays,
            BigDecimal totalAmount,
            BigDecimal totalCost,
            BigDecimal totalProfit
    ) {
    }

    private record CareTaskTemplate(
            String taskType,
            String taskName,
            String taskTime
    ) {
    }
}
