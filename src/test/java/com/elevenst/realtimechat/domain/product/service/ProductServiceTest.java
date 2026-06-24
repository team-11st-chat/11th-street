package com.elevenst.realtimechat.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elevenst.realtimechat.domain.product.dto.ProductCreateRequest;
import com.elevenst.realtimechat.domain.product.dto.ProductResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductUpdateRequest;
import com.elevenst.realtimechat.domain.product.entity.Category;
import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.entity.SaleStatus;
import com.elevenst.realtimechat.domain.product.exception.ProductException;
import com.elevenst.realtimechat.domain.product.repository.CategoryRepository;
import com.elevenst.realtimechat.domain.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryRepository);
    }

    @Test
    void createProduct_registersProductWithOnSaleStatus() {
        Category category = Category.createChild(Category.createRoot("디지털·가전", 1), "이어폰", 1);
        Product savedProduct = Product.create(1L, category, "무선 이어폰", new BigDecimal("89000"), 500);

        when(categoryRepository.findById(11L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(
                1L,
                new ProductCreateRequest("무선 이어폰", 11L, new BigDecimal("89000"), 500)
        );

        assertThat(response.sellerId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("무선 이어폰");
        assertThat(response.saleStatus()).isEqualTo(SaleStatus.ON_SALE);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_rejectsRootCategory() {
        Category rootCategory = Category.createRoot("디지털·가전", 1);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));

        assertThatThrownBy(() -> productService.createProduct(
                1L,
                new ProductCreateRequest("무선 이어폰", 1L, new BigDecimal("89000"), 500)
        )).isInstanceOf(ProductException.class)
                .hasMessage("상품은 중분류 카테고리에만 등록할 수 있습니다.");
    }

    @Test
    void updateProduct_changesProductWhenSellerOwnsIt() {
        Category category = Category.createChild(Category.createRoot("디지털·가전", 1), "이어폰", 1);
        Product product = Product.create(1L, category, "무선 이어폰", new BigDecimal("89000"), 500);

        when(productRepository.findById(1001L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.updateProduct(
                1L,
                1001L,
                new ProductUpdateRequest("무선 이어폰 Pro", null, new BigDecimal("99000"), 300, SaleStatus.ON_SALE)
        );

        assertThat(response.name()).isEqualTo("무선 이어폰 Pro");
        assertThat(response.price()).isEqualByComparingTo("99000");
        assertThat(response.stockQuantity()).isEqualTo(300);
    }

    @Test
    void updateProduct_rejectsOtherSellerProduct() {
        Category category = Category.createChild(Category.createRoot("디지털·가전", 1), "이어폰", 1);
        Product product = Product.create(1L, category, "무선 이어폰", new BigDecimal("89000"), 500);

        when(productRepository.findById(1001L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.updateProduct(
                2L,
                1001L,
                new ProductUpdateRequest("무선 이어폰 Pro", null, null, null, null)
        )).isInstanceOf(ProductException.class)
                .hasMessage("해당 판매자의 상품이 아닙니다.");
    }

    @Test
    void updateProduct_rejectsOnSaleWithoutStock() {
        Category category = Category.createChild(Category.createRoot("디지털·가전", 1), "이어폰", 1);
        Product product = Product.create(1L, category, "무선 이어폰", new BigDecimal("89000"), 500);

        when(productRepository.findById(1001L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.updateProduct(
                1L,
                1001L,
                new ProductUpdateRequest(null, null, null, 0, SaleStatus.ON_SALE)
        )).isInstanceOf(ProductException.class)
                .hasMessage("판매 상태와 재고 수량이 올바르지 않습니다.");
    }
}
