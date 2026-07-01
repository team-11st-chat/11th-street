package com.elevenst.realtimechat.domain.message.service;

import com.elevenst.realtimechat.domain.message.exception.ChatMessageErrorCode;
import com.elevenst.realtimechat.domain.message.exception.ChatMessageException;
import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductRepositoryChatMessageProductSnapshotReader implements ChatMessageProductSnapshotReader {

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public ChatMessageProductSnapshot getSnapshot(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ChatMessageException(ChatMessageErrorCode.PRODUCT_REFERENCE_NOT_FOUND));
        return new ChatMessageProductSnapshot(product.getId(), product.getName(), product.getPrice());
    }
}
