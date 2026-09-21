package com.passro.passrobackend.sender.controller;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.global.response.APIResponse;
import com.passro.passrobackend.delivery.dto.DeliveryStatusUpdateRequestDto;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.delivery.location.dto.ShipperLocationResponseDto;
import com.passro.passrobackend.delivery.location.service.ShipperLocationService;
import com.passro.passrobackend.sender.code.SenderSuccessCode;
import com.passro.passrobackend.sender.dto.SenderDeliveryCreateRequestDto;
import com.passro.passrobackend.sender.dto.SenderDeliveryDetailDto;
import com.passro.passrobackend.sender.dto.SenderDeliveryListDto;
import com.passro.passrobackend.sender.dto.SenderPaymentAmountDto;
import com.passro.passrobackend.sender.service.SenderQueryService;
import com.passro.passrobackend.sender.service.SenderCommandService;
import com.passro.passrobackend.subway.dto.SubwayRouteResponseDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.passro.passrobackend.global.configuration.SwaggerErrorExamples.*;
import static com.passro.passrobackend.global.configuration.SwaggerSuccessExamples.*;

@RestController
@RequestMapping("/sender")
@RequiredArgsConstructor
@Tag(name = "발송자", description = "발송자의 배송 요청 조회 및 관리 API")
public class SenderController {

    private final SenderQueryService senderQueryService;
    private final SenderCommandService senderCommandService;
    private final ShipperLocationService shipperLocationService;

