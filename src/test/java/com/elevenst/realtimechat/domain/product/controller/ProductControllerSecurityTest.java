package com.elevenst.realtimechat.domain.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.elevenst.realtimechat.domain.member.entity.MemberRole;
import com.elevenst.realtimechat.domain.product.dto.ProductCreateRequest;
import com.elevenst.realtimechat.domain.product.dto.ProductResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductUpdateRequest;
import com.elevenst.realtimechat.domain.product.entity.SaleStatus;
import com.elevenst.realtimechat.domain.product.service.ProductService;
import com.elevenst.realtimechat.global.security.AuthenticatedMember;
import com.elevenst.realtimechat.global.security.JwtTokenProvider;
import com.elevenst.realtimechat.global.security.token.AccessTokenBlacklist;
import com.elevenst.realtimechat.global.security.token.TokenInvalidationRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class ProductControllerSecurityTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlacklist accessTokenBlacklist;

    @MockitoBean
    private TokenInvalidationRegistry tokenInvalidationRegistry;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        SecurityContextHolder.clearContext();
    }

    @Test
    void createProduct_withSellerRole_returnsCreated() throws Exception {
        // Given
        ProductCreateRequest request = new ProductCreateRequest("무선 이어폰", 11L, new BigDecimal("89000"), 500);
        ProductResponse response = new ProductResponse(1001L, 1L, 11L, "무선 이어폰", new BigDecimal("89000"), 500, SaleStatus.ON_SALE);
        
        AuthenticatedMember authenticatedMember = new AuthenticatedMember(1L, MemberRole.SELLER);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                authenticatedMember, null, List.of(new SimpleGrantedAuthority("ROLE_SELLER"))
        );

        when(productService.createProduct(eq(1L), any(ProductCreateRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/products")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createProduct_withBuyerRole_returnsForbidden() throws Exception {
        // Given
        ProductCreateRequest request = new ProductCreateRequest("무선 이어폰", 11L, new BigDecimal("89000"), 500);
        
        AuthenticatedMember authenticatedMember = new AuthenticatedMember(1L, MemberRole.BUYER);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                authenticatedMember, null, List.of(new SimpleGrantedAuthority("ROLE_BUYER"))
        );

        // When & Then
        mockMvc.perform(post("/api/v1/products")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_anonymousUser_returnsUnauthorized() throws Exception {
        // Given
        ProductCreateRequest request = new ProductCreateRequest("무선 이어폰", 11L, new BigDecimal("89000"), 500);

        // When & Then
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProduct_withSellerRole_returnsOk() throws Exception {
        // Given
        ProductUpdateRequest request = new ProductUpdateRequest("무선 이어폰 Pro", null, null, null, SaleStatus.ON_SALE);
        ProductResponse response = new ProductResponse(1001L, 1L, 11L, "무선 이어폰 Pro", new BigDecimal("99000"), 300, SaleStatus.ON_SALE);
        
        AuthenticatedMember authenticatedMember = new AuthenticatedMember(1L, MemberRole.SELLER);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                authenticatedMember, null, List.of(new SimpleGrantedAuthority("ROLE_SELLER"))
        );

        when(productService.updateProduct(eq(1L), eq(1001L), any(ProductUpdateRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(patch("/api/v1/products/1001")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateProduct_withBuyerRole_returnsForbidden() throws Exception {
        // Given
        ProductUpdateRequest request = new ProductUpdateRequest("무선 이어폰 Pro", null, null, null, SaleStatus.ON_SALE);
        
        AuthenticatedMember authenticatedMember = new AuthenticatedMember(1L, MemberRole.BUYER);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                authenticatedMember, null, List.of(new SimpleGrantedAuthority("ROLE_BUYER"))
        );

        // When & Then
        mockMvc.perform(patch("/api/v1/products/1001")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProduct_anonymousUser_returnsUnauthorized() throws Exception {
        // Given
        ProductUpdateRequest request = new ProductUpdateRequest("무선 이어폰 Pro", null, null, null, SaleStatus.ON_SALE);

        // When & Then
        mockMvc.perform(patch("/api/v1/products/1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
