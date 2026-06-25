package com.elevenst.realtimechat.domain.product.entity;

import com.elevenst.realtimechat.domain.product.exception.ProductErrorCode;
import com.elevenst.realtimechat.domain.product.exception.ProductException;
import com.elevenst.realtimechat.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int depth;

    @Column(nullable = false)
    private int sortOrder;

    public static Category createRoot(String name, int sortOrder) {
        validateName(name);
        validateSortOrder(sortOrder);
        return new Category(null, null, name.trim(), 1, sortOrder);
    }

    public static Category createChild(Category parent, String name, int sortOrder) {
        if (parent == null || parent.getDepth() != 1) {
            throw new ProductException(ProductErrorCode.INVALID_CATEGORY);
        }
        validateName(name);
        validateSortOrder(sortOrder);
        return new Category(null, parent, name.trim(), 2, sortOrder);
    }

    public boolean isLeaf() {
        return depth == 2;
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isBlank() || name.trim().length() > 50) {
            throw new ProductException(ProductErrorCode.INVALID_CATEGORY);
        }
    }

    private static void validateSortOrder(int sortOrder) {
        if (sortOrder < 0) {
            throw new ProductException(ProductErrorCode.INVALID_CATEGORY);
        }
    }
}
