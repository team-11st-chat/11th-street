package com.elevenst.realtimechat.domain.product.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductSearchSortOrderTest {

    @Test
    void createProduct_setsSearchSortOrderFromInitialSaleStatus() {
        Category category = leafCategory();

        Product onSaleProduct = Product.create(1L, category, "Wireless Keyboard", BigDecimal.valueOf(10000), 10);
        Product soldOutProduct = Product.create(1L, category, "Wireless Mouse", BigDecimal.valueOf(10000), 0);

        assertThat(onSaleProduct.getSearchSortOrder()).isZero();
        assertThat(soldOutProduct.getSearchSortOrder()).isOne();
    }

    @Test
    void updateProduct_refreshesSearchSortOrderWhenSaleStatusChanges() {
        Product product = Product.create(1L, leafCategory(), "Wireless Keyboard", BigDecimal.valueOf(10000), 10);

        product.update(1L, null, null, null, null, SaleStatus.SUSPENDED);

        assertThat(product.getSaleStatus()).isEqualTo(SaleStatus.SUSPENDED);
        assertThat(product.getSearchSortOrder()).isZero();
    }

    @Test
    void decreaseStock_refreshesSearchSortOrderWhenProductBecomesSoldOut() {
        Product product = Product.create(1L, leafCategory(), "Wireless Keyboard", BigDecimal.valueOf(10000), 1);

        product.decreaseStock(1);

        assertThat(product.getSaleStatus()).isEqualTo(SaleStatus.SOLD_OUT);
        assertThat(product.getSearchSortOrder()).isOne();
    }

    private Category leafCategory() {
        return Category.createChild(Category.createRoot("Electronics", 1), "Keyboard", 1);
    }
}
