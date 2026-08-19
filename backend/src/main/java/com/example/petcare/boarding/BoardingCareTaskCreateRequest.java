package com.example.petcare.boarding;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BoardingCareTaskCreateRequest(
        @NotNull(message = "不能为空") Long boardingOrderId,
        @NotBlank(message = "不能为空") @Size(max = 30, message = "不能超过30个字符") String taskType,
        @NotBlank(message = "不能为空") @Size(max = 50, message = "不能超过50个字符") String taskName,
        @NotBlank(message = "不能为空") String taskDate,
        @NotBlank(message = "不能为空") String startTime,
        @Min(value = 1, message = "必须大于0") @Max(value = 24, message = "不能超过24小时") Integer intervalHours,
        @NotNull(message = "不能为空") @Min(value = 1, message = "必须大于0") @Max(value = 12, message = "不能超过12次") Integer repeatCount,
        @Size(max = 500, message = "不能超过500个字符") String remark
) {
}
