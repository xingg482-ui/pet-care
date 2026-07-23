package com.example.petcare.order;

import java.math.BigDecimal;

public record OrderListView(
        Long id,
        String orderNo,
        Long customerId,
        String customerName,
        Long petId,
        String petName,
        String appointmentTime,
        String status,
        BigDecimal totalAmount,
        String remark,
        String createdAt,
        String updatedAt
) {
    public static OrderListView of(ServiceOrder order, String customerName, String petName) {
        return new OrderListView(
                order.getId(),
                order.getOrderNo(),
                order.getCustomerId(),
                customerName,
                order.getPetId(),
                petName,
                order.getAppointmentTime(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getRemark(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