    @GetMapping("/{deliveryId}/shipper-location")
    @Operation(
            summary = "배송기사 현재 위치 조회",
            description = "발송자가 배송 중인 본인 배송의 배송기사 위치와 현재 역부터 배송 도착역까지의 예상 소요시간(분)을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "위치 조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "배송 중 상태가 아님",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(
                                    name = "SHIPPER_LOCATION400_1",
                                    summary = "위치 조회 불가능",
                                    value = SHIPPER_LOCATION_TRACKING_NOT_AVAILABLE))),
            @ApiResponse(responseCode = "403", description = "배송 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(
                                    name = "DELIVERY403_1",
                                    summary = "배송 접근 권한 없음",
                                    value = DELIVERY_FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "배송, 위치, 역 또는 경로 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class), examples = {
                            @ExampleObject(name = "DELIVERY404_1", summary = "배송 정보 없음", value = DELIVERY_NOT_FOUND),
                            @ExampleObject(name = "SHIPPER_LOCATION404_1", summary = "위치 정보 없음", value = SHIPPER_LOCATION_NOT_FOUND),
                            @ExampleObject(name = "SUBWAY404_1", summary = "현재 역 또는 도착역 없음", value = SUBWAY_PLACE_NOT_FOUND),
                            @ExampleObject(name = "SUBWAY404_2", summary = "도착 경로 없음", value = SUBWAY_ROUTE_NOT_FOUND)
                    }))
    })
    public APIResponse<ShipperLocationResponseDto> getShipperLocation(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "배송 ID", example = "1") @PathVariable Long deliveryId) {
        return APIResponse.onSuccess(
                SenderSuccessCode.OK, shipperLocationService.getLocation(account, deliveryId));
    }

    @GetMapping("/{deliveryId}/routes/shipper-commute")
    @Operation(
            summary = "매칭 배송기사 통학 경로 조회",
            description = "발송자가 본인 배송에 매칭된 배송기사의 출발역·경유역·도착역 기준 최단 경로를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "통학 경로 조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "배송 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(
                                    name = "DELIVERY403_1",
                                    summary = "배송 접근 권한 없음",
                                    value = DELIVERY_FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "배송, 배송기사, 통학 경로 또는 지하철 경로를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class), examples = {
                            @ExampleObject(name = "DELIVERY404_1", summary = "배송 정보 없음", value = DELIVERY_NOT_FOUND),
                            @ExampleObject(name = "DELIVERY404_4", summary = "매칭 배송기사 없음", value = DELIVERY_SHIPPER_NOT_ASSIGNED),
                            @ExampleObject(name = "DELIVERY404_5", summary = "배송기사 통학 경로 없음", value = DELIVERY_SHIPPER_ROUTE_NOT_FOUND),
                            @ExampleObject(name = "SUBWAY404_1", summary = "통학 경로의 역 없음", value = SUBWAY_PLACE_NOT_FOUND),
                            @ExampleObject(name = "SUBWAY404_2", summary = "통학 경로 탐색 불가능", value = SUBWAY_ROUTE_NOT_FOUND)
                    }))
    })
    public APIResponse<SubwayRouteResponseDto> getShipperCommuteRoute(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "배송 ID", example = "1") @PathVariable Long deliveryId) {
        return APIResponse.onSuccess(
                SenderSuccessCode.OK, senderQueryService.getShipperCommuteRoute(account, deliveryId));
    }

    @GetMapping("/{deliveryId}/routes/delivery")
    @Operation(
            summary = "배송 출발·도착 경로 조회",
            description = "발송자가 본인 배송에 등록된 출발역부터 도착역까지의 최단 경로를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배송 경로 조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "배송 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(
                                    name = "DELIVERY403_1",
                                    summary = "배송 접근 권한 없음",
                                    value = DELIVERY_FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "배송, 지하철역 또는 경로를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class), examples = {
                            @ExampleObject(name = "DELIVERY404_1", summary = "배송 정보 없음", value = DELIVERY_NOT_FOUND),
                            @ExampleObject(name = "SUBWAY404_1", summary = "배송 출발역 또는 도착역 없음", value = SUBWAY_PLACE_NOT_FOUND),
                            @ExampleObject(name = "SUBWAY404_2", summary = "배송 경로 탐색 불가능", value = SUBWAY_ROUTE_NOT_FOUND)
                    }))
    })
    public APIResponse<SubwayRouteResponseDto> getDeliveryRoute(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "배송 ID", example = "1") @PathVariable Long deliveryId) {
        return APIResponse.onSuccess(
                SenderSuccessCode.OK, senderQueryService.getDeliveryRoute(account, deliveryId));
    }

    // 발송자 배송 조회
    @GetMapping
    @Operation(summary = "내 배송 목록 조회", description = "로그인한 발송자가 요청한 배송 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
    public APIResponse<List<SenderDeliveryListDto>> getSenders(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "배송 상태 필터", example = "DELIVERING")
            @RequestParam(required = false) DeliveryState status) {
        return APIResponse.onSuccess(SenderSuccessCode.OK, senderQueryService.getSenders(account, status));
    }

    // 발송자 배송 단건 조회
    @GetMapping("/{deliveryId}")
    @Operation(summary = "배송 상세 조회", description = "본인이 요청한 배송의 현재 상태와 진행 이력을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "해당 배송에 접근할 권한이 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY403_1", summary = "배송 접근 권한 없음", value = DELIVERY_FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "배송 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY404_1", summary = "배송 정보 없음", value = DELIVERY_NOT_FOUND)))
    })
    public APIResponse<SenderDeliveryDetailDto> getSenderById(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "배송 ID", example = "1") @PathVariable Long deliveryId) {
        return APIResponse.onSuccess(SenderSuccessCode.OK, senderQueryService.getDeliveryDetail(account, deliveryId));
    }

    // 결제 금액 계산
    @GetMapping("/payment")
    @Operation(summary = "배송 결제 금액 계산", description = "배송 요청 생성 전, 출발역·도착역·물품 크기를 기반으로 기본·거리·무게 및 총 결제 포인트를 계산합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "계산 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "SENDER200_1", summary = "결제 금액 계산 성공", value = SENDER_PAYMENT))),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "404", description = "장소 또는 경로 정보를 찾을 수 없음")
    })
    public APIResponse<SenderPaymentAmountDto> getPaymentAmount(
            @Parameter(description = "출발역 Place ID", example = "101") @RequestParam Long sourceStationId,
            @Parameter(description = "도착역 Place ID", example = "420") @RequestParam Long destinationStationId,
            @Parameter(description = "물품 크기 (S, M, L)", example = "M") @RequestParam String size) {
        return APIResponse.onSuccess(SenderSuccessCode.OK, senderQueryService.getPaymentAmount(sourceStationId, destinationStationId, size));
    }

    // 배송 요청
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "배송 요청 생성",
            description = "출발지, 도착지, 물품 정보를 입력해 새 배송을 요청합니다. "
                    + "배송 가격은 이동 거리와 물품 크기를 기준으로 서버에서 계산하며, 응답 result는 생성된 배송 ID입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "배송 요청 생성 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "SENDER201_1", summary = "배송 요청 생성 성공", value = SENDER_CREATED))),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 또는 출발역과 도착역이 동일함",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = {
                                    @ExampleObject(name = "COMMON400", summary = "요청 값 검증 실패", value = COMMON_VALIDATION),
                                    @ExampleObject(name = "DELIVERY400_4", summary = "출발역과 도착역 동일 불가능", value = DELIVERY_SAME_ORIGIN_DEST)
                            })),
            @ApiResponse(responseCode = "404", description = "해당 출발역/도착역 장소를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY404_2", summary = "장소 정보 없음", value = DELIVERY_PLACE_NOT_FOUND)))
    })
    public APIResponse<String> createDelivery(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Valid @RequestBody SenderDeliveryCreateRequestDto request) {
        Long deliveryId = senderCommandService.createDelivery(account, request);
        return APIResponse.onSuccess(SenderSuccessCode.CREATED, deliveryId.toString());
    }

    // 발송 완료 처리
    @PatchMapping("/{deliveryId}/complete")
    @Operation(summary = "배송 완료 승인", description = "배송기사가 요청한 배송 완료를 승인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배송 완료 처리 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "SENDER200_1", summary = "배송 완료 처리 성공", value = SENDER_OK))),
            @ApiResponse(responseCode = "400", description = "배송 완료 처리 가능한 상태가 아님",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY400_2", summary = "배송 완료 처리 불가능", value = DELIVERY_INVALID_COMPLETION_STATUS))),
            @ApiResponse(responseCode = "403", description = "해당 배송에 접근할 권한이 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY403_1", summary = "배송 접근 권한 없음", value = DELIVERY_FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "배송 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY404_1", summary = "배송 정보 없음", value = DELIVERY_NOT_FOUND)))
    })
    public APIResponse<Void> completeDelivery(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "배송 ID", example = "1") @PathVariable Long deliveryId,
            @RequestBody(required = false) DeliveryStatusUpdateRequestDto request) {
        senderCommandService.completeDelivery(account, deliveryId, request == null ? null : request.getImageKey());
        return APIResponse.onSuccess(SenderSuccessCode.OK, null);
    }

    // 발송 약관 동의
    @PatchMapping("/{deliveryId}/terms")
    @Operation(summary = "배송 약관 동의", description = "해당 배송 요청의 약관 동의 상태를 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "약관 동의 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "SENDER200_1", summary = "약관 동의 성공", value = SENDER_OK))),
            @ApiResponse(responseCode = "403", description = "해당 배송에 접근할 권한이 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY403_1", summary = "배송 접근 권한 없음", value = DELIVERY_FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "배송 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY404_1", summary = "배송 정보 없음", value = DELIVERY_NOT_FOUND)))
    })
    public APIResponse<Void> agreeTerms(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "배송 ID", example = "1") @PathVariable Long deliveryId) {
        senderCommandService.agreeTerms(account, deliveryId);
        return APIResponse.onSuccess(SenderSuccessCode.OK, null);
    }

    // 발송 요청 취소
    @PatchMapping("/{deliveryId}/cancel")
    @Operation(summary = "배송 요청 취소", description = "매칭 전인 배송 요청을 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배송 요청 취소 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "SENDER200_1", summary = "배송 요청 취소 성공", value = SENDER_OK))),
            @ApiResponse(responseCode = "400", description = "이미 매칭되어 취소할 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY400_1", summary = "배송 취소 불가능", value = DELIVERY_CANNOT_CANCEL))),
            @ApiResponse(responseCode = "403", description = "해당 배송에 접근할 권한이 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY403_1", summary = "배송 접근 권한 없음", value = DELIVERY_FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "배송 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "DELIVERY404_1", summary = "배송 정보 없음", value = DELIVERY_NOT_FOUND)))
    })
    public APIResponse<Void> cancelDelivery(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "배송 ID", example = "1") @PathVariable Long deliveryId) {
        senderCommandService.cancelDelivery(account, deliveryId);
        return APIResponse.onSuccess(SenderSuccessCode.OK, null);
    }

}
