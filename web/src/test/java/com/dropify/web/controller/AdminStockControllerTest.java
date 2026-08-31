package com.dropify.web.controller;

import com.dropify.product.dto.response.StockReplenishResponse;
import com.dropify.product.service.StockService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminStockController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AdminStockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StockService stockService;

    @Test
    @DisplayName("유효한 재고 보충 요청이면 200을 반환한다")
    void replenish_valid_returns200() throws Exception {
        when(stockService.replenishStock(eq(1L), any()))
                .thenReturn(new StockReplenishResponse(1L, 20));

        mockMvc.perform(post("/api/admin/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("quantity", 10))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("재고 보충 시 quantity 누락이면 400을 반환한다")
    void replenish_missingQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("재고 보충 시 quantity가 0이면 400을 반환한다")
    void replenish_zeroQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("quantity", 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("재고 보충 시 quantity가 음수이면 400을 반환한다")
    void replenish_negativeQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("quantity", -1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("재고 보충 시 reason이 255자를 초과하면 400을 반환한다")
    void replenish_reasonTooLong_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "quantity", 10,
                                "reason", "a".repeat(256)
                        ))))
                .andExpect(status().isBadRequest());
    }
}
