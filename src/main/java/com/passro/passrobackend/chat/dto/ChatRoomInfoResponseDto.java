package com.passro.passrobackend.chat.dto;

import com.passro.passrobackend.delivery.enums.DeliveryState;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(types = "object", description = "채팅방 헤더 정보 응답")
public record ChatRoomInfoResponseDto(
        String partnerNickname,
        String partnerPicture,
        String itemName,
        String departure,
        String arrival,
        DeliveryState deliveryStatus
) {
}
