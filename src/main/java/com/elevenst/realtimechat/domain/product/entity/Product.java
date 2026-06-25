package com.elevenst.realtimechat.domain.product.entity;

import com.elevenst.realtimechat.domain.product.exception.ProductErrorCode;
import com.elevenst.realtimechat.domain.product.exception.ProductException;
import com.elevenst.realtimechat.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sellerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus saleStatus;

    public static Product create(Long sellerId, Category category, String name, BigDecimal price, int stockQuantity) {
        validateSellerId(sellerId);
        validateCategory(category);
        validateName(name);
        validatePrice(price);
        validateStockQuantity(stockQuantity);

        SaleStatus status = stockQuantity == 0 ? SaleStatus.SOLD_OUT : SaleStatus.ON_SALE;
        return new Product(null, sellerId, category, name.trim(), price, stockQuantity, status);
    }

    public void update(Long sellerId, Category category, String name, BigDecimal price, Integer stockQuantity, SaleStatus saleStatus) {
        validateOwner(sellerId);
        if (category != null) {
            validateCategory(category);
            this.category = category;
        }
        if (name != null) {
            validateName(name);
            this.name = name.trim();
        }
        if (price != null) {
            validatePrice(price);
            this.price = price;
        }
        if (stockQuantity != null) {
            validateStockQuantity(stockQuantity);
            this.stockQuantity = stockQuantity;
        }
        if (saleStatus != null) {
            validateSaleStatus(saleStatus, this.stockQuantity);
            this.saleStatus = saleStatus;
        } else if (stockQuantity != null && this.saleStatus != SaleStatus.SUSPENDED) {
            this.saleStatus = stockQuantity == 0 ? SaleStatus.SOLD_OUT : SaleStatus.ON_SALE;
        }
    }

    private void validateOwner(Long sellerId) {
        if (!this.sellerId.equals(sellerId)) {
            throw new ProductException(ProductErrorCode.PRODUCT_OWNER_MISMATCH);
        }
    }

    private static void validateSellerId(Long sellerId) {
        if (sellerId == null || sellerId <= 0) {
            throw new ProductException(ProductErrorCode.INVALID_SELLER);
        }
    }

    private static void validateCategory(Category category) {
        if (category == null || !category.isLeaf()) {
            throw new ProductException(ProductErrorCode.INVALID_CATEGORY);
        }
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isBlank() || name.trim().length() > 100) {
            throw new ProductException(ProductErrorCode.INVALID_PRODUCT_NAME);
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ProductException(ProductErrorCode.INVALID_PRICE);
        }
    }

    private static void validateStockQuantity(int stockQuantity) {
        if (stockQuantity < 0) {
            throw new ProductException(ProductErrorCode.INVALID_STOCK_QUANTITY);
        }
    }

    private static void validateSaleStatus(SaleStatus saleStatus, int stockQuantity) {
        if (saleStatus == SaleStatus.ON_SALE && stockQuantity == 0) {
            throw new ProductException(ProductErrorCode.INVALID_SALE_STATUS);
        }
    }
}
