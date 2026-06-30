package com.elevenst.realtimechat.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elevenst.realtimechat.domain.product.dto.ProductCreateRequest;
import com.elevenst.realtimechat.domain.product.dto.ProductPageResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductSummaryResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductUpdateRequest;
import com.elevenst.realtimechat.domain.category.entity.Category;
import com.elevenst.realtimechat.domain.category.exception.CategoryErrorCode;
import com.elevenst.realtimechat.domain.category.exception.CategoryException;
import com.elevenst.realtimechat.domain.category.service.CategoryQueryService;
import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.entity.SaleStatus;
import com.elevenst.realtimechat.domain.product.exception.ProductException;
import com.elevenst.realtimechat.domain.product.repository.ProductRepository;
import com.elevenst.realtimechat.domain.search.service.SearchKeywordRecordCommand;
import com.elevenst.realtimechat.domain.search.service.SearchKeywordRecorder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryQueryService categoryQueryService;

    @Mock
    private SearchKeywordRecorder searchKeywordRecorder;

    @Mock
    private ProductSearchService productSearchService;

    @Mock
    private ProductSearchCacheEvictor productSearchCacheEvictor;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository,
                categoryQueryService,
                searchKeywordRecorder,
                productSearchService,
                new ProductSearchCacheProperties(ProductSearchCacheProperties.Mode.LOCAL),
                productSearchCacheEvictor
        );
    }

    @Test
    void createProduct_registersProductWithOnSaleStatus() {
        Category category = Category.createChild(Category.createRoot("디지털·가전", 1), "이어폰", 1);
        Product savedProduct = Product.create(1L, category, "무선 이어폰", new BigDecimal("89000"), 500);

        when(categoryQueryService.getCategoryOrThrow(11L)).thenReturn(category);
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

        when(categoryQueryService.getCategoryOrThrow(1L)).thenReturn(rootCategory);

        assertThatThrownBy(() -> productService.createProduct(
                1L,
                new ProductCreateRequest("무선 이어폰", 1L, new BigDecimal("89000"), 500)
        )).isInstanceOf(ProductException.class)
                .hasMessage("상품은 중분류 카테고리에만 등록할 수 있습니다.");
    }

    @Test
    void createProduct_throwsCategoryException_whenCategoryIsNotFound() {
        when(categoryQueryService.getCategoryOrThrow(404L))
                .thenThrow(new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        assertThatThrownBy(() -> productService.createProduct(
                1L,
                new ProductCreateRequest("Wireless Earbuds", 404L, new BigDecimal("89000"), 500)
        )).isInstanceOf(CategoryException.class);
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

    @Test
    void updateProduct_rejectsBlankName() {
        Category category = Category.createChild(Category.createRoot("Electronics", 1), "Audio", 1);
        Product product = Product.create(1L, category, "Wireless Earbuds", new BigDecimal("89000"), 500);

        when(productRepository.findById(1001L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.updateProduct(
                1L,
                1001L,
                new ProductUpdateRequest("   ", null, null, null, null)
        )).isInstanceOf(ProductException.class);
    }

    @Test
    void searchProducts_returnsPagedProducts_whenInputsAreValid() {
        Category category = Category.createChild(Category.createRoot("디지털·가전", 1), "이어폰", 1);
        Product product = Product.create(1L, category, "무선 이어폰", new BigDecimal("89000"), 500);
        Page<Product> productPage = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
        ProductPageResponse expectedResponse = ProductPageResponse.from(productPage.map(ProductSummaryResponse::from));

        when(productSearchService.searchProducts(eq("이어폰"), eq(11L), eq(0), eq(10)))
                .thenReturn(expectedResponse);

        var response = productService.searchProducts("  이어폰  ", 11L, 0, 10, "guest_123");

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("무선 이어폰");
        verify(searchKeywordRecorder).record(SearchKeywordRecordCommand.guest("  이어폰  ", "guest_123", 11L));
    }

    @Test
    void searchProducts_throwsException_whenPageIsInvalid() {
        assertThatThrownBy(() -> productService.searchProducts("이어폰", 11L, -1, 10, "guest_123"))
                .isInstanceOf(ProductException.class)
                .hasMessage("페이징 파라미터가 올바르지 않습니다.");
    }

    @Test
    void searchProductsV2_throwsException_whenSizeExceedsLimit() {
        assertThatThrownBy(() -> productService.searchProductsV2("이어폰", 11L, 0, 101, "guest_123"))
                .isInstanceOf(ProductException.class)
                .hasMessage("페이징 파라미터가 올바르지 않습니다.");
    }

    @Test
    void searchProductsV2_usesRemoteCacheWhenConfigured() {
        ProductPageResponse expectedResponse = ProductPageResponse.from(
                new PageImpl<Product>(List.of()).map(ProductSummaryResponse::from)
        );
        productService = new ProductService(
                productRepository,
                categoryQueryService,
                searchKeywordRecorder,
                productSearchService,
                new ProductSearchCacheProperties(ProductSearchCacheProperties.Mode.REMOTE),
                productSearchCacheEvictor
        );

        when(productSearchService.searchProductsWithRemoteCache(eq("airpods"), eq(11L), eq(0), eq(20)))
                .thenReturn(expectedResponse);

        ProductPageResponse response = productService.searchProductsV2("AirPods", 11L, 0, 20, "guest_123");

        assertThat(response).isEqualTo(expectedResponse);
        verify(productSearchService).searchProductsWithRemoteCache("airpods", 11L, 0, 20);
    }

    @Test
    void searchProducts_doesNotRecordKeyword_whenKeywordIsBlank() {
        Page<Product> emptyPage = new PageImpl<>(List.of());
        when(productSearchService.searchProducts(isNull(), eq(11L), eq(0), eq(10)))
                .thenReturn(ProductPageResponse.from(emptyPage.map(ProductSummaryResponse::from)));

        productService.searchProducts("   ", 11L, 0, 10, "guest_123");

        verify(searchKeywordRecorder, org.mockito.Mockito.never()).record(any(SearchKeywordRecordCommand.class));
    }

    @Test
    void searchProducts_succeeds_evenWhenSearchKeywordRecordFails() {
        Category category = Category.createChild(Category.createRoot("디지털·가전", 1), "이어폰", 1);
        Product product = Product.create(1L, category, "무선 이어폰", new BigDecimal("89000"), 500);
        Page<Product> productPage = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
        ProductPageResponse expectedResponse = ProductPageResponse.from(productPage.map(ProductSummaryResponse::from));

        when(productSearchService.searchProducts(eq("이어폰"), eq(11L), eq(0), eq(10)))
                .thenReturn(expectedResponse);
        doThrow(new RuntimeException("Database error"))
                .when(searchKeywordRecorder).record(any(SearchKeywordRecordCommand.class));

        var response = productService.searchProducts("이어폰", 11L, 0, 10, "guest_123");

        assertThat(response.content()).hasSize(1);
        verify(searchKeywordRecorder).record(any(SearchKeywordRecordCommand.class));
    }
}
