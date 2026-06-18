package com.dropify.user.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.user.domain.entity.RefreshToken;
import com.dropify.user.domain.entity.User;
import com.dropify.user.domain.repository.RefreshTokenRepository;
import com.dropify.user.domain.repository.UserRepository;
import com.dropify.user.dto.request.LoginRequest;
import com.dropify.user.dto.request.ReissueRequest;
import com.dropify.user.dto.request.SignupRequest;
import com.dropify.user.dto.response.TokenResponse;
import com.dropify.user.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("정상 회원가입 시 사용자가 저장된다")
    void signup_success() {
        SignupRequest request = mock(SignupRequest.class);
        when(request.getEmail()).thenReturn("test@example.com");
        when(request.getPassword()).thenReturn("password");
        when(request.getName()).thenReturn("테스트");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");

        authService.signup(request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("중복 이메일로 회원가입 시 DUPLICATE_EMAIL 예외가 발생한다")
    void signup_duplicateEmail_throwsBusinessException() {
        SignupRequest request = mock(SignupRequest.class);
        when(request.getEmail()).thenReturn("dup@example.com");
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.DUPLICATE_EMAIL.getMessage());
    }

    @Test
    @DisplayName("정상 로그인 시 토큰이 반환된다")
    void login_success() {
        User user = mock(User.class);
        LoginRequest request = mock(LoginRequest.class);
        when(request.getEmail()).thenReturn("test@example.com");
        when(request.getPassword()).thenReturn("password");
        when(user.getId()).thenReturn(1L);
        when(user.getPassword()).thenReturn("encoded-password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(1L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("refresh-token");

        TokenResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시 USER_NOT_FOUND 예외가 발생한다")
    void login_userNotFound_throwsBusinessException() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.getEmail()).thenReturn("unknown@example.com");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 INVALID_PASSWORD 예외가 발생한다")
    void login_invalidPassword_throwsBusinessException() {
        User user = mock(User.class);
        LoginRequest request = mock(LoginRequest.class);
        when(request.getEmail()).thenReturn("test@example.com");
        when(request.getPassword()).thenReturn("wrong-password");
        when(user.getPassword()).thenReturn("encoded-password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("정상 재발급 시 새 토큰이 반환된다")
    void reissue_success() {
        ReissueRequest request = mock(ReissueRequest.class);
        when(request.getRefreshToken()).thenReturn("valid-refresh-token");

        RefreshToken savedToken = RefreshToken.of(1L, "valid-refresh-token");
        when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.extractUserId("valid-refresh-token")).thenReturn(1L);
        when(refreshTokenRepository.findById("1")).thenReturn(Optional.of(savedToken));
        when(jwtTokenProvider.createAccessToken(1L)).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("new-refresh-token");

        TokenResponse response = authService.reissue(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenRepository).save(savedToken);
    }

    @Test
    @DisplayName("저장된 리프레시 토큰이 없으면 UNAUTHORIZED 예외가 발생한다")
    void reissue_noSavedToken_throwsUnauthorized() {
        ReissueRequest request = mock(ReissueRequest.class);
        when(request.getRefreshToken()).thenReturn("valid-refresh-token");

        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.extractUserId(anyString())).thenReturn(1L);
        when(refreshTokenRepository.findById("1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissue(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    @DisplayName("요청 토큰과 저장된 토큰이 다르면 UNAUTHORIZED 예외가 발생한다")
    void reissue_tokenMismatch_throwsUnauthorized() {
        ReissueRequest request = mock(ReissueRequest.class);
        when(request.getRefreshToken()).thenReturn("request-token");

        RefreshToken savedToken = RefreshToken.of(1L, "different-saved-token");
        when(jwtTokenProvider.validateToken("request-token")).thenReturn(true);
        when(jwtTokenProvider.extractUserId("request-token")).thenReturn(1L);
        when(refreshTokenRepository.findById("1")).thenReturn(Optional.of(savedToken));

        assertThatThrownBy(() -> authService.reissue(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    @DisplayName("정상 로그아웃 시 리프레시 토큰이 삭제된다")
    void logout_success() {
        ReissueRequest request = mock(ReissueRequest.class);
        when(request.getRefreshToken()).thenReturn("valid-refresh-token");

        RefreshToken savedToken = RefreshToken.of(1L, "valid-refresh-token");
        when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.extractUserId("valid-refresh-token")).thenReturn(1L);
        when(refreshTokenRepository.findById("1")).thenReturn(Optional.of(savedToken));

        authService.logout(request);

        verify(refreshTokenRepository).deleteById("1");
    }

    @Test
    @DisplayName("로그아웃 시 요청 토큰과 저장된 토큰이 다르면 UNAUTHORIZED 예외가 발생한다")
    void logout_tokenMismatch_throwsUnauthorized() {
        ReissueRequest request = mock(ReissueRequest.class);
        when(request.getRefreshToken()).thenReturn("request-token");

        RefreshToken savedToken = RefreshToken.of(1L, "different-saved-token");
        when(jwtTokenProvider.validateToken("request-token")).thenReturn(true);
        when(jwtTokenProvider.extractUserId("request-token")).thenReturn(1L);
        when(refreshTokenRepository.findById("1")).thenReturn(Optional.of(savedToken));

        assertThatThrownBy(() -> authService.logout(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED.getMessage());
    }
}
