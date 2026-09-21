package com.passro.passrobackend.inquiry.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.inquiry.dto.InquiryCreateRequestDto;
import com.passro.passrobackend.inquiry.dto.InquiryResponseDto;
import com.passro.passrobackend.inquiry.entity.Inquiry;
import com.passro.passrobackend.inquiry.enums.InquiryCategory;
import com.passro.passrobackend.inquiry.repository.InquiryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private InquiryService inquiryService;

    private Account account(Long id) {
        return Account.builder().id(id).nickname("tester").build();
    }

    @Test
    @DisplayName("공통 문의 작성 성공 - 이미지 없음")
    void createInquiry_success_withoutImage() {
        // given
        Account account = account(10L);
        InquiryCreateRequestDto request = InquiryCreateRequestDto.builder()
                .category(InquiryCategory.ACCOUNT)
                .title("로그인이 안돼요")
                .content("비밀번호 재설정 이메일이 오지 않습니다.")
                .build();
        given(inquiryRepository.save(any(Inquiry.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        InquiryResponseDto response = inquiryService.createInquiry(account, request);

        // then
        assertThat(response.getCategory()).isEqualTo(InquiryCategory.ACCOUNT);
        assertThat(response.getTitle()).isEqualTo("로그인이 안돼요");
        assertThat(response.getImageKey()).isNull();
        assertThat(response.getImageUrl()).isNull();
        assertThat(response.getWriterNickname()).isEqualTo("tester");
        verify(s3Service, never()).getPresignedDownloadUrl(any());
    }

    @Test
    @DisplayName("공통 문의 작성 성공 - 이미지 첨부")
    void createInquiry_success_withImage() throws Exception {
        // given
        Account account = account(10L);
        String imageKey = "inquiry/2026/08/uuid-1234.png";
        InquiryCreateRequestDto request = InquiryCreateRequestDto.builder()
                .category(InquiryCategory.BUG)
                .title("오류 발견")
                .content("스크린샷 첨부합니다.")
                .imageKey(imageKey)
                .build();
        URL presigned = new URL("https://s3.example.com/" + imageKey + "?sig=abc");
        given(inquiryRepository.save(any(Inquiry.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(s3Service.getPresignedDownloadUrl(eq(imageKey))).willReturn(presigned);

        // when
        InquiryResponseDto response = inquiryService.createInquiry(account, request);

        // then
        assertThat(response.getImageKey()).isEqualTo(imageKey);
        assertThat(response.getImageUrl()).isEqualTo(presigned.toString());
        verify(s3Service).getPresignedDownloadUrl(imageKey);
    }

    @Test
    @DisplayName("공통 문의 작성 성공 - imageKey 빈 문자열은 null 로 저장")
    void createInquiry_blankImageKeyBecomesNull() {
        // given
        Account account = account(10L);
        InquiryCreateRequestDto request = InquiryCreateRequestDto.builder()
                .category(InquiryCategory.ETC)
                .title("문의")
                .content("내용")
                .imageKey("   ")
                .build();
        given(inquiryRepository.save(any(Inquiry.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        InquiryResponseDto response = inquiryService.createInquiry(account, request);

        // then
        assertThat(response.getImageKey()).isNull();
        assertThat(response.getImageUrl()).isNull();
        verify(s3Service, never()).getPresignedDownloadUrl(any());
    }
}
