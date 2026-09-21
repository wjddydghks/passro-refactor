package com.passro.passrobackend.market.controller;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.global.response.APIResponse;
import com.passro.passrobackend.market.code.MarketSuccessCode;
import com.passro.passrobackend.market.dto.MarketCreateRequestDto;
import com.passro.passrobackend.market.dto.MarketItemResponseDto;
import com.passro.passrobackend.market.dto.MarketPurchaseResponseDto;
import com.passro.passrobackend.market.service.MarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/market")
@Tag(name = "마켓", description = "포인트 상품 조회 및 구매 API")
public class MarketController {

    private final MarketService marketService;

    @GetMapping
    @Operation(
            summary = "마켓 상품 목록 조회",
            description = "포인트로 구매할 수 있는 상품을 조회합니다. 카테고리를 생략하면 전체 상품을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 목록 조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 카테고리",
                    content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public APIResponse<List<MarketItemResponseDto>> getItems(
            @Parameter(
                    description = "상품 카테고리. 미입력 시 전체 조회",
                    example = "카페",
                    schema = @Schema(allowableValues = {"음식", "카페", "편의점", "기타"}))
            @RequestParam(required = false) String category) {
        return APIResponse.onSuccess(MarketSuccessCode.OK, marketService.getItems(category));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "마켓 상품 등록",
            description = "관리자가 마켓 상품을 등록합니다. 이미지 키는 마켓 전용 경로로 확정 저장됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "상품 등록 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "요청 값, 카테고리 또는 이미지가 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class))),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class))),
            @ApiResponse(responseCode = "404", description = "업로드된 이미지를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public APIResponse<MarketItemResponseDto> createItem(
            @Valid @RequestBody MarketCreateRequestDto request) {
        return APIResponse.onSuccess(MarketSuccessCode.CREATED, marketService.createItem(request));
    }

    @PostMapping("/{marketId}/purchase")
    @Operation(
            summary = "마켓 상품 구매",
            description = "로그인한 사용자의 포인트 잔액을 확인한 뒤 상품 가격만큼 차감합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 구매 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "상품 또는 포인트 계정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class))),
            @ApiResponse(responseCode = "409", description = "보유 포인트 부족",
                    content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public APIResponse<MarketPurchaseResponseDto> purchase(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "마켓 상품 ID", example = "1") @PathVariable Long marketId) {
        return APIResponse.onSuccess(
                MarketSuccessCode.OK,
                marketService.purchase(account.getId(), marketId));
    }
}
