package com.example.petcare.boarding;

import java.math.BigDecimal;

public record BoardingOrderView(
        Long id,
        String boardingNo,
        Long customerId,
        String customerName,
        String customerPhone,
        Long petId,
        String petName,
        String petSpecies,
        Long locationId,
        String locationCode,
        String locationName,
        Long areaId,
        String areaName,
        String plannedCheckInTime,
        String plannedCheckOutTime,
        String actualCheckInTime,
        String actualCheckOutTime,
        String status,
        BigDecimal unitPrice,
        BigDecimal unitCost,
        Integer chargeDays,
        BigDecimal totalAmount,
        BigDecimal totalCost,
        BigDecimal totalProfit,
        String paymentStatus,
        BigDecimal paidAmount,
        String paidAt,
        String paymentMethod,
        String paymentNo,
        String remark
) {
}
