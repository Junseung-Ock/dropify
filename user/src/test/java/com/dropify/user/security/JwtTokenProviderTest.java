package com.dropify.user.security;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-for-unit-testing-must-be-long-enough";

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, 3_600_000L, 604_800_000L);
    }

    @Test
    @DisplayName("액세스 토큰에서 userId를 정확히 추출한다")
    void createAccessToken_extractUserId_success() {
        String token = jwtTokenProvider.createAccessToken(1L);

        assertThat(jwtTokenProvider.extractUserId(token)).isEqualTo(1L);
    }

    @Test
    @DisplayName("리프레시 토큰에서 userId를 정확히 추출한다")
    void createRefreshToken_extractUserId_success() {
        String token = jwtTokenProvider.createRefreshToken(42L);

        assertThat(jwtTokenProvider.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("유효한 토큰 검증이 성공한다")
    void validateToken_valid_returnsTrue() {
        String token = jwtTokenProvider.createAccessToken(1L);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰 검증 시 TOKEN_EXPIRED 예외가 발생한다")
    void validateToken_expired_throwsTokenExpired() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, -1000L, -1000L);
        String expiredToken = expiredProvider.createAccessToken(1L);

        assertThatThrownBy(() -> jwtTokenProvider.validateToken(expiredToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.TOKEN_EXPIRED.getMessage());
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 검증 시 UNAUTHORIZED 예외가 발생한다")
    void validateToken_invalid_throwsUnauthorized() {
        assertThatThrownBy(() -> jwtTokenProvider.validateToken("invalid.token.value"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED.getMessage());
    }
}
