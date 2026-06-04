package com.dropify.user.domain.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "refreshToken", timeToLive = 60 * 60 * 24 * 7)
public class RefreshToken {

    @Id
    private String id; // userId

    private String token;

    private RefreshToken(String id, String token) {
        this.id = id;
        this.token = token;
    }

    public static RefreshToken of(Long userId, String token) {
        return new RefreshToken(String.valueOf(userId), token);
    }

    public void rotate(String newToken) {
        this.token = newToken;
    }
}
