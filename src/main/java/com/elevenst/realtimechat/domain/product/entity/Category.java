package com.elevenst.realtimechat.domain.product.entity;

import com.elevenst.realtimechat.domain.product.exception.ProductErrorCode;
import com.elevenst.realtimechat.domain.product.exception.ProductException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

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

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static Category createRoot(String name, int sortOrder) {
        validateName(name);
        validateSortOrder(sortOrder);
        LocalDateTime now = LocalDateTime.now();
        return new Category(null, null, name.trim(), 1, sortOrder, now, now);
    }

    public static Category createChild(Category parent, String name, int sortOrder) {
        if (parent == null || parent.getDepth() != 1) {
            throw new ProductException(ProductErrorCode.INVALID_CATEGORY);
        }
        validateName(name);
        validateSortOrder(sortOrder);
        LocalDateTime now = LocalDateTime.now();
        return new Category(null, parent, name.trim(), 2, sortOrder, now, now);
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
