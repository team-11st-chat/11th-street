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
        Category rootCategory = Category.createRoot("Electronics", 1);
        categoryRepository.save(rootCategory);

        testCategory = Category.createChild(rootCategory, "Audio", 1);
        categoryRepository.save(testCategory);
    }

    @Test
    @DisplayName("SUSPENDED products are excluded from search results")
    void searchProducts_excludesSuspendedProducts() {
        Product onSaleProduct = Product.create(1L, testCategory, "Wireless Earbuds A", new BigDecimal("50000"), 10);
        Product suspendedProduct = Product.create(1L, testCategory, "Wireless Earbuds B", new BigDecimal("60000"), 10);

        productRepository.save(onSaleProduct);
        productRepository.save(suspendedProduct);

        suspendedProduct.update(1L, null, null, null, null, SaleStatus.SUSPENDED);
        productRepository.save(suspendedProduct);

        Page<Product> result = productRepository.searchProducts(
                "earbuds", null, SaleStatus.SUSPENDED, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Wireless Earbuds A");
    }

    @Test
    @DisplayName("SOLD_OUT products are sorted below ON_SALE products")
    void searchProducts_sortsSoldOutProductsAtTheBottom() {
        Product onSale1 = Product.create(1L, testCategory, "Wireless Earbuds A", new BigDecimal("50000"), 10);
        Product onSale2 = Product.create(1L, testCategory, "Wireless Earbuds B", new BigDecimal("50000"), 10);
        Product soldOut1 = Product.create(1L, testCategory, "Wireless Earbuds C", new BigDecimal("50000"), 0);
        Product onSale3 = Product.create(1L, testCategory, "Wireless Earbuds D", new BigDecimal("50000"), 10);

        productRepository.save(onSale1);
        productRepository.save(onSale2);
        productRepository.save(soldOut1);
        productRepository.save(onSale3);

        Page<Product> result = productRepository.searchProducts(
                "earbuds", null, SaleStatus.SUSPENDED, PageRequest.of(0, 10)
        );

        List<Product> products = result.getContent();
        assertThat(products).hasSize(4);
        assertThat(products.get(0).getName()).isEqualTo("Wireless Earbuds D");
        assertThat(products.get(1).getName()).isEqualTo("Wireless Earbuds B");
        assertThat(products.get(2).getName()).isEqualTo("Wireless Earbuds A");
        assertThat(products.get(3).getName()).isEqualTo("Wireless Earbuds C");
    }

    @Test
    @DisplayName("Category id filters product search results")
    void searchProducts_filtersByCategoryId() {
        Category otherCategory = Category.createChild(testCategory.getParent(), "Headphones", 2);
        categoryRepository.save(otherCategory);

        Product earphone = Product.create(1L, testCategory, "Wireless Earbuds", new BigDecimal("50000"), 10);
        Product headphone = Product.create(1L, otherCategory, "Wireless Headphones", new BigDecimal("150000"), 10);

        productRepository.save(earphone);
        productRepository.save(headphone);

        Page<Product> result = productRepository.searchProducts(
                "wireless", testCategory.getId(), SaleStatus.SUSPENDED, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Wireless Earbuds");
    }

    @Test
    @DisplayName("Search keyword is case insensitive")
    void searchProducts_searchesWithCaseInsensitiveKeyword() {
        Product product1 = Product.create(1L, testCategory, "Apple Airpods", new BigDecimal("250000"), 10);
        Product product2 = Product.create(1L, testCategory, "Samsung Buds", new BigDecimal("150000"), 10);

        productRepository.save(product1);
        productRepository.save(product2);

        Page<Product> result = productRepository.searchProducts(
                "apple", null, SaleStatus.SUSPENDED, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Apple Airpods");
    }
}
