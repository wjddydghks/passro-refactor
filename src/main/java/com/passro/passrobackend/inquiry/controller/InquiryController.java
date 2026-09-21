package com.passro.passrobackend.inquiry.controller;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.global.response.APIResponse;
import com.passro.passrobackend.inquiry.code.InquirySuccessCode;
import com.passro.passrobackend.inquiry.dto.InquiryCreateRequestDto;
import com.passro.passrobackend.inquiry.dto.InquiryResponseDto;
import com.passro.passrobackend.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.passro.passrobackend.global.configuration.SwaggerErrorExamples.COMMON_VALIDATION;

@RestController
@RequestMapping("/inquiry")
@RequiredArgsConstructor
@Tag(name = "문의(공통)", description = "배송과 무관한 공통 문의 API")
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "공통 문의 작성", description = "배송과 무관한 공통 문의를 작성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "문의 작성 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "COMMON400", summary = "요청 값 검증 실패", value = COMMON_VALIDATION)))
    })
    public APIResponse<InquiryResponseDto> createInquiry(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Valid @RequestBody InquiryCreateRequestDto request) {
        return APIResponse.onSuccess(InquirySuccessCode.CREATED, inquiryService.createInquiry(account, request));
    }
}
