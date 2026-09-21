package com.passro.passrobackend.deliveryinquiry.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.exception.DeliveryException;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.deliveryinquiry.dto.DeliveryInquiryCreateRequestDto;
import com.passro.passrobackend.deliveryinquiry.dto.DeliveryInquiryResponseDto;
import com.passro.passrobackend.deliveryinquiry.entity.DeliveryInquiry;
import com.passro.passrobackend.deliveryinquiry.enums.DeliveryInquiryCategory;
import com.passro.passrobackend.deliveryinquiry.repository.DeliveryInquiryRepository;
import com.passro.passrobackend.file.service.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeliveryInquiryServiceTest {

    @Mock
    private DeliveryInquiryRepository deliveryInquiryRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private DeliveryInquiryService deliveryInquiryService;

    private Account account(Long id) {
        return Account.builder().id(id).nickname("tester").build();
    }

    private Delivery delivery(Long id) {
        return Delivery.builder().id(id).build();
    }

    @Test
    @DisplayName("배송 문의 작성 성공 - 이미지 없음")
    void createDeliveryInquiry_success_withoutImage() {
        // given
        Account account = account(10L);
        DeliveryInquiryCreateRequestDto request = DeliveryInquiryCreateRequestDto.builder()
                .deliveryId(1L)
                .category(DeliveryInquiryCategory.DELAY)
                .title("제목")
                .content("내용")
                .build();
        given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery(1L)));
        given(deliveryInquiryRepository.save(any(DeliveryInquiry.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        DeliveryInquiryResponseDto response = deliveryInquiryService.createDeliveryInquiry(account, request);

        // then
        assertThat(response.getDeliveryId()).isEqualTo(1L);
        assertThat(response.getCategory()).isEqualTo(DeliveryInquiryCategory.DELAY);
        assertThat(response.getImageKey()).isNull();
        assertThat(response.getImageUrl()).isNull();
        verify(s3Service, never()).getPresignedDownloadUrl(any());
    }

    @Test
    @DisplayName("배송 문의 작성 성공 - 이미지 첨부")
    void createDeliveryInquiry_success_withImage() throws Exception {
        // given
        Account account = account(10L);
        String imageKey = "inquiry/2026/08/uuid-5678.png";
        DeliveryInquiryCreateRequestDto request = DeliveryInquiryCreateRequestDto.builder()
                .deliveryId(1L)
                .category(DeliveryInquiryCategory.DAMAGE)
                .title("파손")
                .content("사진 첨부합니다.")
                .imageKey(imageKey)
                .build();
        URL presigned = new URL("https://s3.example.com/" + imageKey + "?sig=xyz");
        given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery(1L)));
        given(deliveryInquiryRepository.save(any(DeliveryInquiry.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(s3Service.getPresignedDownloadUrl(eq(imageKey))).willReturn(presigned);

        // when
        DeliveryInquiryResponseDto response = deliveryInquiryService.createDeliveryInquiry(account, request);

        // then
        assertThat(response.getImageKey()).isEqualTo(imageKey);
        assertThat(response.getImageUrl()).isEqualTo(presigned.toString());
        verify(s3Service).getPresignedDownloadUrl(imageKey);
    }

    @Test
    @DisplayName("배송 문의 작성 실패 - 존재하지 않는 배송")
    void createDeliveryInquiry_deliveryNotFound() {
        // given
        DeliveryInquiryCreateRequestDto request = DeliveryInquiryCreateRequestDto.builder()
                .deliveryId(999L)
                .category(DeliveryInquiryCategory.ETC)
                .content("내용")
                .build();
        given(deliveryRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deliveryInquiryService.createDeliveryInquiry(account(10L), request))
                .isInstanceOf(DeliveryException.class);
        verify(deliveryInquiryRepository, never()).save(any());
        verify(s3Service, never()).getPresignedDownloadUrl(any());
    }

    @Test
    @DisplayName("배송 문의 조회 성공 - 이미지 있는 것과 없는 것 혼합")
    void getDeliveryInquiries_success_mixedImages() throws Exception {
        // given
        Delivery delivery = delivery(1L);
        DeliveryInquiry withImage = DeliveryInquiry.builder()
                .id(100L)
                .delivery(delivery)
                .account(account(10L))
                .category(DeliveryInquiryCategory.DAMAGE)
                .content("파손")
                .imageKey("inquiry/2026/08/a.png")
                .build();
        DeliveryInquiry withoutImage = DeliveryInquiry.builder()
                .id(101L)
                .delivery(delivery)
                .account(account(10L))
                .category(DeliveryInquiryCategory.DELAY)
                .content("지연")
                .build();
        URL presigned = new URL("https://s3.example.com/inquiry/2026/08/a.png?sig=1");
        given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery));
        given(deliveryInquiryRepository.findAllByDeliveryOrderByCreatedAtDesc(delivery))
                .willReturn(List.of(withImage, withoutImage));
        given(s3Service.getPresignedDownloadUrl(eq("inquiry/2026/08/a.png"))).willReturn(presigned);

        // when
        List<DeliveryInquiryResponseDto> result = deliveryInquiryService.getDeliveryInquiriesByDelivery(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getImageKey()).isEqualTo("inquiry/2026/08/a.png");
        assertThat(result.get(0).getImageUrl()).isEqualTo(presigned.toString());
        assertThat(result.get(1).getImageKey()).isNull();
        assertThat(result.get(1).getImageUrl()).isNull();
    }

    @Test
    @DisplayName("배송 문의 조회 성공 - 문의가 없으면 빈 목록")
    void getDeliveryInquiries_empty() {
        // given
        Delivery delivery = delivery(1L);
        given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery));
        given(deliveryInquiryRepository.findAllByDeliveryOrderByCreatedAtDesc(delivery))
                .willReturn(List.of());

        // when
        List<DeliveryInquiryResponseDto> result = deliveryInquiryService.getDeliveryInquiriesByDelivery(1L);

        // then
        assertThat(result).isEmpty();
        verify(s3Service, never()).getPresignedDownloadUrl(any());
    }

    @Test
    @DisplayName("배송 문의 조회 실패 - 존재하지 않는 배송")
    void getDeliveryInquiries_deliveryNotFound() {
        // given
        given(deliveryRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deliveryInquiryService.getDeliveryInquiriesByDelivery(999L))
                .isInstanceOf(DeliveryException.class);
    }
}
