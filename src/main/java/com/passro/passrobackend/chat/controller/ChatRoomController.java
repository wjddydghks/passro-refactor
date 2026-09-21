package com.passro.passrobackend.chat.controller;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.chat.code.ChatSuccessCode;
import com.passro.passrobackend.chat.dto.ChatRoomListItemResponseDto;
import com.passro.passrobackend.chat.service.ChatService;
import com.passro.passrobackend.global.response.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "채팅", description = "채팅 API - delivery의 sender/shipper 간 1:1 채팅. WAIT·CANCEL 상태의 배송건은 접근 불가.")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatService chatService;

    @Operation(
            summary = "채팅방 목록 조회",
            description = "로그인한 사용자가 참여 중인 채팅방 목록을 반환한다. " +
                    "각 항목에는 상대방 정보(id, 닉네임, 프로필 사진), 관련 물품명, " +
                    "마지막 메시지 내용 및 시각, 안읽은 메시지 수가 포함된다. " +
                    "최근 메시지 순으로 정렬되며, WAIT·CANCEL 상태의 배송건은 제외된다."
    )
    @GetMapping("/rooms")
    public APIResponse<List<ChatRoomListItemResponseDto>> getChatRoomList(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account
    ) {
        return APIResponse.onSuccess(ChatSuccessCode.OK, chatService.getChatRoomList(account));
    }
}