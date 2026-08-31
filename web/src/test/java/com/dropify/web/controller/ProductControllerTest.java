package com.dropify.web.controller;

import com.dropify.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProductController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("검색 시 minPrice가 maxPrice보다 크면 400 반환")
    void search_invalidPriceRange_returns400() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("minPrice", "5000")
                        .param("maxPrice", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("검색 시 정상적인 가격 범위면 200 반환")
    void search_validPriceRange_returns200() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("minPrice", "1000")
                        .param("maxPrice", "5000"))
                .andExpect(status().isOk());
    }
}
