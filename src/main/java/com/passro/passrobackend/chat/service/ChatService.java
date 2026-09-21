package com.passro.passrobackend.chat.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.chat.dto.ChatMessageRequestDto;
import com.passro.passrobackend.chat.dto.ChatMessageResponseDto;
import com.passro.passrobackend.chat.dto.ChatMessageSendResponseDto;
import com.passro.passrobackend.chat.dto.ChatPartnerDto;
import com.passro.passrobackend.chat.dto.ChatRoomInfoResponseDto;
import com.passro.passrobackend.chat.dto.ChatRoomListItemResponseDto;
import com.passro.passrobackend.chat.entity.ChatMessage;
import com.passro.passrobackend.chat.entity.ChatRoom;
import com.passro.passrobackend.chat.exception.ChatException;
import com.passro.passrobackend.chat.exception.code.ChatErrorCode;
import com.passro.passrobackend.chat.repository.ChatMessageRepository;
import com.passro.passrobackend.chat.repository.ChatRoomRepository;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.file.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final DeliveryRepository deliveryRepository;
    private final S3Service s3Service;

    // 채팅 참여자 여부 검증 (sender 또는 shipper만 접근 가능)
    private Delivery getDeliveryAndValidateAccess(Long deliveryId, Account account) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.DELIVERY_NOT_FOUND));
        validateAccess(delivery, account);
        chatRoomRepository.findByDeliveryId(deliveryId)
                .ifPresent(chatRoom -> validateNotLeft(chatRoom, account));
        return delivery;
    }

    // 메시지 전체 조회 (최초 진입) — 조회 시 상대방 메시지 읽음 처리
    @Transactional
    public List<ChatMessageResponseDto> getMessages(Long deliveryId, Account account) {
        getDeliveryAndValidateAccess(deliveryId, account);
        chatMessageRepository.markAllAsRead(deliveryId, account.getId());
        return chatMessageRepository.findAllResponseByDeliveryIdOrderByCreatedAtAsc(deliveryId);
    }

    // polling: lastMessageId 이후 새 메시지만 조회 — 조회 시 상대방 메시지 읽음 처리
    @Transactional
    public List<ChatMessageResponseDto> getMessagesAfter(Long deliveryId, Long afterId, Account account) {
        getDeliveryAndValidateAccess(deliveryId, account);
        List<ChatMessageResponseDto> messages = chatMessageRepository
                .findAllResponseByDeliveryIdAndIdGreaterThanOrderByIdAsc(deliveryId, afterId);
        if (hasUnreadPartnerMessage(messages, account.getId())) {
            chatMessageRepository.markAllAsRead(deliveryId, account.getId());
            return markPartnerMessagesAsRead(messages, account.getId());
        }
        return messages;
    }

    // 안읽은 메시지 수 조회
    @Transactional(readOnly = true)
    public long getUnreadCount(Long deliveryId, Account account) {
        getDeliveryAndValidateAccess(deliveryId, account);
        return chatMessageRepository.countByDelivery_IdAndSender_IdNotAndIsReadFalse(deliveryId, account.getId());
    }

    // 채팅방 헤더 정보 조회
    @Transactional(readOnly = true)
    public ChatRoomInfoResponseDto getChatRoomInfo(Long deliveryId, Account account) {
        Delivery delivery = getDeliveryAndValidateAccess(deliveryId, account);

        boolean isSender = delivery.getSender().getId().equals(account.getId());
        Account partner = isSender ? delivery.getShipper() : delivery.getSender();

        return new ChatRoomInfoResponseDto(
                partner.getNickname(),
                imageUrl(partner.getPicture()),
                delivery.getDeliveryGoodInfo() != null
                        ? delivery.getDeliveryGoodInfo().getName()
                        : null,
                delivery.getOrigin() != null ? delivery.getOrigin().getSubwayStationName() : null,
                delivery.getDest() != null ? delivery.getDest().getSubwayStationName() : null,
                delivery.getStatus()
        );
    }

    // 채팅방 목록 조회 (사용자가 참여 중인 모든 채팅방, 최근 메시지 순 정렬)
    @Transactional(readOnly = true)
    public List<ChatRoomListItemResponseDto> getChatRoomList(Account account) {
        List<ChatRoom> chatRooms = chatRoomRepository.findAllActiveByAccount(
                account, List.of(DeliveryState.WAIT, DeliveryState.CANCEL));

        return chatRooms.stream()
                .map(chatRoom -> {
                    Delivery delivery = chatRoom.getDelivery();
                    Account partner = delivery.getSender().getId().equals(account.getId())
                            ? delivery.getShipper()
                            : delivery.getSender();

                    ChatMessage lastMsg = chatMessageRepository
                            .findTopByDelivery_IdOrderByCreatedAtDesc(delivery.getId())
                            .orElse(null);

                    long unreadCount = chatMessageRepository
                            .countByDelivery_IdAndSender_IdNotAndIsReadFalse(delivery.getId(), account.getId());

                    return new ChatRoomListItemResponseDto(
                            chatRoom.getId(),
                            delivery.getId(),
                            ChatPartnerDto.from(partner, this::imageUrl),
                            delivery.getDeliveryGoodInfo() != null ? delivery.getDeliveryGoodInfo().getName() : null,
                            lastMsg != null ? lastMsg.getContent() : null,
                            lastMsg != null ? lastMsg.getCreatedAt() : null,
                            unreadCount
                    );
                })
                .sorted(Comparator.comparing(
                        ChatRoomListItemResponseDto::lastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private String imageUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }
        return s3Service.getPresignedDownloadUrlString(imageKey);
    }

    private boolean hasUnreadPartnerMessage(List<ChatMessageResponseDto> messages, Long accountId) {
        return messages.stream()
                .anyMatch(message -> !message.senderId().equals(accountId) && !message.isRead());
    }

    private List<ChatMessageResponseDto> markPartnerMessagesAsRead(
            List<ChatMessageResponseDto> messages,
            Long accountId
    ) {
        return messages.stream()
                .map(message -> message.senderId().equals(accountId) || message.isRead()
                        ? message
                        : new ChatMessageResponseDto(
                                message.id(),
                                message.senderId(),
                                message.senderNickname(),
                                message.content(),
                                true,
                                message.createdAt(),
                                message.imageKey(),
                                message.systemMessage()))
                .toList();
    }

    // 메시지 전송
    @Transactional
    public ChatMessageSendResponseDto sendMessage(
            Long deliveryId,
            ChatMessageRequestDto request,
            Account account) {
        Delivery delivery = getDeliveryForUpdateAndValidateAccess(deliveryId, account);
        ChatRoom chatRoom = chatRoomRepository.findByDeliveryId(deliveryId)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder()
                        .delivery(delivery)
                        .build()));
        validateNotLeft(chatRoom, account);

        String imageKey = normalizeImageKey(request.imageKey());
        if (imageKey != null) {
            s3Service.validateUploadedImage(imageKey);
        }

        ChatMessage message = ChatMessage.builder()
                .delivery(delivery)
                .sender(account)
                .content(request.content())
                .imageKey(imageKey)
                .systemMessage(false)
                .build();

        return ChatMessageSendResponseDto.of(chatRoom, chatMessageRepository.save(message));
    }

    /**
     * 배송 상태 변경 안내를 채팅방에 등록하고, 검증·확정된 이미지가 있으면 함께 첨부한다.
     * 전달자가 채팅방을 나간 상태여도 발송자는 상태 변경 및 증빙 이미지를 확인할 수 있어야 하므로
     * 사용자 채팅 전송의 퇴장 여부 검증은 적용하지 않는다.
     */
    @Transactional
    public void sendDeliveryStatusMessage(
            Delivery delivery,
            Account sender,
            String content,
            String imageKey
    ) {
        validateAccess(delivery, sender);

        chatRoomRepository.findByDeliveryId(delivery.getId())
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder()
                        .delivery(delivery)
                        .build()));

        chatMessageRepository.save(ChatMessage.builder()
                .delivery(delivery)
                .sender(sender)
                .content(content)
                .imageKey(normalizeImageKey(imageKey))
                .systemMessage(true)
                .build());
    }

    // 채팅방 나가기 — 상대방의 채팅방과 메시지는 유지하고 요청자에게만 숨김 처리
    @Transactional
    public void leaveChatRoom(Long deliveryId, Account account) {
        Delivery delivery = getDeliveryForUpdateAndValidateAccess(deliveryId, account);
        ChatRoom chatRoom = chatRoomRepository.findByDeliveryId(deliveryId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        chatRoom.leave(account.getId());
        chatRoomRepository.save(chatRoom);
    }

    private Delivery getDeliveryForUpdateAndValidateAccess(Long deliveryId, Account account) {
        Delivery delivery = deliveryRepository.findByIdForUpdate(deliveryId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.DELIVERY_NOT_FOUND));
        validateAccess(delivery, account);
        return delivery;
    }

    private void validateAccess(Delivery delivery, Account account) {
        if (delivery.getStatus() == DeliveryState.WAIT || delivery.getStatus() == DeliveryState.CANCEL) {
            throw new ChatException(ChatErrorCode.CHAT_NOT_AVAILABLE);
        }

        boolean isSender = delivery.getSender() != null && delivery.getSender().getId().equals(account.getId());
        boolean isShipper = delivery.getShipper() != null && delivery.getShipper().getId().equals(account.getId());

        if (!isSender && !isShipper) {
            throw new ChatException(ChatErrorCode.FORBIDDEN_ACCESS);
        }
    }

    private void validateNotLeft(ChatRoom chatRoom, Account account) {
        if (chatRoom.hasLeft(account.getId())) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_ALREADY_LEFT);
        }
    }

    private String normalizeImageKey(String imageKey) {
        return imageKey == null || imageKey.isBlank() ? null : imageKey;
    }
}
