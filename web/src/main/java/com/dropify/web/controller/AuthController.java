package com.dropify.web.controller;

import com.dropify.common.response.ApiResponse;
import com.dropify.user.dto.request.LoginRequest;
import com.dropify.user.dto.request.ReissueRequest;
import com.dropify.user.dto.request.SignupRequest;
import com.dropify.user.dto.response.TokenResponse;
import com.dropify.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", security = {})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 입력값 (COMMON_001)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 가입된 이메일 (USER_002)")
    })
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> signup(@RequestBody @Valid SignupRequest request) {
        authService.signup(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "로그인", security = {})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "잘못된 비밀번호 (USER_003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 사용자 (USER_001)")
    })
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "토큰 재발급", security = {})
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "만료된 토큰 (USER_005)")
    )
    @PostMapping("/reissue")
    public ApiResponse<TokenResponse> reissue(@RequestBody @Valid ReissueRequest request) {
        return ApiResponse.ok(authService.reissue(request));
    }

    @Operation(summary = "로그아웃", security = {})
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Valid ReissueRequest request) {
        authService.logout(request);
        return ApiResponse.ok();
    }
}
