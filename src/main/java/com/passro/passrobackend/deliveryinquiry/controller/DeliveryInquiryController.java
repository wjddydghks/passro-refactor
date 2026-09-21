package com.passro.passrobackend.deliveryinquiry.controller;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.deliveryinquiry.code.DeliveryInquirySuccessCode;
import com.passro.passrobackend.deliveryinquiry.dto.DeliveryInquiryCreateRequestDto;
import com.passro.passrobackend.deliveryinquiry.dto.DeliveryInquiryResponseDto;
import com.passro.passrobackend.deliveryinquiry.service.DeliveryInquiryService;
import com.passro.passrobackend.global.response.APIResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.passro.passrobackend.global.configuration.SwaggerErrorExamples.COMMON_VALIDATION;
import static com.passro.passrobackend.global.configuration.SwaggerErrorExamples.DELIVERY_NOT_FOUND;
import static com.passro.passrobackend.global.configuration.SwaggerSuccessExamples.INQUIRY_CREATED;
import static com.passro.passrobackend.global.configuration.SwaggerSuccessExamples.INQUIRY_LIST;

@RestController
@RequestMapping("/delivery-inquiry")
@RequiredArgsConstructor
@Tag(name = "배송 문의", description = "특정 배송 건에 대한 문의 작성 및 조회 API")
public class DeliveryInquiryController {

    private final DeliveryInquiryService deliveryInquiryService;

    // 배송 문의 작성
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "배송 문의 작성", description = "특정 배송에 대한 문의를 작성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "문의 작성 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "DELIVERY_INQUIRY201_1", summary = "배송 문의 작성 성공", value = INQUIRY_CREATED))),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "COMMON400", summary = "요청 값 검증 실패", value = COMMON_VALIDATION))),
            @ApiResponse(responseCode = "404", description = "배송 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY404_1", summary = "배송 정보 없음", value = DELIVERY_NOT_FOUND)))
    })
    public APIResponse<DeliveryInquiryResponseDto> createDeliveryInquiry(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Valid @RequestBody DeliveryInquiryCreateRequestDto request) {
        return APIResponse.onSuccess(DeliveryInquirySuccessCode.CREATED,
                deliveryInquiryService.createDeliveryInquiry(account, request));
    }

    // 배송별 문의 목록 조회
    @GetMapping("/{deliveryId}")
    @Operation(summary = "배송 문의 목록 조회", description = "특정 배송의 문의 목록을 최신순으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "DELIVERY_INQUIRY200_1", summary = "배송 문의 목록 조회 성공", value = INQUIRY_LIST))),
            @ApiResponse(responseCode = "404", description = "배송 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY404_1", summary = "배송 정보 없음", value = DELIVERY_NOT_FOUND)))
    })
    public APIResponse<List<DeliveryInquiryResponseDto>> getDeliveryInquiriesByDelivery(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "배송 ID", example = "1") @PathVariable Long deliveryId) {
        return APIResponse.onSuccess(DeliveryInquirySuccessCode.OK,
                deliveryInquiryService.getDeliveryInquiriesByDelivery(deliveryId));
    }
}
