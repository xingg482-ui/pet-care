package com.example.petcare.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderCreateRequest(
        @NotNull(message = "不能为空") Long customerId,
        @NotNull(message = "不能为空") Long petId,
        @NotEmpty(message = "不能为空") List<Long> serviceItemIds,
        @NotNull(message = "不能为空") String appointmentTime,
        @Size(max = 500, message = "不能超过500个字符") String remark
) {
}
