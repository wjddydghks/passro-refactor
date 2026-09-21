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
import com.passro.passrobackend.review.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private ReviewService reviewService;

    private Account account(Long id) {
        return Account.builder()
                .id(id)
                .nickname("tester" + id)
                .build();
    }

    private Delivery delivery(Long id, Account sender, Account shipper, DeliveryState status) {
        return Delivery.builder()
                .id(id)
                .sender(sender)
                .shipper(shipper)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("리뷰 작성 성공")
    void createReview_success() {
        // given
        Account sender = account(1L);
        Account shipper = account(2L);

        ReviewCreateRequestDto request = new ReviewCreateRequestDto(100L, 5, "좋아요");
        Delivery delivery = delivery(100L, sender, shipper, DeliveryState.DELIVERED);

        given(deliveryRepository.findById(100L)).willReturn(Optional.of(delivery));
        given(reviewRepository.existsByDeliveryId(100L)).willReturn(false);
        given(reviewRepository.save(any(Review.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        reviewService.createReview(sender, request);

        // then
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 평점이 null")
    void createReview_ratingNull() {
        // given
        Account sender = account(1L);
        ReviewCreateRequestDto request = new ReviewCreateRequestDto(100L, null, "좋아요");

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(sender, request))
                .isInstanceOf(ReviewException.class);

        verify(deliveryRepository, never()).findById(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 평점이 1점 미만")
    void createReview_ratingTooLow() {
        // given
        Account sender = account(1L);
        ReviewCreateRequestDto request = new ReviewCreateRequestDto(100L, 0, "좋아요");

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(sender, request))
                .isInstanceOf(ReviewException.class);

        verify(deliveryRepository, never()).findById(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 평점이 5점 초과")
    void createReview_ratingTooHigh() {
        // given
        Account sender = account(1L);
        ReviewCreateRequestDto request = new ReviewCreateRequestDto(100L, 6, "좋아요");

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(sender, request))
                .isInstanceOf(ReviewException.class);

        verify(deliveryRepository, never()).findById(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 배송이 존재하지 않음")
    void createReview_deliveryNotFound() {
        // given
        Account sender = account(1L);
        ReviewCreateRequestDto request = new ReviewCreateRequestDto(100L, 5, "좋아요");

        given(deliveryRepository.findById(100L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(sender, request))
                .isInstanceOf(ReviewException.class);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 발송자가 아님")
    void createReview_forbidden() {
        // given
        Account loginUser = account(1L);
        Account realSender = account(3L);
        Account shipper = account(2L);

        ReviewCreateRequestDto request = new ReviewCreateRequestDto(100L, 5, "좋아요");
        Delivery delivery = delivery(100L, realSender, shipper, DeliveryState.DELIVERED);

        given(deliveryRepository.findById(100L)).willReturn(Optional.of(delivery));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(loginUser, request))
                .isInstanceOf(ReviewException.class);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 배송 완료 상태가 아님")
    void createReview_deliveryNotCompleted() {
        // given
        Account sender = account(1L);
        Account shipper = account(2L);

        ReviewCreateRequestDto request = new ReviewCreateRequestDto(100L, 5, "좋아요");
        Delivery delivery = delivery(100L, sender, shipper, DeliveryState.MATCHED);

        given(deliveryRepository.findById(100L)).willReturn(Optional.of(delivery));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(sender, request))
                .isInstanceOf(ReviewException.class);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 이미 리뷰가 존재함")
    void createReview_alreadyExists() {
        // given
        Account sender = account(1L);
        Account shipper = account(2L);

        ReviewCreateRequestDto request = new ReviewCreateRequestDto(100L, 5, "좋아요");
        Delivery delivery = delivery(100L, sender, shipper, DeliveryState.DELIVERED);

        given(deliveryRepository.findById(100L)).willReturn(Optional.of(delivery));
        given(reviewRepository.existsByDeliveryId(100L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(sender, request))
                .isInstanceOf(ReviewException.class);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("평균 평점 조회 성공")
    void getAverageRating_success() {
        // given
        Account shipper = account(2L);

        given(accountRepository.findById(2L)).willReturn(Optional.of(shipper));
        given(reviewRepository.findAverageRatingByShipperId(2L)).willReturn(4.5);

        // when
        ReviewAverageResponseDto result = reviewService.getAverageRating(2L);

        // then
        assertThat(result.getAverageRating()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("평균 평점 조회 성공 - 리뷰가 없으면 0.0 반환")
    void getAverageRating_noReview() {
        // given
        Account shipper = account(2L);

        given(accountRepository.findById(2L)).willReturn(Optional.of(shipper));
        given(reviewRepository.findAverageRatingByShipperId(2L)).willReturn(null);

        // when
        ReviewAverageResponseDto result = reviewService.getAverageRating(2L);

        // then
        assertThat(result.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("평균 평점 조회 실패 - 사용자가 존재하지 않음")
    void getAverageRating_accountNotFound() {
        // given
        given(accountRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.getAverageRating(99L))
                .isInstanceOf(ReviewException.class);
    }
}
