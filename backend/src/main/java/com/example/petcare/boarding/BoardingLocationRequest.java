package com.example.petcare.boarding;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BoardingLocationRequest(
        @NotNull(message = "不能为空") Long areaId,
        @NotBlank(message = "不能为空") @Size(max = 30, message = "不能超过30个字符") String code,
        @NotBlank(message = "不能为空") @Size(max = 50, message = "不能超过50个字符") String name,
        @NotBlank(message = "不能为空") @Size(max = 30, message = "不能超过30个字符") String locationType,
        @NotBlank(message = "不能为空") @Size(max = 30, message = "不能超过30个字符") String petSpecies,
        @NotBlank(message = "不能为空") @Size(max = 30, message = "不能超过30个字符") String petSize,
        @NotNull(message = "不能为空") @Min(value = 1, message = "必须大于0") Integer capacity,
        @DecimalMin(value = "0.0", message = "不能小于0") BigDecimal pricePerDay,
        @DecimalMin(value = "0.0", message = "不能小于0") BigDecimal costPerDay,
        @Size(max = 300, message = "不能超过300个字符") String remark
) {
}
