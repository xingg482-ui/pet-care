package com.example.petcare.health;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WeightRecordRequest(
        @NotBlank(message = "不能为空") String recordDate,
        @NotNull(message = "不能为空") @DecimalMin(value = "0.0", inclusive = false, message = "必须大于0") BigDecimal weight,
        @Size(max = 500, message = "不能超过500个字符") String remark
) {
}
