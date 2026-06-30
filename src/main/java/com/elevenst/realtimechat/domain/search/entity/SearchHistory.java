package com.elevenst.realtimechat.domain.search.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private Long categoryId;

    @Column(length = 50)
    private String guestUuid;

    @Column(nullable = false)
    private String keyword;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static SearchHistory create(Long memberId, Long categoryId, String guestUuid, String keyword) {
        return new SearchHistory(
                null,
                memberId,
                categoryId,
                normalizeGuestUuid(guestUuid),
                keyword,
                null
        );
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    private static String normalizeGuestUuid(String guestUuid) {
        if (guestUuid == null || guestUuid.trim().isBlank()) {
            return null;
        }

        return guestUuid.trim();
    }
}
