package com.passro.passrobackend.review.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.repository.AccountRepository;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.review.dto.ReviewAverageResponseDto;
import com.passro.passrobackend.review.dto.ReviewCreateRequestDto;
import com.passro.passrobackend.review.entity.Review;
import com.passro.passrobackend.review.exception.ReviewException;
import com.passro.passrobackend.review.exception.code.ReviewErrorCode;
import com.passro.passrobackend.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final DeliveryRepository deliveryRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public void createReview(Account account, ReviewCreateRequestDto request) {
        if (request.getDeliveryId() == null) {
            throw new ReviewException(ReviewErrorCode.INVALID_REVIEW_DELIVERY_ID);
        }

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new ReviewException(ReviewErrorCode.INVALID_REVIEW_RATING);
        }

        Delivery delivery = deliveryRepository.findById(request.getDeliveryId())
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.REVIEW_DELIVERY_NOT_FOUND));

        if (!delivery.getSender().getId().equals(account.getId())) {
            throw new ReviewException(ReviewErrorCode.REVIEW_FORBIDDEN);
        }

        if (delivery.getStatus() != DeliveryState.DELIVERED) {
            throw new ReviewException(ReviewErrorCode.REVIEW_DELIVERY_NOT_COMPLETED);
        }

        if (reviewRepository.existsByDeliveryId(delivery.getId())) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.builder()
                .delivery(delivery)
                .rating(request.getRating())
                .content(request.getContent())
                .build();

        try {
            reviewRepository.save(review);
        } catch (DataIntegrityViolationException e) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }
    }

    public ReviewAverageResponseDto getAverageRating(Long userId) {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.REVIEW_ACCOUNT_NOT_FOUND));

        Double averageRating = reviewRepository.findAverageRatingByShipperId(account.getId());

        if (averageRating == null) {
            averageRating = 0.0;
        }

        return new ReviewAverageResponseDto(averageRating);
    }
}
