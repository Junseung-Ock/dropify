package com.dropify.user.controller;

import com.dropify.common.response.ApiResponse;
import com.dropify.user.dto.request.LoginRequest;
import com.dropify.user.dto.request.ReissueRequest;
import com.dropify.user.dto.request.SignupRequest;
import com.dropify.user.dto.response.TokenResponse;
import com.dropify.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> signup(@RequestBody @Valid SignupRequest request) {
        authService.signup(request);
        return ApiResponse.ok();
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/reissue")
    public ApiResponse<TokenResponse> reissue(@RequestBody @Valid ReissueRequest request) {
        return ApiResponse.ok(authService.reissue(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Valid ReissueRequest request) {
        authService.logout(request);
        return ApiResponse.ok();
    }
}
