package com.example.petcare.petavatar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PetAvatarLibraryRequest(
        @NotBlank(message = "不能为空") @Size(max = 50, message = "不能超过50个字符") String species,
        @NotBlank(message = "不能为空") @Size(max = 50, message = "不能超过50个字符") String breed,
        @Size(max = 300, message = "不能超过300个字符") String keywords,
        @Size(max = 300, message = "不能超过300个字符") String avatarUrl,
        Integer sortOrder,
        @Size(max = 500, message = "不能超过500个字符") String remark
) {
}
