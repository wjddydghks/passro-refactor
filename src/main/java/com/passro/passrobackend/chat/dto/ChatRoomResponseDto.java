package com.passro.passrobackend.chat.dto;

import com.passro.passrobackend.chat.entity.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(types = "object", description = "채팅방 응답")
public record ChatRoomResponseDto(
        Long id,
        Long deliveryId,
        LocalDateTime createdAt
) {
    public static ChatRoomResponseDto from(ChatRoom chatRoom) {
        return new ChatRoomResponseDto(
                chatRoom.getId(),
                chatRoom.getDelivery().getId(),
                chatRoom.getCreatedAt());
    }
}
