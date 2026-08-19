package com.example.petcare.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.petcare.account.Account;
import com.example.petcare.account.AccountMapper;
import com.example.petcare.boarding.BoardingOrder;
import com.example.petcare.boarding.BoardingOrderMapper;
import com.example.petcare.common.PageResult;
import com.example.petcare.order.ServiceOrder;
import com.example.petcare.order.ServiceOrderMapper;
import com.example.petcare.pet.Pet;
import com.example.petcare.pet.PetMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class CustomerService {

    private static final String ENABLED = "ENABLED";
    private static final String DISABLED = "DISABLED";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CustomerMapper customerMapper;
    private final AccountMapper accountMapper;
    private final PetMapper petMapper;
    private final ServiceOrderMapper orderMapper;
    private final BoardingOrderMapper boardingOrderMapper;

    public CustomerService(
            CustomerMapper customerMapper,
            AccountMapper accountMapper,
            PetMapper petMapper,
            ServiceOrderMapper orderMapper,
            BoardingOrderMapper boardingOrderMapper) {
        this.customerMapper = customerMapper;
        this.accountMapper = accountMapper;
        this.petMapper = petMapper;
        this.orderMapper = orderMapper;
        this.boardingOrderMapper = boardingOrderMapper;
    }

    public PageResult<Customer> list(CustomerQuery query) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .like(StringUtils.hasText(query.getName()), Customer::getName, query.getName())
                .like(StringUtils.hasText(query.getPhone()), Customer::getPhone, query.getPhone())
                .eq(StringUtils.hasText(query.getStatus()), Customer::getStatus, query.getStatus())
                .orderByDesc(Customer::getCreatedAt);
        Page<Customer> page = customerMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public Customer getByIdOrThrow(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        return customer;
    }

    public Customer create(CustomerRequest request) {
        ensurePhoneUnique(trimToNull(request.phone()), null);
        Customer customer = new Customer();
        applyRequest(customer, request);
        customer.setStatus(ENABLED);
        customer.setCreatedAt(now());
        customer.setUpdatedAt(now());
        customerMapper.insert(customer);
        return getByIdOrThrow(customer.getId());
    }

    public Customer update(Long id, CustomerRequest request) {
        Customer customer = getByIdOrThrow(id);
        ensurePhoneUnique(trimToNull(request.phone()), id);
        applyRequest(customer, request);
        customer.setUpdatedAt(now());
        customerMapper.updateById(customer);
        return getByIdOrThrow(id);
    }

    public Customer updateStatus(Long id, String status) {
        getByIdOrThrow(id);
        if (!ENABLED.equals(status) && !DISABLED.equals(status)) {
            throw new IllegalArgumentException("客户状态不合法");
        }
        customerMapper.update(new LambdaUpdateWrapper<Customer>()
                .eq(Customer::getId, id)
                .set(Customer::getStatus, status)
                .set(Customer::getUpdatedAt, now()));
        return getByIdOrThrow(id);
    }

    public void delete(Long id) {
        Customer customer = getByIdOrThrow(id);
        if (!DISABLED.equals(customer.getStatus())) {
            throw new IllegalArgumentException("请先停用客户后再删除");
        }
        ensureNoRelatedData(id);
        customerMapper.deleteById(id);
    }

    private void ensureNoRelatedData(Long id) {
        if (accountMapper.selectCount(new LambdaQueryWrapper<Account>().eq(Account::getCustomerId, id)) > 0) {
            throw new IllegalArgumentException("客户已关联登录账号，无法删除");
        }
        if (petMapper.selectCount(new LambdaQueryWrapper<Pet>().eq(Pet::getCustomerId, id)) > 0) {
            throw new IllegalArgumentException("客户已关联宠物，无法删除");
        }
        if (orderMapper.selectCount(new LambdaQueryWrapper<ServiceOrder>().eq(ServiceOrder::getCustomerId, id)) > 0) {
            throw new IllegalArgumentException("客户已关联订单，无法删除");
        }
        if (boardingOrderMapper.selectCount(new LambdaQueryWrapper<BoardingOrder>().eq(BoardingOrder::getCustomerId, id)) > 0) {
            throw new IllegalArgumentException("客户已关联寄养订单，无法删除");
        }
    }

    private void ensurePhoneUnique(String phone, Long currentId) {
        if (!StringUtils.hasText(phone)) {
            return;
        }
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getPhone, phone)
                .ne(currentId != null, Customer::getId, currentId);
        if (customerMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("手机号已存在");
        }
    }

    private void applyRequest(Customer customer, CustomerRequest request) {
        customer.setName(request.name().trim());
        customer.setPhone(trimToNull(request.phone()));
        customer.setEmail(trimToNull(request.email()));
        customer.setAddress(trimToNull(request.address()));
        customer.setRemark(trimToNull(request.remark()));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}
