package com.passro.passrobackend.domain.market.repository;

import com.passro.passrobackend.domain.market.entity.Market;
import com.passro.passrobackend.domain.market.enums.MarketCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRepository extends JpaRepository<Market, Long> {

    List<Market> findAllByOrderByIdAsc();

    List<Market> findAllByCategoryOrderByIdAsc(MarketCategory category);

    List<Market> findAllByCategoryOrCategoryIsNullOrderByIdAsc(MarketCategory category);
}
