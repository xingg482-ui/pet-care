package com.example.petcare.boarding;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardingAreaRequest(
        @NotBlank(message = "不能为空") @Size(max = 50, message = "不能超过50个字符") String name,
        @Min(value = 0, message = "不能小于0") Integer sortOrder,
        @Size(max = 300, message = "不能超过300个字符") String remark
) {
}
