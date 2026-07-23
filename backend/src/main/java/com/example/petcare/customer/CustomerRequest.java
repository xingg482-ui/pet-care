package com.example.petcare.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "不能为空") @Size(max = 50, message = "不能超过50个字符") String name,
        @NotBlank(message = "不能为空") @Size(max = 30, message = "不能超过30个字符") String phone,
        @Size(max = 100, message = "不能超过100个字符") String email,
        @Size(max = 200, message = "不能超过200个字符") String address,
        @Size(max = 500, message = "不能超过500个字符") String remark
) {
}
