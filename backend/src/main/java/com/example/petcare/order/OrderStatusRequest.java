package com.example.petcare.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderStatusRequest(
        @NotBlank(message = "不能为空") String status,
        @Size(max = 500, message = "不能超过500个字符") String remark
) {
}
