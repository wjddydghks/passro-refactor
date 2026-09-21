package com.passro.passrobackend.chat.controller;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.chat.code.ChatSuccessCode;
import com.passro.passrobackend.chat.dto.ChatMessageRequestDto;
import com.passro.passrobackend.chat.dto.ChatMessageResponseDto;
import com.passro.passrobackend.chat.dto.ChatMessageSendResponseDto;
import com.passro.passrobackend.chat.dto.ChatRoomInfoResponseDto;
import com.passro.passrobackend.chat.service.ChatService;
import com.passro.passrobackend.global.response.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "채팅", description = "채팅 API - delivery의 sender/shipper 간 1:1 채팅. WAIT·CANCEL 상태의 배송건은 접근 불가.")
@RestController
@RequestMapping("/chat/{deliveryId}")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "메시지 조회",
            description = "afterId 없으면 전체 메시지 반환 (최초 진입), afterId 있으면 해당 id 이후의 새 메시지만 반환 (polling). 조회 시 상대방 메시지 자동 읽음 처리."
    )
    @ApiResponse(responseCode = "200", description = "메시지 조회 성공", useReturnTypeSchema = true)
    @GetMapping("/messages")
    public APIResponse<List<ChatMessageResponseDto>> getMessages(
            @PathVariable Long deliveryId,
            @Parameter(description = "마지막으로 받은 메시지 id. 없으면 전체 조회, 있으면 해당 id 이후 메시지만 반환")
            @RequestParam(required = false) Long afterId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account
    ) {
        List<ChatMessageResponseDto> messages = (afterId == null)
                ? chatService.getMessages(deliveryId, account)
                : chatService.getMessagesAfter(deliveryId, afterId, account);

        return APIResponse.onSuccess(ChatSuccessCode.OK, messages);
    }

    @Operation(
            summary = "메시지 전송",
            description = "메시지를 전송합니다. imageKey는 선택값이며, POST /file/image/upload-url로 먼저 업로드한 이미지 키를 전달합니다. " +
                    "해당 deliveryId의 채팅방이 없으면 최초 전송 시 생성하며, 채팅방과 저장된 메시지를 함께 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "메시지 전송 성공", useReturnTypeSchema = true)
    @PostMapping("/messages")
    public APIResponse<ChatMessageSendResponseDto> sendMessage(
            @PathVariable Long deliveryId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Valid @RequestBody ChatMessageRequestDto request
    ) {
        return APIResponse.onSuccess(ChatSuccessCode.CREATED, chatService.sendMessage(deliveryId, request, account));
    }

    @Operation(
            summary = "채팅방 헤더 정보 조회",
            description = "채팅방 상단에 표시할 정보를 반환한다. 상대방 닉네임·프로필 사진, 물품명, 출발지·도착지, 현재 배송 상태를 포함한다."
    )
    @ApiResponse(responseCode = "200", description = "채팅방 정보 조회 성공", useReturnTypeSchema = true)
    @GetMapping("/info")
    public APIResponse<ChatRoomInfoResponseDto> getChatRoomInfo(
            @PathVariable Long deliveryId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account
    ) {
        return APIResponse.onSuccess(ChatSuccessCode.OK, chatService.getChatRoomInfo(deliveryId, account));
    }

    @Operation(
            summary = "안읽은 메시지 수 조회",
            description = "상대방이 보낸 메시지 중 아직 읽지 않은 메시지 수를 반환한다. 채팅 목록 화면의 뱃지 숫자 표시에 사용한다."
    )
    @GetMapping("/unread-count")
    public APIResponse<Long> getUnreadCount(
            @PathVariable Long deliveryId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account
    ) {
        return APIResponse.onSuccess(ChatSuccessCode.OK, chatService.getUnreadCount(deliveryId, account));
    }

    @Operation(
            summary = "채팅방 나가기",
            description = "채팅방을 현재 사용자의 목록에서 제거합니다. 상대방의 채팅방과 기존 메시지는 유지되며, 나간 사용자는 해당 채팅방에 다시 접근하거나 메시지를 전송할 수 없습니다. 반복 요청도 성공합니다."
    )
    @ApiResponse(responseCode = "200", description = "채팅방 나가기 성공", useReturnTypeSchema = true)
    @DeleteMapping
    public APIResponse<Void> leaveChatRoom(
            @PathVariable Long deliveryId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account
    ) {
        chatService.leaveChatRoom(deliveryId, account);
        return APIResponse.onSuccess(ChatSuccessCode.LEFT, null);
    }
}
