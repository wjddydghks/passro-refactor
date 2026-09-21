package com.passro.passrobackend.chat.dto;

import java.time.LocalDateTime;

public record ChatRoomListItemResponseDto(
        Long chatRoomId,
        Long deliveryId,
        ChatPartnerDto partner,
        String itemName,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount
) {
    public ChatRoomListItemResponseDto(
            Long deliveryId,
            ChatPartnerDto partner,
            String itemName,
            String lastMessage,
            LocalDateTime lastMessageAt,
            long unreadCount) {
        this(null, deliveryId, partner, itemName, lastMessage, lastMessageAt, unreadCount);
    }
}
