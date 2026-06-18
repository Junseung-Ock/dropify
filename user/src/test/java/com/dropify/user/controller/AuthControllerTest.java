package com.dropify.user.controller;

import com.dropify.user.dto.response.TokenResponse;
import com.dropify.user.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("유효한 회원가입 요청이면 201을 반환한다")
    void signup_valid_returns201() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "test@example.com",
                                "password", "password123",
                                "name", "테스트"
                        ))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("회원가입 시 이메일 누락이면 400을 반환한다")
    void signup_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", "password123",
                                "name", "테스트"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 시 이메일 형식이 잘못되면 400을 반환한다")
    void signup_invalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "not-an-email",
                                "password", "password123",
                                "name", "테스트"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 시 비밀번호 누락이면 400을 반환한다")
    void signup_missingPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "test@example.com",
                                "name", "테스트"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 시 이름 누락이면 400을 반환한다")
    void signup_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "test@example.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효한 로그인 요청이면 200을 반환한다")
    void login_valid_returns200() throws Exception {
        when(authService.login(any())).thenReturn(TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "test@example.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인 시 이메일 누락이면 400을 반환한다")
    void login_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", "password123"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 시 비밀번호 누락이면 400을 반환한다")
    void login_missingPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "test@example.com"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("토큰 재발급 시 refreshToken 누락이면 400을 반환한다")
    void reissue_missingRefreshToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그아웃 시 refreshToken 누락이면 400을 반환한다")
    void logout_missingRefreshToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
