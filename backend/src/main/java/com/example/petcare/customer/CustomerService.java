package com.example.petcare.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.petcare.common.PageResult;
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

    public CustomerService(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
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
        ensurePhoneUnique(request.phone(), null);
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
        ensurePhoneUnique(request.phone(), id);
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

    private void ensurePhoneUnique(String phone, Long currentId) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getPhone, phone)
                .ne(currentId != null, Customer::getId, currentId);
        if (customerMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("手机号已存在");
        }
    }

    private void applyRequest(Customer customer, CustomerRequest request) {
        customer.setName(request.name());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setAddress(request.address());
        customer.setRemark(request.remark());
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}
