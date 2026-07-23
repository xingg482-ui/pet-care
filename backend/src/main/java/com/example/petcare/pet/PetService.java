package com.example.petcare.pet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.petcare.common.PageResult;
import com.example.petcare.customer.Customer;
import com.example.petcare.customer.CustomerMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PetService {

    private static final String ENABLED = "ENABLED";
    private static final String DISABLED = "DISABLED";
    private static final String UNKNOWN = "UNKNOWN";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PetMapper petMapper;
    private final CustomerMapper customerMapper;

    public PetService(PetMapper petMapper, CustomerMapper customerMapper) {
        this.petMapper = petMapper;
        this.customerMapper = customerMapper;
    }

    public PageResult<PetView> list(PetQuery query) {
        LambdaQueryWrapper<Pet> wrapper = buildQuery(query).orderByDesc(Pet::getCreatedAt);
        Page<Pet> page = petMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        Map<Long, Customer> customers = loadCustomers(page.getRecords().stream()
                .map(Pet::getCustomerId)
                .collect(Collectors.toSet()));
        return new PageResult<>(
                page.getRecords().stream()
                        .map(pet -> PetView.from(pet, customerName(customers.get(pet.getCustomerId()))))
                        .toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    public PetView getViewByIdOrThrow(Long id) {
        Pet pet = getByIdOrThrow(id);
        Customer customer = customerMapper.selectById(pet.getCustomerId());
        return PetView.from(pet, customerName(customer));
    }

    public PageResult<PetView> listByCustomer(Long customerId, boolean onlyEnabled) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        PetQuery query = new PetQuery();
        query.setCustomerId(customerId);
        query.setPageSize(100);
        if (onlyEnabled) {
            query.setStatus(ENABLED);
        }
        return list(query);
    }

    public PetView create(PetRequest request) {
        ensureCustomerEnabled(request.customerId());
        Pet pet = new Pet();
        applyRequest(pet, request);
        pet.setStatus(ENABLED);
        pet.setCreatedAt(now());
        pet.setUpdatedAt(now());
        petMapper.insert(pet);
        return getViewByIdOrThrow(pet.getId());
    }

    public PetView update(Long id, PetRequest request) {
        Pet pet = getByIdOrThrow(id);
        ensureCustomerEnabled(request.customerId());
        applyRequest(pet, request);
        pet.setUpdatedAt(now());
        petMapper.updateById(pet);
        return getViewByIdOrThrow(id);
    }

    public PetView updateStatus(Long id, String status) {
        getByIdOrThrow(id);
        if (!ENABLED.equals(status) && !DISABLED.equals(status)) {
            throw new IllegalArgumentException("宠物状态不合法");
        }
        petMapper.update(new LambdaUpdateWrapper<Pet>()
                .eq(Pet::getId, id)
                .set(Pet::getStatus, status)
                .set(Pet::getUpdatedAt, now()));
        return getViewByIdOrThrow(id);
    }

    private LambdaQueryWrapper<Pet> buildQuery(PetQuery query) {
        return new LambdaQueryWrapper<Pet>()
                .like(StringUtils.hasText(query.getName()), Pet::getName, query.getName())
                .eq(query.getCustomerId() != null, Pet::getCustomerId, query.getCustomerId())
                .like(StringUtils.hasText(query.getSpecies()), Pet::getSpecies, query.getSpecies())
                .eq(StringUtils.hasText(query.getStatus()), Pet::getStatus, query.getStatus());
    }

    private Pet getByIdOrThrow(Long id) {
        Pet pet = petMapper.selectById(id);
        if (pet == null) {
            throw new IllegalArgumentException("宠物不存在");
        }
        return pet;
    }

    private void ensureCustomerEnabled(Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        if (!ENABLED.equals(customer.getStatus())) {
            throw new IllegalArgumentException("停用客户不能关联新宠物");
        }
    }

    private void applyRequest(Pet pet, PetRequest request) {
        pet.setCustomerId(request.customerId());
        pet.setName(request.name());
        pet.setSpecies(request.species());
        pet.setBreed(request.breed());
        pet.setGender(StringUtils.hasText(request.gender()) ? request.gender() : UNKNOWN);
        pet.setBirthday(request.birthday());
        pet.setWeight(request.weight());
        pet.setSterilized(request.sterilized());
        pet.setRemark(request.remark());
    }

    private Map<Long, Customer> loadCustomers(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return customerMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
    }

    private String customerName(Customer customer) {
        return customer == null ? "-" : customer.getName();
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}
