# Product Search Index EXPLAIN Report

Issue: #19

## Target Query

The optimized query is the product search used by `GET /api/v1/products` and `GET /api/v2/products`.

```sql
SELECT p.*
FROM product p
JOIN category c ON c.id = p.category_id
WHERE p.sale_status <> 'SUSPENDED'
  AND lower(p.name) LIKE concat('%', 'cache-target', '%')
ORDER BY p.search_sort_order ASC, p.id DESC
LIMIT 20;
```

Category-filtered requests add:

```sql
AND p.category_id = 2
```

## Measurement Setup

- Database: MySQL 8.4 from `docker-compose.yml`
- Database name: `explain_issue19`
- Dataset: 50,000 product rows, 5 leaf categories
- Keyword distribution: `cache-target` every 10th product, `wireless` every 3rd product
- Baseline schema: `V1__init_schema.sql`
- After schema: `V2__optimize_product_search_indexes.sql`

## Applied DDL

```sql
ALTER TABLE product
    ADD COLUMN search_sort_order TINYINT NOT NULL DEFAULT 0;

UPDATE product
SET search_sort_order = CASE WHEN sale_status = 'SOLD_OUT' THEN 1 ELSE 0 END;

DROP INDEX idx_product_search ON product;

CREATE INDEX idx_product_search_default
    ON product (search_sort_order, id DESC, sale_status);

CREATE INDEX idx_product_search_category
    ON product (category_id, search_sort_order, id DESC, sale_status);
```

## EXPLAIN Before

### Keyword Only

| table | type | possible_keys | key | rows | Extra |
| --- | --- | --- | --- | ---: | --- |
| p | ALL | fk_product_category, idx_product_search | NULL | 49810 | Using where; Using filesort |
| c | eq_ref | PRIMARY | PRIMARY | 1 | Using index |

### Keyword + Category

| table | type | possible_keys | key | rows | Extra |
| --- | --- | --- | --- | ---: | --- |
| c | const | PRIMARY | PRIMARY | 1 | Using index; Using filesort |
| p | ref | fk_product_category, idx_product_search | fk_product_category | 18568 | Using where |

## EXPLAIN After

### Keyword Only

| table | type | possible_keys | key | rows | Extra |
| --- | --- | --- | --- | ---: | --- |
| p | index | idx_product_search_category | idx_product_search_default | 20 | Using where |
| c | eq_ref | PRIMARY | PRIMARY | 1 | Using index |

### Keyword + Category

| table | type | possible_keys | key | rows | Extra |
| --- | --- | --- | --- | ---: | --- |
| c | const | PRIMARY | PRIMARY | 1 | Using index |
| p | ref | idx_product_search_category | idx_product_search_category | 19310 | Using index condition; Using where |

## Rationale

The original index `(sale_status, category_id, name)` did not match the search shape well because `sale_status <> 'SUSPENDED'` is not selective enough for the leading column, `lower(name) LIKE '%keyword%'` cannot use a normal b-tree prefix range, and the query still needed `SOLD_OUT`-last ordering plus `id DESC`.

`search_sort_order` stores that ordering rule as data so the query can order by an indexed column instead of a runtime `CASE` expression. Two indexes are used because category-filtered and unfiltered searches have different leading access patterns.

## Trade-off

- Read benefit: removes filesort for the measured first-page search scenarios and lets MySQL scan in response order.
- Write cost: product inserts and sale-status changes maintain one small integer column and two secondary indexes.
- Storage cost: one `TINYINT` column plus two product-table secondary indexes.
- Keyword limitation: `%keyword%` matching is still a post-filter. Full-text or n-gram search would be a separate search-quality and indexing decision.
