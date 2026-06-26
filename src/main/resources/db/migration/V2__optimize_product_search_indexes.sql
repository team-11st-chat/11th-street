-- Product search index optimization for issue #19.
-- search_sort_order mirrors Product.searchSortOrder so MySQL can index the
-- SOLD_OUT-last ordering used by the search query.

ALTER TABLE product
    ADD COLUMN search_sort_order TINYINT NOT NULL DEFAULT 0;

UPDATE product
SET search_sort_order = CASE WHEN sale_status = 'SOLD_OUT' THEN 1 ELSE 0 END;

DROP INDEX idx_product_search ON product;

CREATE INDEX idx_product_search_default
    ON product (search_sort_order, id DESC, sale_status);

CREATE INDEX idx_product_search_category
    ON product (category_id, search_sort_order, id DESC, sale_status);
