package com.elevenst.realtimechat.domain.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductSearchIndexMigrationTest {

    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/V2__optimize_product_search_indexes.sql"
    );

    @Test
    void productSearchIndexMigrationAddsSortColumnBackfillAndIndexes() throws IOException {
        String migration = Files.readString(MIGRATION);

        assertThat(migration)
                .contains("ADD COLUMN search_sort_order TINYINT NOT NULL DEFAULT 0")
                .contains("SET search_sort_order = CASE WHEN sale_status = 'SOLD_OUT' THEN 1 ELSE 0 END")
                .contains("DROP INDEX idx_product_search ON product")
                .contains("CREATE INDEX idx_product_search_default")
                .contains("ON product (search_sort_order, id DESC, sale_status)")
                .contains("CREATE INDEX idx_product_search_category")
                .contains("ON product (category_id, search_sort_order, id DESC, sale_status)");
    }
}
