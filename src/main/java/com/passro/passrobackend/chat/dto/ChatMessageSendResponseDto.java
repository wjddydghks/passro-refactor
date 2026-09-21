package com.passro.passrobackend.chat.dto;

import com.passro.passrobackend.chat.entity.ChatMessage;
import com.passro.passrobackend.chat.entity.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(types = "object", description = "메시지 전송 응답. 기존 메시지 응답 필드를 유지하고 생성·조회된 채팅방 정보를 추가로 제공한다.")
public record ChatMessageSendResponseDto(
        Long id,
        Long senderId,
        String senderNickname,
        String content,
        boolean isRead,
        LocalDateTime createdAt,
        @Schema(description = "첨부 이미지 S3 키", nullable = true)
        String imageKey,
        @Schema(description = "시스템 메시지 여부", example = "false")
        boolean systemMessage,
        ChatRoomResponseDto chatRoom
) {
    public ChatMessageSendResponseDto(
            Long id,
            Long senderId,
            String senderNickname,
            String content,
            boolean isRead,
            LocalDateTime createdAt,
            ChatRoomResponseDto chatRoom
    ) {
        this(id, senderId, senderNickname, content, isRead, createdAt, null, false, chatRoom);
    }

    public ChatMessageSendResponseDto(
            Long id,
            Long senderId,
            String senderNickname,
            String content,
            boolean isRead,
            LocalDateTime createdAt,
            String imageKey,
            ChatRoomResponseDto chatRoom
    ) {
        this(id, senderId, senderNickname, content, isRead, createdAt, imageKey, false, chatRoom);
    }

    public static ChatMessageSendResponseDto of(ChatRoom chatRoom, ChatMessage message) {
        return new ChatMessageSendResponseDto(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getNickname(),
                message.getContent(),
                message.isRead(),
                message.getCreatedAt(),
                message.getImageKey(),
                message.isSystemMessage(),
                ChatRoomResponseDto.from(chatRoom));
    }
}
