package com.example.petcare.serviceitem;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServiceItemRequest(
        @NotBlank(message = "不能为空") @Size(max = 100, message = "不能超过100个字符") String name,
        @NotBlank(message = "不能为空") @Size(max = 50, message = "不能超过50个字符") String category,
        @NotNull(message = "不能为空") @DecimalMin(value = "0.0", inclusive = false, message = "必须大于0") BigDecimal price,
        @NotNull(message = "不能为空") @Min(value = 1, message = "必须大于0") Integer durationMinutes,
        @Size(max = 500, message = "不能超过500个字符") String description
) {
}
