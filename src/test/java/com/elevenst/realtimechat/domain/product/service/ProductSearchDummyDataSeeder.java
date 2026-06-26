package com.elevenst.realtimechat.domain.product.service;

import com.elevenst.realtimechat.domain.product.entity.Category;
import com.elevenst.realtimechat.domain.product.repository.CategoryRepository;
import com.elevenst.realtimechat.domain.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class ProductSearchDummyDataSeeder {

    static final int DEFAULT_PRODUCT_COUNT = 50_000;
    static final String PRIMARY_KEYWORD = "cache-target";
    static final String SECONDARY_KEYWORD = "wireless";

    private static final long SELLER_ID = 1L;
    private static final int CATEGORY_COUNT = 5;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final JdbcTemplate jdbcTemplate;

    ProductSearchDummyDataSeeder(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    SeedResult resetAndSeedDefaultProducts() {
        return resetAndSeedProducts(DEFAULT_PRODUCT_COUNT);
    }

    SeedResult resetAndSeedProducts(int productCount) {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category root = categoryRepository.save(Category.createRoot("Performance", 1));
        List<Category> leaves = java.util.stream.IntStream.rangeClosed(1, CATEGORY_COUNT)
                .mapToObj(i -> categoryRepository.save(Category.createChild(root, "Search-" + i, i)))
                .toList();

        insertProducts(productCount, leaves);

        return new SeedResult(productCount, root.getId(), leaves.stream().map(Category::getId).toList());
    }

    private void insertProducts(int productCount, List<Category> leaves) {
        String sql = "INSERT INTO product (seller_id, category_id, name, price, stock_quantity, sale_status, search_sort_order, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Category category = leaves.get(i % leaves.size());
                ps.setLong(1, SELLER_ID);
                ps.setLong(2, category.getId());
                ps.setString(3, productName(i));
                ps.setBigDecimal(4, BigDecimal.valueOf(10_000L + i));
                ps.setInt(5, stockQuantity(i));
                String saleStatus = saleStatus(i);
                ps.setString(6, saleStatus);
                ps.setInt(7, searchSortOrder(saleStatus));
                ps.setTimestamp(8, Timestamp.valueOf(now.minusSeconds(productCount - i)));
                ps.setTimestamp(9, Timestamp.valueOf(now.minusSeconds(productCount - i)));
            }

            @Override
            public int getBatchSize() {
                return productCount;
            }
        });
    }

    private String productName(int index) {
        if (index % 10 == 0) {
            return "Cache-Target Wireless Product " + index;
        }
        if (index % 3 == 0) {
            return "Wireless Product " + index;
        }
        return "Catalog Product " + index;
    }

    private int stockQuantity(int index) {
        return index % 10 == 0 ? 0 : 100;
    }

    private String saleStatus(int index) {
        return index % 10 == 0 ? "SOLD_OUT" : "ON_SALE";
    }

    private int searchSortOrder(String saleStatus) {
        return "SOLD_OUT".equals(saleStatus) ? 1 : 0;
    }

    record SeedResult(int productCount, Long rootCategoryId, List<Long> leafCategoryIds) {
    }
}
