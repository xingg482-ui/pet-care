package com.example.petcare.order;

import java.math.BigDecimal;

public record OrderPayResult(
        Long orderId,
        String orderNo,
        String paymentStatus,
        BigDecimal paidAmount,
        String paymentMethod,
        String paymentNo,
        String paidAt
) {
    public static OrderPayResult from(ServiceOrder order) {
        return new OrderPayResult(
                order.getId(),
                order.getOrderNo(),
                order.getPaymentStatus(),
                order.getPaidAmount(),
                order.getPaymentMethod(),
                order.getPaymentNo(),
                order.getPaidAt()
        );
    }
}
