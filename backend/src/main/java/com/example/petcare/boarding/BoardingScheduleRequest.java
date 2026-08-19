package com.example.petcare.boarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BoardingScheduleRequest(
        @NotNull(message = "不能为空") Long locationId,
        @NotBlank(message = "不能为空") String plannedCheckInTime,
        @NotBlank(message = "不能为空") String plannedCheckOutTime
) {
}
