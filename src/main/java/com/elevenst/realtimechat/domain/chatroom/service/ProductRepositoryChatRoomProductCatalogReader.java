package com.elevenst.realtimechat.domain.chatroom.service;

import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomErrorCode;
import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomException;
import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductRepositoryChatRoomProductCatalogReader implements ChatRoomProductCatalogReader {

    private final ProductRepository productRepository;

    @Override
    public ProductSellerSnapshot getProductSeller(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorCode.PRODUCT_NOT_FOUND));
        return new ProductSellerSnapshot(product.getId(), product.getSellerId());
    }
}
