package com.passro.passrobackend.shipper.controller;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.global.response.APIResponse;
import com.passro.passrobackend.delivery.dto.DeliveryStatusUpdateRequestDto;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.delivery.location.dto.ShipperLocationResponseDto;
import com.passro.passrobackend.delivery.location.dto.ShipperLocationUpdateRequestDto;
import com.passro.passrobackend.delivery.location.service.ShipperLocationService;
import com.passro.passrobackend.shipper.code.ShipperSuccessCode;
import com.passro.passrobackend.shipper.dto.ShipperDeliveryDetailDto;
import com.passro.passrobackend.shipper.dto.ShipperDeliveryListDto;
import com.passro.passrobackend.shipper.service.ShipperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.passro.passrobackend.global.configuration.SwaggerErrorExamples.DELIVERY_NOT_FOUND;
import static com.passro.passrobackend.global.configuration.SwaggerErrorExamples.DELIVERY_SELF_MATCH_FORBIDDEN;
import static com.passro.passrobackend.global.configuration.SwaggerSuccessExamples.SHIPPER_OK;

import com.passro.passrobackend.shipper.service.ShipperMatchingService;

@RestController
@RequestMapping("/shipper")
@RequiredArgsConstructor
@Tag(name = "전달자", description = "전즈사의 배송 조회 및 배송 상태 변경 API")
public class ShipperController {
    private final ShipperService shipperService;
    private final ShipperMatchingService shipperMatchingService;
    private final ShipperLocationService shipperLocationService;

    @PutMapping("/location")
    @Operation(
            summary = "현재 위치 갱신",
            description = "전달중인 전달자의 현재 위도·경도를 갱신합니다. placeId는 선택값이며, 생략하면 전달된 좌표에서 가장 가까운 지하철역을 자동으로 선택합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "위치 갱신 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "좌표 또는 전달된 역 ID 검증 실패"),
            @ApiResponse(responseCode = "403", description = "전달 중인 전달자가 아님"),
            @ApiResponse(responseCode = "404", description = "전달된 역 또는 좌표와 비교할 역 정보를 찾을 수 없음")
    })
    public APIResponse<ShipperLocationResponseDto> updateLocation(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Valid @RequestBody ShipperLocationUpdateRequestDto request) {
        return APIResponse.onSuccess(
                ShipperSuccessCode.OK, shipperLocationService.updateLocation(account, request));
    }

    @GetMapping("/matched")
    @ResponseBody
    @Operation(
            summary = "매칭 대기 배송 목록 조회",
            description = "로그인한 전달자의 권역 및 동선을 기반으로 우선순위 정렬된 배송 요청 목록을 조회합니다. 본인이 요청한 배송은 제외됩니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
    public APIResponse<List<ShipperDeliveryListDto>> listMatched(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account) {
        return APIResponse.onSuccess(ShipperSuccessCode.OK,
                shipperMatchingService.listMatchRequestedWithPriority(account).stream()
                        .map(result -> shipperService.toDeliveryListDto(
                                result.delivery(), result.estimatedTimeMinutes()))
                        .toList());
    }

    @GetMapping("/")
    @ResponseBody
    @Operation(summary = "내 배송 목록 조회", description = "로그인한 전달자에게 배정된 배송 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
    public APIResponse<List<ShipperDeliveryListDto>> listDelivery(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "배송 상태 필터", example = "DELIVERING")
            @RequestParam(required = false) DeliveryState status) {
         return APIResponse.onSuccess(ShipperSuccessCode.OK,
                 shipperService.listAllByShipper(account, status).stream()
                         .map(shipperService::toDeliveryListDto)
                         .toList());
    }

    @GetMapping("/{deliveryId}/")
    @ResponseBody
    @Operation(summary = "배송 상세 조회", description = "배송 ID로 배송 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "배송 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY404_1", summary = "배송 정보 없음", value = DELIVERY_NOT_FOUND)))
    })
    public APIResponse<ShipperDeliveryDetailDto> getDelivery(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "배송 ID", example = "1") @PathVariable("deliveryId") Long deliveryId) {
        return APIResponse.onSuccess(ShipperSuccessCode.OK, shipperService.getDeliveryById(account, deliveryId));
    }

    @PatchMapping("/{deliveryId}/matched")
    @ResponseBody
    @Operation(summary = "배송 매칭 수락", description = "배송 요청을 수락하고 전달자를 배정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매칭 수락 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "SHIPPER200_1", summary = "매칭 수락 성공", value = SHIPPER_OK))),
            @ApiResponse(responseCode = "403", description = "본인이 요청한 배송",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(
                                    name = "DELIVERY403_2",
                                    summary = "본인 배송 수락 불가",
                                    value = DELIVERY_SELF_MATCH_FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "배송 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY404_1", summary = "배송 정보 없음", value = DELIVERY_NOT_FOUND)))
    })
    public APIResponse<Void> matchAccept(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "배송 ID", example = "1") @PathVariable("deliveryId") Long deliveryId) {
        shipperService.matchAccept(account, deliveryId);
        return APIResponse.onSuccess(ShipperSuccessCode.OK, null);
    }

    @PatchMapping("/{deliveryId}/acquire")
    @ResponseBody
    @Operation(
            summary = "물품 인수 처리",
            description = "물품을 인수하고 배송 상태를 배송 중으로 변경합니다. 해당 배송 채팅방에 상태 메시지가 자동 등록되며, 이미지가 있으면 함께 첨부됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인수 처리 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "SHIPPER200_1", summary = "물품 인수 처리 성공", value = SHIPPER_OK))),
            @ApiResponse(responseCode = "404", description = "배송 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY404_1", summary = "배송 정보 없음", value = DELIVERY_NOT_FOUND)))
    })
    public APIResponse<Void> acquireAccept(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "배송 ID", example = "1") @PathVariable("deliveryId") Long deliveryId,
            @RequestBody(required = false) DeliveryStatusUpdateRequestDto request) {
        shipperService.acquireAccept(account, deliveryId, request == null ? null : request.getImageKey());
        return APIResponse.onSuccess(ShipperSuccessCode.OK, null);
    }

    @PatchMapping("/{deliveryId}/confirm")
    @ResponseBody
    @Operation(
            summary = "배송 완료 확인 요청",
            description = "발송자에게 배송 완료 확인을 요청합니다. 해당 배송 채팅방에 상태 메시지가 자동 등록되며, 이미지가 있으면 함께 첨부됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "확인 요청 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "SHIPPER200_1", summary = "배송 완료 확인 요청 성공", value = SHIPPER_OK))),
            @ApiResponse(responseCode = "404", description = "배송 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY404_1", summary = "배송 정보 없음", value = DELIVERY_NOT_FOUND)))
    })
    public APIResponse<Void> acquireConfirm(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "배송 ID", example = "1") @PathVariable("deliveryId") Long deliveryId,
            @RequestBody(required = false) DeliveryStatusUpdateRequestDto request) {
        shipperService.acquireConfirm(account, deliveryId, request == null ? null : request.getImageKey());
        return APIResponse.onSuccess(ShipperSuccessCode.OK, null);
    }
}
