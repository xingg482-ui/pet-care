package com.example.petcare.health;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DewormingRecordRequest(
        @NotBlank(message = "不能为空") String dewormingType,
        @NotBlank(message = "不能为空") @Size(max = 100, message = "不能超过100个字符") String medicineName,
        @NotBlank(message = "不能为空") String dewormingDate,
        String nextDewormingDate,
        @Size(max = 500, message = "不能超过500个字符") String remark
) {
}
