package com.example.petcare.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcare.customer.Customer;
import com.example.petcare.customer.CustomerMapper;
import com.example.petcare.order.ServiceOrder;
import com.example.petcare.order.ServiceOrderMapper;
import com.example.petcare.pet.Pet;
import com.example.petcare.pet.PetMapper;
import com.example.petcare.serviceitem.ServiceItem;
import com.example.petcare.serviceitem.ServiceItemMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DashboardService {

    private static final String PENDING = "PENDING";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CustomerMapper customerMapper;
    private final PetMapper petMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final ServiceOrderMapper orderMapper;

    public DashboardService(
            CustomerMapper customerMapper,
            PetMapper petMapper,
            ServiceItemMapper serviceItemMapper,
            ServiceOrderMapper orderMapper
    ) {
        this.customerMapper = customerMapper;
        this.petMapper = petMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.orderMapper = orderMapper;
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
}
