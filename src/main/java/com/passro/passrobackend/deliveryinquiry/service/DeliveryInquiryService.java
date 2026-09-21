package com.passro.passrobackend.deliveryinquiry.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.exception.DeliveryException;
import com.passro.passrobackend.delivery.exception.code.DeliveryErrorCode;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.deliveryinquiry.dto.DeliveryInquiryCreateRequestDto;
import com.passro.passrobackend.deliveryinquiry.dto.DeliveryInquiryResponseDto;
import com.passro.passrobackend.deliveryinquiry.entity.DeliveryInquiry;
import com.passro.passrobackend.deliveryinquiry.repository.DeliveryInquiryRepository;
import com.passro.passrobackend.file.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryInquiryService {

    private final DeliveryInquiryRepository deliveryInquiryRepository;
    private final DeliveryRepository deliveryRepository;
    private final S3Service s3Service;

    // 배송 문의 작성
    @Transactional
    public DeliveryInquiryResponseDto createDeliveryInquiry(Account account, DeliveryInquiryCreateRequestDto request) {
        Delivery delivery = deliveryRepository.findById(request.getDeliveryId())
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.NOT_FOUND));

        String imageKey = normalizeImageKey(request.getImageKey());

        if (imageKey != null) {
            s3Service.validateUploadedImage(imageKey);
        }

        DeliveryInquiry inquiry = DeliveryInquiry.builder()
                .delivery(delivery)
                .account(account)
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .imageKey(imageKey)
                .build();

        DeliveryInquiry saved = deliveryInquiryRepository.save(inquiry);
        return DeliveryInquiryResponseDto.fromDeliveryInquiry(saved, resolveImageUrl(imageKey));
    }

    // 특정 배송에 달린 문의 목록 조회 (최신순)
    @Transactional(readOnly = true)
    public List<DeliveryInquiryResponseDto> getDeliveryInquiriesByDelivery(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.NOT_FOUND));

        return deliveryInquiryRepository.findAllByDeliveryOrderByCreatedAtDesc(delivery)
                .stream()
                .map(inquiry -> DeliveryInquiryResponseDto.fromDeliveryInquiry(
                        inquiry, resolveImageUrl(inquiry.getImageKey())))
                .toList();
    }

    // 빈 문자열은 null 로 정리
    private String normalizeImageKey(String imageKey) {
        return (imageKey == null || imageKey.isBlank()) ? null : imageKey;
    }

    // imageKey 있으면 Presigned Download URL 발급, 없으면 null
    private String resolveImageUrl(String imageKey) {
        if (imageKey == null) {
            return null;
        }
        return s3Service.getPresignedDownloadUrl(imageKey).toString();
    }
}
