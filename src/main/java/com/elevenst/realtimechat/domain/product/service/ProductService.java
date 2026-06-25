package com.elevenst.realtimechat.domain.product.service;

import com.elevenst.realtimechat.domain.product.dto.ProductCreateRequest;
import com.elevenst.realtimechat.domain.product.dto.ProductPageResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductSummaryResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductUpdateRequest;
import com.elevenst.realtimechat.domain.product.entity.Category;
import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.entity.SaleStatus;
import com.elevenst.realtimechat.domain.product.exception.ProductErrorCode;
import com.elevenst.realtimechat.domain.product.exception.ProductException;
import com.elevenst.realtimechat.domain.product.repository.CategoryRepository;
import com.elevenst.realtimechat.domain.product.repository.ProductRepository;
import com.elevenst.realtimechat.domain.search.service.SearchKeywordRecordCommand;
import com.elevenst.realtimechat.domain.search.service.SearchKeywordRecorder;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SearchKeywordRecorder searchKeywordRecorder;

    @Transactional
    public ProductResponse createProduct(Long sellerId, ProductCreateRequest request) {
        Category category = getCategory(request.categoryId());
        Product product = Product.create(sellerId, category, request.name(), request.price(), request.stockQuantity());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long sellerId, Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));
        Category category = request.categoryId() == null ? null : getCategory(request.categoryId());

        product.update(
                sellerId,
                category,
                request.name(),
                request.price(),
                request.stockQuantity(),
                request.saleStatus()
        );
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        return ProductResponse.from(getProductEntity(productId));
    }

    @Transactional
    public ProductPageResponse searchProducts(String keyword, Long categoryId, int page, int size, String guestId) {
        validatePageRequest(page, size);
        String normalizedKeyword = normalizeKeyword(keyword);
        recordSearchKeyword(normalizedKeyword, categoryId, guestId);

        return ProductPageResponse.from(productRepository
                .searchProducts(normalizedKeyword, categoryId, SaleStatus.SUSPENDED, SaleStatus.SOLD_OUT, PageRequest.of(page, size))
                .map(ProductSummaryResponse::from));
    }

    private Product getProductEntity(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND));
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0 || size < 1) {
            throw new ProductException(ProductErrorCode.INVALID_PAGE_REQUEST);
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private void recordSearchKeyword(String keyword, Long categoryId, String guestId) {
        if (keyword != null) {
            searchKeywordRecorder.record(SearchKeywordRecordCommand.guest(keyword, guestId, categoryId));
        }
    }
}
