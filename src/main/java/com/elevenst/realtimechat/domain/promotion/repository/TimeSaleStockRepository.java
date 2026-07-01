package com.elevenst.realtimechat.domain.promotion.repository;

import com.elevenst.realtimechat.domain.promotion.entity.TimeSaleStock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeSaleStockRepository extends JpaRepository<TimeSaleStock, Long> {
    Optional<TimeSaleStock> findByTimeSaleId(Long timeSaleId);

    List<TimeSaleStock> findByTimeSaleIdIn(Collection<Long> timeSaleIds);
}
