package com.example.petcare.order;

import java.math.BigDecimal;
import java.util.List;

public record OrderListView(
        String orderType,
        Long id,
        String orderNo,
        Long customerId,
        String customerName,
        Long petId,
        String petName,
        String appointmentTime,
        String plannedCheckOutTime,
        String status,
        BigDecimal totalAmount,
        BigDecimal totalCost,
        BigDecimal totalProfit,
        String paymentStatus,
        BigDecimal paidAmount,
        String paidAt,
        String paymentMethod,
        String paymentNo,
        String paymentConfirmedByName,
        List<String> serviceNames,
        String remark,
        String createdAt,
        String updatedAt
) {
    public static OrderListView of(ServiceOrder order, String customerName, String petName) {
        return of(order, customerName, petName, List.of());
    }

    public static OrderListView of(ServiceOrder order, String customerName, String petName, List<String> serviceNames) {
        return of(order, customerName, petName, serviceNames, null);
    }

    public static OrderListView of(
            ServiceOrder order,
            String customerName,
            String petName,
            List<String> serviceNames,
            String paymentConfirmedByName
    ) {
        return new OrderListView(
                "SERVICE",
                order.getId(),
                order.getOrderNo(),
                order.getCustomerId(),
                customerName,
                order.getPetId(),
                petName,
                order.getAppointmentTime(),
                null,
                order.getStatus(),
                order.getTotalAmount(),
                order.getTotalCost(),
                order.getTotalProfit(),
                normalizePaymentStatus(order.getPaymentStatus()),
                order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount(),
                order.getPaidAt(),
                order.getPaymentMethod(),
                order.getPaymentNo(),
                paymentConfirmedByName,
                serviceNames,
                order.getRemark(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public static OrderListView ofBoarding(
            Long id,
            String boardingNo,
            Long customerId,
            String customerName,
            Long petId,
            String petName,
            String timeRange,
            String plannedCheckOutTime,
            String status,
            BigDecimal totalAmount,
            BigDecimal totalCost,
            BigDecimal totalProfit,
            Integer chargeDays,
            String paymentStatus,
            BigDecimal paidAmount,
            String paidAt,
            String paymentMethod,
            String paymentNo,
            String paymentConfirmedByName,
            String remark,
            String createdAt,
            String updatedAt
    ) {
        String daysText = "托管 " + (chargeDays == null ? 0 : chargeDays) + " 天";
        return new OrderListView(
                "BOARDING",
                id,
                boardingNo,
                customerId,
                customerName,
                petId,
                petName,
                timeRange,
                plannedCheckOutTime,
                status,
                totalAmount,
                totalCost,
                totalProfit,
                normalizePaymentStatus(paymentStatus),
                paidAmount == null ? BigDecimal.ZERO : paidAmount,
                paidAt,
                paymentMethod,
                paymentNo,
                paymentConfirmedByName,
                List.of("宠物托管", daysText),
                remark,
                createdAt,
                updatedAt
        );
    }

    private static String normalizePaymentStatus(String paymentStatus) {
        return paymentStatus == null || paymentStatus.isBlank() ? "UNPAID" : paymentStatus;
    }
}
