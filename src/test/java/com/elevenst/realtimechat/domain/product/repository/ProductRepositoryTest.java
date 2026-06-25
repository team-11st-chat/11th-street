package com.elevenst.realtimechat.domain.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.elevenst.realtimechat.domain.product.entity.Category;
import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.entity.SaleStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Tag("integration")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        Category rootCategory = Category.createRoot("디지털·가전", 1);
        categoryRepository.save(rootCategory);

        testCategory = Category.createChild(rootCategory, "이어폰", 1);
        categoryRepository.save(testCategory);
    }

    @Test
    @DisplayName("SUSPENDED 상태의 상품은 검색 결과에서 제외된다")
    void searchProducts_excludesSuspendedProducts() {
        // given
        Product onSaleProduct = Product.create(1L, testCategory, "무선 이어폰 A", new BigDecimal("50000"), 10);
        Product suspendedProduct = Product.create(1L, testCategory, "무선 이어폰 B", new BigDecimal("60000"), 10);

        productRepository.save(onSaleProduct);
        productRepository.save(suspendedProduct);

        suspendedProduct.update(1L, null, null, null, null, SaleStatus.SUSPENDED);
        productRepository.save(suspendedProduct);

        // when
        Page<Product> result = productRepository.searchProducts(
                "이어폰", null, SaleStatus.SUSPENDED, SaleStatus.SOLD_OUT, PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("무선 이어폰 A");
    }

    @Test
    @DisplayName("SOLD_OUT 상태의 상품은 ON_SALE 상태의 상품보다 뒤에 정렬된다")
    void searchProducts_sortsSoldOutProductsAtTheBottom() {
        // given
        Product onSale1 = Product.create(1L, testCategory, "무선 이어폰 A", new BigDecimal("50000"), 10);
        Product onSale2 = Product.create(1L, testCategory, "무선 이어폰 B", new BigDecimal("50000"), 10);
        Product soldOut1 = Product.create(1L, testCategory, "무선 이어폰 C", new BigDecimal("50000"), 0);
        Product onSale3 = Product.create(1L, testCategory, "무선 이어폰 D", new BigDecimal("50000"), 10);

        productRepository.save(onSale1);
        productRepository.save(onSale2);
        productRepository.save(soldOut1);
        productRepository.save(onSale3);

        // when
        Page<Product> result = productRepository.searchProducts(
                "이어폰", null, SaleStatus.SUSPENDED, SaleStatus.SOLD_OUT, PageRequest.of(0, 10)
        );

        // then
        List<Product> products = result.getContent();
        assertThat(products).hasSize(4);

        assertThat(products.get(0).getName()).isEqualTo("무선 이어폰 D");
        assertThat(products.get(1).getName()).isEqualTo("무선 이어폰 B");
        assertThat(products.get(2).getName()).isEqualTo("무선 이어폰 A");
        assertThat(products.get(3).getName()).isEqualTo("무선 이어폰 C");
    }

    @Test
    @DisplayName("카테고리 ID가 지정되면 해당 카테고리의 상품만 필터링된다")
    void searchProducts_filtersByCategoryId() {
        // given
        Category otherCategory = Category.createChild(testCategory.getParent(), "헤드폰", 2);
        categoryRepository.save(otherCategory);

        Product earphone = Product.create(1L, testCategory, "무선 이어폰", new BigDecimal("50000"), 10);
        Product headphone = Product.create(1L, otherCategory, "무선 헤드폰", new BigDecimal("150000"), 10);

        productRepository.save(earphone);
        productRepository.save(headphone);

        // when
        Page<Product> result = productRepository.searchProducts(
                "무선", testCategory.getId(), SaleStatus.SUSPENDED, SaleStatus.SOLD_OUT, PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("무선 이어폰");
    }

    @Test
    @DisplayName("키워드가 공백이거나 대소문자가 달라도 검색이 올바르게 수행된다")
    void searchProducts_searchesWithCaseInsensitiveKeyword() {
        // given
        Product product1 = Product.create(1L, testCategory, "Apple Airpods", new BigDecimal("250000"), 10);
        Product product2 = Product.create(1L, testCategory, "Samsung Buds", new BigDecimal("150000"), 10);

        productRepository.save(product1);
        productRepository.save(product2);

        // when
        Page<Product> result = productRepository.searchProducts(
                "apple", null, SaleStatus.SUSPENDED, SaleStatus.SOLD_OUT, PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Apple Airpods");
    }
}
