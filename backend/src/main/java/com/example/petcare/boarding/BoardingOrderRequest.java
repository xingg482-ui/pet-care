package com.example.petcare.boarding;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BoardingOrderRequest(
        @NotNull(message = "不能为空") Long customerId,
        @NotNull(message = "不能为空") Long petId,
        @NotNull(message = "不能为空") Long locationId,
        @NotBlank(message = "不能为空") String plannedCheckInTime,
        @NotBlank(message = "不能为空") String plannedCheckOutTime,
        @DecimalMin(value = "0.0", message = "不能小于0") BigDecimal totalAmount,
        @Size(max = 500, message = "不能超过500个字符") String remark
) {
}
