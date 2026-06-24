package com.elevenst.realtimechat.domain.product.repository;

import com.elevenst.realtimechat.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
