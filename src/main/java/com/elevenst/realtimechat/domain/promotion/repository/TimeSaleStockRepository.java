package com.elevenst.realtimechat.domain.promotion.repository;

import com.elevenst.realtimechat.domain.promotion.entity.TimeSaleStock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeSaleStockRepository extends JpaRepository<TimeSaleStock, Long> {
    Optional<TimeSaleStock> findByTimeSaleId(Long timeSaleId);
}
