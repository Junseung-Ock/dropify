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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .build();

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        return issueTokens(user.getId());
    }

    public TokenResponse reissue(ReissueRequest request) {
        jwtTokenProvider.validateToken(request.getRefreshToken());

        Long userId = jwtTokenProvider.extractUserId(request.getRefreshToken());

        RefreshToken saved = refreshTokenRepository.findById(String.valueOf(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (!saved.getToken().equals(request.getRefreshToken())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
        saved.rotate(newRefreshToken);
        refreshTokenRepository.save(saved);

        return TokenResponse.builder()
                .accessToken(jwtTokenProvider.createAccessToken(userId))
                .refreshToken(newRefreshToken)
                .build();
    }

    public void logout(ReissueRequest request) {
        jwtTokenProvider.validateToken(request.getRefreshToken());

        Long userId = jwtTokenProvider.extractUserId(request.getRefreshToken());
        refreshTokenRepository.deleteById(String.valueOf(userId));
    }

    private TokenResponse issueTokens(Long userId) {
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        refreshTokenRepository.save(RefreshToken.of(userId, refreshToken));

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
