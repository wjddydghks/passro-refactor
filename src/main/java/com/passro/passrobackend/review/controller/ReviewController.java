package com.passro.passrobackend.review.controller;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.global.response.APIResponse;
import com.passro.passrobackend.review.code.ReviewSuccessCode;
import com.passro.passrobackend.review.dto.ReviewAverageResponseDto;
import com.passro.passrobackend.review.dto.ReviewCreateRequestDto;
import com.passro.passrobackend.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.passro.passrobackend.global.configuration.SwaggerErrorExamples.*;
import static com.passro.passrobackend.global.configuration.SwaggerSuccessExamples.REVIEW_AVERAGE;
import static com.passro.passrobackend.global.configuration.SwaggerSuccessExamples.REVIEW_CREATED;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "리뷰", description = "배송기사 리뷰 작성 및 평점 조회 API")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "배송기사 리뷰 작성", description = "배송이 완료된 건에 대해 발송자가 배송기사 리뷰를 작성합니다. 배송 건당 한 번만 작성할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "리뷰 작성 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "REVIEW201_1", summary = "리뷰 작성 성공", value = REVIEW_CREATED))),
            @ApiResponse(responseCode = "400", description = "잘못된 평점, 미완료 배송 또는 이미 작성된 리뷰",
                    content = @Content(schema = @Schema(implementation = APIResponse.class), examples = {
                            @ExampleObject(name = "REVIEW400_1", summary = "배송 미완료", value = REVIEW_DELIVERY_NOT_COMPLETED),
                            @ExampleObject(name = "REVIEW400_2", summary = "리뷰 중복", value = REVIEW_ALREADY_EXISTS),
                            @ExampleObject(name = "REVIEW400_3", summary = "평점 범위 오류", value = REVIEW_INVALID_RATING),
                            @ExampleObject(name = "REVIEW400_4", summary = "배송 ID 누락", value = REVIEW_INVALID_DELIVERY_ID)
                    })),
            @ApiResponse(responseCode = "403", description = "해당 배송의 발송자가 아님",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "REVIEW403_1", summary = "리뷰 작성 권한 없음", value = REVIEW_FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "배송 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "REVIEW404_1", summary = "배송 정보 없음", value = REVIEW_DELIVERY_NOT_FOUND)))
    })
    public APIResponse<String> createReview(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @RequestBody ReviewCreateRequestDto request
    ) {
        reviewService.createReview(account, request);
        return APIResponse.onSuccess(ReviewSuccessCode.REVIEW_CREATED, "리뷰 작성이 완료되었습니다.");
    }

    @GetMapping("/average/{userId}")
    @Operation(summary = "배송기사 평균 평점 조회", description = "사용자 ID에 해당하는 배송기사의 평균 리뷰 평점을 조회합니다. 리뷰가 없으면 0점을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "평균 평점 조회 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "REVIEW200_1", summary = "평균 평점 조회 성공", value = REVIEW_AVERAGE))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "REVIEW404_2", summary = "사용자 정보 없음", value = REVIEW_ACCOUNT_NOT_FOUND)))
    })
    public APIResponse<ReviewAverageResponseDto> getAverageRating(
            @Parameter(description = "조회할 사용자 ID", example = "1") @PathVariable Long userId) {
        return APIResponse.onSuccess(
                ReviewSuccessCode.REVIEW_AVERAGE_RATING_FOUND,
                reviewService.getAverageRating(userId)
        );
    }
}
