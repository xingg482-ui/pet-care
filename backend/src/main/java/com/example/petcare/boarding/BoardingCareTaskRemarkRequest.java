package com.example.petcare.boarding;

import jakarta.validation.constraints.Size;

public record BoardingCareTaskRemarkRequest(
        @Size(max = 500, message = "不能超过500个字符") String remark
) {
}
