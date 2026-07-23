package com.example.petcare.health;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VaccineRecordRequest(
        @NotBlank(message = "不能为空") @Size(max = 100, message = "不能超过100个字符") String vaccineName,
        @NotBlank(message = "不能为空") String vaccinationDate,
        @Size(max = 100, message = "不能超过100个字符") String institution,
        String nextVaccinationDate,
        @Size(max = 500, message = "不能超过500个字符") String remark
) {
}
