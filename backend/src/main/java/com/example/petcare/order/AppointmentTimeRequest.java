package com.example.petcare.order;

import jakarta.validation.constraints.NotBlank;

public record AppointmentTimeRequest(
        @NotBlank(message = "不能为空") String appointmentTime
) {
}
