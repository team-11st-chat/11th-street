package com.elevenst.realtimechat.domain.product.support;

import org.springframework.stereotype.Component;

@Component
public class FakeSellerStub {

    public Long getSellerId() {
        return 1L;
    }
}
