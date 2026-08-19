package com.example.petcare.boarding;

import jakarta.validation.constraints.NotNull;

public record BoardingChangeLocationRequest(
        @NotNull(message = "不能为空") Long locationId
) {
}
