package com.dropify.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequest {

    @Schema(example = "admin@gmail.com")
    @Email
    @NotBlank
    private String email;

    @Schema(example = "admin")
    @NotBlank
    private String password;
}
