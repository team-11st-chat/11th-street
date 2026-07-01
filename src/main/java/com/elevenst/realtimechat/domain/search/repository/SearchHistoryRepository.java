package com.elevenst.realtimechat.domain.search.repository;

import com.elevenst.realtimechat.domain.search.entity.SearchHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    @Query(
            value = """
                    select
                        keyword as keyword,
                        count(distinct case
                            when member_id is not null then concat('M:', member_id)
                            when guest_uuid is not null then concat('G:', guest_uuid)
                            else concat('H:', id)
                        end) as searchCount
                    from search_history
                    where created_at >= :from
                    group by keyword
                    order by searchCount desc, max(created_at) desc, keyword asc
                    """,
            nativeQuery = true
    )
    List<PopularKeywordRow> findPopularKeywords(@Param("from") LocalDateTime from, Pageable pageable);

    interface PopularKeywordRow {

        String getKeyword();

        long getSearchCount();
    }
}
