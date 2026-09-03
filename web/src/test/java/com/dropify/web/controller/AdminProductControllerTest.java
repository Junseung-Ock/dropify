package com.dropify.web.controller;

import com.dropify.product.service.ProductService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminProductController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("상품 등록 시 stockQuantity 누락하면 400 반환")
    void create_missingStockQuantity_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "테스트 상품",
                "price", 1000
        );

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("상품 수정 시 stockQuantity 누락하면 400 반환")
    void update_missingStockQuantity_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "테스트 상품",
                "price", 1000
        );

        mockMvc.perform(put("/api/admin/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("상태 변경 시 올바르지 않은 status 값이면 400 반환")
    void changeStatus_invalidStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/admin/products/1/status")
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("상품 등록 시 가격이 음수면 400 반환")
    void create_negativePrice_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "테스트 상품",
                "price", -1000,
                "stockQuantity", 10
        );

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
