package com.example.petcare.pet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PetRequest(
        @NotNull(message = "不能为空") Long customerId,
        @NotBlank(message = "不能为空") @Size(max = 50, message = "不能超过50个字符") String name,
        @NotBlank(message = "不能为空") @Size(max = 50, message = "不能超过50个字符") String species,
        @Size(max = 50, message = "不能超过50个字符") String breed,
        String gender,
        String birthday,
        @DecimalMin(value = "0.0", inclusive = false, message = "必须大于0") BigDecimal weight,
        Boolean sterilized,
        @Size(max = 500, message = "不能超过500个字符") String remark
) {
}
