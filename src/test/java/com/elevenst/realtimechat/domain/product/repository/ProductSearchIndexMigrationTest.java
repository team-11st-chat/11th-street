package com.elevenst.realtimechat.domain.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Tag("integration")
class ProductSearchIndexMigrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("migration_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void productSearchIndexMigrationAppliesColumnBackfillAndIndexes() throws SQLException {
        migrateToVersion("1");
        insertProductRowsBeforeSearchSortOrderExists();

        migrateToLatest();

        try (Connection connection = getConnection()) {
            assertSearchSortOrderColumnType(connection);
            assertSearchSortOrderBackfilled(connection);
            assertProductSearchIndexesCreated(connection);
            assertSearchOrderUsesBackfilledSortValue(connection);
        }
    }

    private void migrateToVersion(String version) {
        Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .target(version)
                .load()
                .migrate();
    }

    private void migrateToLatest() {
        Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private void insertProductRowsBeforeSearchSortOrderExists() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO member (id, email, password_hash, name, role, status, created_at, updated_at)
                    VALUES (1, 'seller@example.com', 'hash', 'seller', 'SELLER', 'ACTIVE', NOW(), NOW())
                    """);
            statement.executeUpdate("""
                    INSERT INTO category (id, parent_id, name, depth, sort_order, created_at, updated_at)
                    VALUES
                        (1, NULL, 'Electronics', 1, 1, NOW(), NOW()),
                        (2, 1, 'Audio', 2, 1, NOW(), NOW())
                    """);
            statement.executeUpdate("""
                    INSERT INTO product (id, seller_id, category_id, name, price, stock_quantity, sale_status, created_at, updated_at)
                    VALUES
                        (1, 1, 2, 'Wireless Earbuds A', 10000.00, 10, 'ON_SALE', NOW(), NOW()),
                        (2, 1, 2, 'Wireless Earbuds B', 10000.00, 0, 'SOLD_OUT', NOW(), NOW()),
                        (3, 1, 2, 'Wireless Earbuds C', 10000.00, 10, 'SUSPENDED', NOW(), NOW())
                    """);
        }
    }

    private void assertSearchSortOrderColumnType(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT DATA_TYPE
                     FROM INFORMATION_SCHEMA.COLUMNS
                     WHERE TABLE_SCHEMA = DATABASE()
                       AND TABLE_NAME = 'product'
                       AND COLUMN_NAME = 'search_sort_order'
                     """)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("DATA_TYPE")).isEqualTo("int");
        }
    }

    private void assertSearchSortOrderBackfilled(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT sale_status, search_sort_order
                     FROM product
                     ORDER BY id
                     """)) {
            assertThat(readRows(resultSet)).containsExactly(
                    new ProductSortRow("ON_SALE", 0),
                    new ProductSortRow("SOLD_OUT", 1),
                    new ProductSortRow("SUSPENDED", 0)
            );
        }
    }

    private void assertProductSearchIndexesCreated(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, COLLATION
                     FROM INFORMATION_SCHEMA.STATISTICS
                     WHERE TABLE_SCHEMA = DATABASE()
                       AND TABLE_NAME = 'product'
                       AND INDEX_NAME IN ('idx_product_search_default', 'idx_product_search_category')
                     ORDER BY INDEX_NAME, SEQ_IN_INDEX
                     """)) {
            assertThat(readIndexRows(resultSet)).containsExactly(
                    new IndexRow("idx_product_search_category", 1, "category_id", "A"),
                    new IndexRow("idx_product_search_category", 2, "search_sort_order", "A"),
                    new IndexRow("idx_product_search_category", 3, "id", "D"),
                    new IndexRow("idx_product_search_category", 4, "sale_status", "A"),
                    new IndexRow("idx_product_search_default", 1, "search_sort_order", "A"),
                    new IndexRow("idx_product_search_default", 2, "id", "D"),
                    new IndexRow("idx_product_search_default", 3, "sale_status", "A")
            );
        }
    }

    private void assertSearchOrderUsesBackfilledSortValue(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT id
                     FROM product
                     WHERE sale_status <> 'SUSPENDED'
                     ORDER BY search_sort_order ASC, id DESC
                     """)) {
            assertThat(readIds(resultSet)).containsExactly(1L, 2L);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        );
    }

    private List<ProductSortRow> readRows(ResultSet resultSet) throws SQLException {
        List<ProductSortRow> rows = new ArrayList<>();
        while (resultSet.next()) {
            rows.add(new ProductSortRow(
                    resultSet.getString("sale_status"),
                    resultSet.getInt("search_sort_order")
            ));
        }
        return rows;
    }

    private List<IndexRow> readIndexRows(ResultSet resultSet) throws SQLException {
        List<IndexRow> rows = new ArrayList<>();
        while (resultSet.next()) {
            rows.add(new IndexRow(
                    resultSet.getString("INDEX_NAME"),
                    resultSet.getInt("SEQ_IN_INDEX"),
                    resultSet.getString("COLUMN_NAME"),
                    resultSet.getString("COLLATION")
            ));
        }
        return rows;
    }

    private List<Long> readIds(ResultSet resultSet) throws SQLException {
        List<Long> ids = new ArrayList<>();
        while (resultSet.next()) {
            ids.add(resultSet.getLong("id"));
        }
        return ids;
    }

    private record ProductSortRow(String saleStatus, int searchSortOrder) {
    }

    private record IndexRow(String indexName, int sequence, String columnName, String collation) {
    }
}
