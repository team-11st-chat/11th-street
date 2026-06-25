package com.elevenst.realtimechat.domain.product.repository;

import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.entity.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(
            value = """
                    select p
                    from Product p
                    join fetch p.category
                    where p.saleStatus <> :suspendedStatus
                      and (:keyword is null or lower(p.name) like concat('%', :keyword, '%'))
                      and (:categoryId is null or p.category.id = :categoryId)
                    order by
                      case when p.saleStatus = :soldOutStatus then 1 else 0 end asc,
                      p.id desc
                    """,
            countQuery = """
                    select count(p)
                    from Product p
                    where p.saleStatus <> :suspendedStatus
                      and (:keyword is null or lower(p.name) like concat('%', :keyword, '%'))
                      and (:categoryId is null or p.category.id = :categoryId)
                    """
    )
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("suspendedStatus") SaleStatus suspendedStatus,
            @Param("soldOutStatus") SaleStatus soldOutStatus,
            Pageable pageable
    );
}
