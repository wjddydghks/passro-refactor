package com.passro.passrobackend.market.service;

import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.market.dto.MarketCreateRequestDto;
import com.passro.passrobackend.market.dto.MarketItemResponseDto;
import com.passro.passrobackend.market.dto.MarketPurchaseResponseDto;
import com.passro.passrobackend.market.entity.Market;
import com.passro.passrobackend.market.enums.MarketCategory;
import com.passro.passrobackend.market.exception.MarketException;
import com.passro.passrobackend.market.exception.code.MarketErrorCode;
import com.passro.passrobackend.market.repository.MarketRepository;
import com.passro.passrobackend.point.service.PointService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketService {

    private static final String MARKET_IMAGE_DIRECTORY = "market-images/";

    private final MarketRepository marketRepository;
    private final PointService pointService;
    private final S3Service s3Service;

    public List<MarketItemResponseDto> getItems(String categoryValue) {
        List<Market> markets = findMarkets(categoryValue);
        return markets.stream()
                .map(MarketItemResponseDto::from)
                .toList();
    }

    private List<Market> findMarkets(String categoryValue) {
        if (categoryValue == null || categoryValue.isBlank()) {
            return marketRepository.findAllByOrderByIdAsc();
        }

        MarketCategory category = parseCategory(categoryValue);

        return category == MarketCategory.ETC
                ? marketRepository.findAllByCategoryOrCategoryIsNullOrderByIdAsc(category)
                : marketRepository.findAllByCategoryOrderByIdAsc(category);
    }

    @Transactional
    public MarketItemResponseDto createItem(MarketCreateRequestDto request) {
        MarketCategory category = parseCategory(request.getCategory());
        String imageKey = s3Service.finalizeUploadedImage(
                request.getImageKey().trim(), MARKET_IMAGE_DIRECTORY);

        Market market = marketRepository.save(Market.builder()
                .name(request.getName().trim())
                .price(request.getPrice())
                .category(category)
                .imageKey(imageKey)
                .build());
        return MarketItemResponseDto.from(market);
    }

    private MarketCategory parseCategory(String categoryValue) {
        if (categoryValue == null || categoryValue.isBlank()) {
            throw new MarketException(MarketErrorCode.INVALID_CATEGORY);
        }
        try {
            return MarketCategory.from(categoryValue.trim());
        } catch (IllegalArgumentException exception) {
            throw new MarketException(MarketErrorCode.INVALID_CATEGORY);
        }
    }

    @Transactional
    public MarketPurchaseResponseDto purchase(Long accountId, Long marketId) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new MarketException(MarketErrorCode.MARKET_NOT_FOUND));

        long remainingPoint = pointService.payForMarket(accountId, market);
        long beforePoint = Math.addExact(remainingPoint, market.getPrice());
        return MarketPurchaseResponseDto.of(market, beforePoint, remainingPoint);
    }
}
