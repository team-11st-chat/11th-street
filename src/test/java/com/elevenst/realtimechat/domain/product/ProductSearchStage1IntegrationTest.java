package com.elevenst.realtimechat.domain.product;

import static com.elevenst.realtimechat.global.config.CacheConfig.PRODUCT_SEARCH_CACHE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.elevenst.realtimechat.domain.product.entity.Category;
import com.elevenst.realtimechat.domain.product.repository.CategoryRepository;
import com.elevenst.realtimechat.domain.product.repository.ProductRepository;
import com.elevenst.realtimechat.domain.search.repository.SearchHistoryRepository;
import com.elevenst.realtimechat.domain.search.service.SearchKeywordRecordCommand;
import com.elevenst.realtimechat.domain.search.service.SearchKeywordRecorder;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class ProductSearchStage1IntegrationTest {

    private static final String GUEST_ID_HEADER = "Request-Guest-ID";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private SearchKeywordRecorder searchKeywordRecorder;

    @Autowired
    private CacheManager cacheManager;

    private Category category;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        clearProductSearchCache();
        searchHistoryRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category root = categoryRepository.save(Category.createRoot("Digital", 1));
        category = categoryRepository.save(Category.createChild(root, "Audio", 1));
    }

    @AfterEach
    void tearDown() {
        clearProductSearchCache();
        searchHistoryRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void productRegistrationDetailListSearchAndPopularKeywordFlow() throws Exception {
        Long productId = createProduct("Apple AirPods Pro", 329000, 15);

        mockMvc.perform(get("/api/v1/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.name").value("Apple AirPods Pro"))
                .andExpect(jsonPath("$.data.saleStatus").value("ON_SALE"));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(productId));

        mockMvc.perform(get("/api/v1/products")
                        .param("keyword", "  AIRPODS  ")
                        .header(GUEST_ID_HEADER, "guest-flow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(productId));

        mockMvc.perform(get("/api/v1/products")
                        .param("keyword", "no-result-keyword")
                        .header(GUEST_ID_HEADER, "guest-flow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        List<KeywordCount> popularKeywords = getPopularKeywordData();

        assertThat(popularKeywords)
                .contains(
                        new KeywordCount("airpods", 1),
                        new KeywordCount("no-result-keyword", 1)
                );
    }

    @Test
    void searchProductsSortsSoldOutAtBottomAndExcludesSuspended() throws Exception {
        Long onSaleOld = createProduct("Stage Keyboard A", 100000, 10);
        Long soldOut = createProduct("Stage Keyboard B", 120000, 0);
        Long onSaleNew = createProduct("Stage Keyboard C", 130000, 3);
        Long suspended = createProduct("Stage Keyboard D", 140000, 5);
        suspendProduct(suspended);

        List<Long> productIds = getProductSearchIds("keyboard", "guest-sort");

        assertThat(productIds).containsExactly(onSaleNew, onSaleOld, soldOut);
        assertThat(productIds).doesNotContain(suspended);
    }

    @Test
    void productSearchV2_returnsSameResponseContractAsV1() throws Exception {
        createProduct("Apple AirPods Pro", 329000, 15);
        createProduct("Apple AirPods Max", 769000, 4);

        Map<String, Object> v1Response = getProductSearchResponse("/api/v1/products", "airpods", "guest-v1");
        Map<String, Object> v2Response = getProductSearchResponse("/api/v2/products", "airpods", "guest-v2");

        assertThat(v2Response).isEqualTo(v1Response);
    }

    @Test
    void productSearchV2_evictsCachedSearchWhenProductChanges() throws Exception {
        createProduct("Galaxy Buds Pro", 199000, 10);

        Map<String, Object> cachedData = getProductSearchData("/api/v2/products", "galaxy", "guest-cache");
        assertThat(cachedData.get("totalElements")).isEqualTo(1);

        createProduct("Galaxy Buds Live", 149000, 7);

        Map<String, Object> refreshedData = getProductSearchData("/api/v2/products", "galaxy", "guest-cache");
        assertThat(refreshedData.get("totalElements")).isEqualTo(2);
    }

    @Test
    void popularKeywordsCountsDistinctMemberAndGuestSearchHistory() throws Exception {
        searchKeywordRecorder.record(SearchKeywordRecordCommand.member("Keyboard", 1L, category.getId()));
        searchKeywordRecorder.record(SearchKeywordRecordCommand.member("keyboard", 1L, category.getId()));
        searchKeywordRecorder.record(SearchKeywordRecordCommand.member("keyboard", 2L, category.getId()));
        searchKeywordRecorder.record(SearchKeywordRecordCommand.guest("keyboard", "guest-a", category.getId()));
        searchKeywordRecorder.record(SearchKeywordRecordCommand.guest("keyboard", "guest-a", category.getId()));
        searchKeywordRecorder.record(SearchKeywordRecordCommand.guest("keyboard", "guest-b", category.getId()));
        searchKeywordRecorder.record(SearchKeywordRecordCommand.guest("keyboard", null, category.getId()));
        searchKeywordRecorder.record(SearchKeywordRecordCommand.guest("keyboard", null, category.getId()));

        List<KeywordCount> popularKeywords = getPopularKeywordData();

        assertThat(popularKeywords)
                .contains(new KeywordCount("keyboard", 6));
    }

    private Long createProduct(String name, int price, int stockQuantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "categoryId": %d,
                                  "price": %d,
                                  "stockQuantity": %d
                                }
                                """.formatted(name, category.getId(), price, stockQuantity)))
                .andExpect(status().isCreated())
                .andReturn();

        Number productId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return productId.longValue();
    }

    private void suspendProduct(Long productId) throws Exception {
        mockMvc.perform(patch("/api/v1/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "saleStatus": "SUSPENDED"
                                }
                                """))
                .andExpect(status().isOk());
    }

    private List<Long> getProductSearchIds(String keyword, String guestId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/products")
                        .param("keyword", keyword)
                        .header(GUEST_ID_HEADER, guestId))
                .andExpect(status().isOk())
                .andReturn();

        List<Number> productIds = JsonPath.read(result.getResponse().getContentAsString(), "$.data.content[*].id");
        return productIds.stream()
                .map(Number::longValue)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getProductSearchData(String path, String keyword, String guestId) throws Exception {
        return (Map<String, Object>) getProductSearchResponse(path, keyword, guestId).get("data");
    }

    private Map<String, Object> getProductSearchResponse(String path, String keyword, String guestId) throws Exception {
        MvcResult result = mockMvc.perform(get(path)
                        .param("keyword", keyword)
                        .header(GUEST_ID_HEADER, guestId))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$");
    }

    private List<KeywordCount> getPopularKeywordData() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/popular-keywords"))
                .andExpect(status().isOk())
                .andReturn();

        List<KeywordCount> keywordCounts = new ArrayList<>();
        List<Map<String, Object>> keywords = JsonPath.read(result.getResponse().getContentAsString(), "$.data[*]");
        keywords.forEach(keyword -> keywordCounts.add(new KeywordCount(
                (String) keyword.get("keyword"),
                ((Number) keyword.get("searchCount")).longValue()
        )));
        return keywordCounts;
    }

    private void clearProductSearchCache() {
        Cache cache = cacheManager.getCache(PRODUCT_SEARCH_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    private record KeywordCount(String keyword, long searchCount) {
    }
}
