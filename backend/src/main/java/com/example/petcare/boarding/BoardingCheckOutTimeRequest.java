package com.example.petcare.boarding;

import jakarta.validation.constraints.NotBlank;

public record BoardingCheckOutTimeRequest(
        @NotBlank(message = "不能为空") String plannedCheckOutTime
) {
}
