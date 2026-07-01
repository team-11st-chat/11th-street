package com.elevenst.realtimechat.domain.product.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
@Disabled("Local database seeder should not run as part of automated CI tests")
class ProductSearchLocalDatabaseSeederTest {

    @Autowired
    private ProductSearchDummyDataSeeder dummyDataSeeder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seedData() {
        System.out.println("Ensuring seller member exists...");
        jdbcTemplate.execute(
            "INSERT INTO member (id, email, password_hash, name, role, status, created_at, updated_at) " +
            "VALUES (1, 'seller@test.com', 'password', 'Seller', 'SELLER', 'ACTIVE', NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE id=id"
        );

        System.out.println("Starting dummy data seeding...");
        ProductSearchDummyDataSeeder.SeedResult result = dummyDataSeeder.resetAndSeedProducts(50000);
        System.out.println("Seeding completed successfully! Product count: " + result.productCount());
    }
}
