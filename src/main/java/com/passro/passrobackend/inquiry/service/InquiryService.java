package com.passro.passrobackend.inquiry.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.inquiry.dto.InquiryCreateRequestDto;
import com.passro.passrobackend.inquiry.dto.InquiryResponseDto;
import com.passro.passrobackend.inquiry.entity.Inquiry;
import com.passro.passrobackend.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final S3Service s3Service;

    @Transactional
    public InquiryResponseDto createInquiry(Account account, InquiryCreateRequestDto request) {
        String imageKey = normalizeImageKey(request.getImageKey());

        if (imageKey != null) {
            s3Service.validateUploadedImage(imageKey);
        }

        Inquiry inquiry = Inquiry.builder()
                .account(account)
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .imageKey(imageKey)
                .build();

        Inquiry saved = inquiryRepository.save(inquiry);
        return InquiryResponseDto.fromInquiry(saved, resolveImageUrl(imageKey));
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
