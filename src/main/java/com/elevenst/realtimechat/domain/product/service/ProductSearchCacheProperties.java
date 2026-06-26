package com.elevenst.realtimechat.domain.product.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache.product-search")
public record ProductSearchCacheProperties(Mode mode) {

    public ProductSearchCacheProperties {
        mode = mode == null ? Mode.LOCAL : mode;
    }

    public enum Mode {
        LOCAL,
        REMOTE
    }
}
