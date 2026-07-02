package com.dropify.order.controller;

import com.dropify.common.exception.GlobalExceptionHandler;
import com.dropify.order.domain.entity.OrderStatus;
import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.PlaceOrderResponse;
import com.dropify.order.service.OrderService;
import com.dropify.user.domain.entity.User;
import com.dropify.user.domain.entity.UserRole;
import com.dropify.user.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import({GlobalExceptionHandler.class, OrderControllerTest.SecurityConfig.class})
class OrderControllerTest {

    @TestConfiguration
    static class SecurityConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("idempotency-key가 빈 문자열이면 400을 반환한다")
    void placeOrder_blankIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("idempotency-key", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", 1, "quantity", 1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("idempotency-key가 공백이면 400을 반환한다")
    void placeOrder_whitespaceIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("idempotency-key", "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", 1, "quantity", 1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효한 idempotency-key와 정상 요청이면 200을 반환한다")
    void placeOrder_validIdempotencyKey_returns200() throws Exception {
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getRole()).thenReturn(UserRole.USER);
        UserDetailsImpl principal = new UserDetailsImpl(mockUser);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);

        when(orderService.placeOrder(eq(1L), any(PlaceOrderRequest.class), eq("valid-uuid")))
                .thenReturn(new PlaceOrderResponse(1L, OrderStatus.PENDING, 10000L));

        mockMvc.perform(post("/api/orders")
                        .header("idempotency-key", "valid-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", 1, "quantity", 1))))
                .andExpect(status().isOk());
    }
}
