package com.dropify.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SignupRequest {

    @Schema(example = "user@example.com")
    @Email
    @NotBlank
    private String email;

    @Schema(example = "password123!")
    @NotBlank
    private String password;

    @Schema(example = "홍길동")
    @NotBlank
    private String name;

    @Schema(example = "010-1234-5678")
    private String phone;
}
