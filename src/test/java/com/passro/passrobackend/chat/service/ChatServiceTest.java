package com.passro.passrobackend.chat.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.chat.dto.ChatMessageRequestDto;
import com.passro.passrobackend.chat.dto.ChatMessageResponseDto;
import com.passro.passrobackend.chat.dto.ChatMessageSendResponseDto;
import com.passro.passrobackend.chat.dto.ChatRoomInfoResponseDto;
import com.passro.passrobackend.chat.entity.ChatMessage;
import com.passro.passrobackend.chat.entity.ChatRoom;
import com.passro.passrobackend.chat.exception.ChatException;
import com.passro.passrobackend.chat.exception.code.ChatErrorCode;
import com.passro.passrobackend.chat.repository.ChatMessageRepository;
import com.passro.passrobackend.chat.repository.ChatRoomRepository;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.entity.DeliveryGoodInfo;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.place.entity.Place;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.passro.passrobackend.chat.dto.ChatRoomListItemResponseDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTest {

    @Mock ChatMessageRepository chatMessageRepository;
    @Mock ChatRoomRepository chatRoomRepository;
    @Mock DeliveryRepository deliveryRepository;
    @Mock S3Service s3Service;

    @InjectMocks ChatService chatService;

    Account sender;
    Account shipper;
    Account outsider;
    Delivery delivery;

    @BeforeEach
    void setUp() {
        sender = mock(Account.class);
        given(sender.getId()).willReturn(1L);
        given(sender.getNickname()).willReturn("sender닉네임");
        given(sender.getPicture()).willReturn("sender.jpg");

        shipper = mock(Account.class);
        given(shipper.getId()).willReturn(2L);
        given(shipper.getNickname()).willReturn("shipper닉네임");
        given(shipper.getPicture()).willReturn("shipper.jpg");
        given(s3Service.getPresignedDownloadUrlString("sender.jpg")).willReturn("sender.jpg");
        given(s3Service.getPresignedDownloadUrlString("shipper.jpg")).willReturn("shipper.jpg");

        outsider = mock(Account.class);
        given(outsider.getId()).willReturn(99L);

        delivery = mock(Delivery.class);
        given(delivery.getId()).willReturn(1L);
        given(delivery.getSender()).willReturn(sender);
        given(delivery.getShipper()).willReturn(shipper);
        given(delivery.getStatus()).willReturn(DeliveryState.MATCHED);
    }

    // ──────────────────────────────────────────────
    // 채팅방 목록 조회
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("채팅방 목록 조회")
    class GetChatRoomList {

        @Test
        @DisplayName("sender로 참여한 경우 partner는 shipper")
        void asSender_partnerIsShipper() {
            DeliveryGoodInfo goodInfo = mock(DeliveryGoodInfo.class);
            given(goodInfo.getName()).willReturn("노트북");
            given(delivery.getDeliveryGoodInfo()).willReturn(goodInfo);

            ChatMessage lastMsg = mock(ChatMessage.class);
            given(lastMsg.getContent()).willReturn("도착했어요");
            given(lastMsg.getCreatedAt()).willReturn(LocalDateTime.now());

            given(chatRoomRepository.findAllActiveByAccount(eq(sender), any()))
                    .willReturn(List.of(chatRoom(100L, delivery)));
            given(chatMessageRepository.findTopByDelivery_IdOrderByCreatedAtDesc(1L)).willReturn(Optional.of(lastMsg));
            given(chatMessageRepository.countByDelivery_IdAndSender_IdNotAndIsReadFalse(1L, 1L)).willReturn(0L);

            List<ChatRoomListItemResponseDto> result = chatService.getChatRoomList(sender);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).partner().id()).isEqualTo(2L);
            assertThat(result.get(0).partner().nickname()).isEqualTo("shipper닉네임");
            assertThat(result.get(0).itemName()).isEqualTo("노트북");
            assertThat(result.get(0).lastMessage()).isEqualTo("도착했어요");
            assertThat(result.get(0).unreadCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("shipper로 참여한 경우 partner는 sender")
        void asShipper_partnerIsSender() {
            given(chatRoomRepository.findAllActiveByAccount(eq(shipper), any()))
                    .willReturn(List.of(chatRoom(100L, delivery)));
            given(chatMessageRepository.findTopByDelivery_IdOrderByCreatedAtDesc(1L)).willReturn(Optional.empty());
            given(chatMessageRepository.countByDelivery_IdAndSender_IdNotAndIsReadFalse(1L, 2L)).willReturn(0L);

            List<ChatRoomListItemResponseDto> result = chatService.getChatRoomList(shipper);

            assertThat(result.get(0).partner().id()).isEqualTo(1L);
            assertThat(result.get(0).partner().nickname()).isEqualTo("sender닉네임");
        }

        @Test
        @DisplayName("메시지 없는 채팅방은 lastMessage와 lastMessageAt이 null")
        void noMessages_lastMessageIsNull() {
            given(chatRoomRepository.findAllActiveByAccount(eq(sender), any()))
                    .willReturn(List.of(chatRoom(100L, delivery)));
            given(chatMessageRepository.findTopByDelivery_IdOrderByCreatedAtDesc(1L)).willReturn(Optional.empty());
            given(chatMessageRepository.countByDelivery_IdAndSender_IdNotAndIsReadFalse(1L, 1L)).willReturn(0L);

            List<ChatRoomListItemResponseDto> result = chatService.getChatRoomList(sender);

            assertThat(result.get(0).lastMessage()).isNull();
            assertThat(result.get(0).lastMessageAt()).isNull();
        }

        @Test
        @DisplayName("안읽은 메시지 수가 정확히 반영된다")
        void unreadCountIsReflected() {
            ChatMessage lastMsg = mock(ChatMessage.class);
            given(lastMsg.getContent()).willReturn("읽어주세요");
            given(lastMsg.getCreatedAt()).willReturn(LocalDateTime.now());

            given(chatRoomRepository.findAllActiveByAccount(eq(sender), any()))
                    .willReturn(List.of(chatRoom(100L, delivery)));
            given(chatMessageRepository.findTopByDelivery_IdOrderByCreatedAtDesc(1L)).willReturn(Optional.of(lastMsg));
            given(chatMessageRepository.countByDelivery_IdAndSender_IdNotAndIsReadFalse(1L, 1L)).willReturn(3L);

            List<ChatRoomListItemResponseDto> result = chatService.getChatRoomList(sender);

            assertThat(result.get(0).unreadCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("최근 메시지 순 내림차순으로 정렬된다")
        void sortedByLastMessageAtDesc() {
            Delivery d1 = mock(Delivery.class);
            Delivery d2 = mock(Delivery.class);
            given(d1.getId()).willReturn(10L);
            given(d1.getSender()).willReturn(sender);
            given(d1.getShipper()).willReturn(shipper);
            given(d2.getId()).willReturn(20L);
            given(d2.getSender()).willReturn(sender);
            given(d2.getShipper()).willReturn(shipper);

            LocalDateTime older = LocalDateTime.now().minusHours(2);
            LocalDateTime newer = LocalDateTime.now();

            ChatMessage msg1 = mock(ChatMessage.class);
            given(msg1.getContent()).willReturn("오래된 메시지");
            given(msg1.getCreatedAt()).willReturn(older);

            ChatMessage msg2 = mock(ChatMessage.class);
            given(msg2.getContent()).willReturn("최신 메시지");
            given(msg2.getCreatedAt()).willReturn(newer);

            given(chatRoomRepository.findAllActiveByAccount(eq(sender), any()))
                    .willReturn(List.of(chatRoom(100L, d1), chatRoom(200L, d2)));
            given(chatMessageRepository.findTopByDelivery_IdOrderByCreatedAtDesc(10L)).willReturn(Optional.of(msg1));
            given(chatMessageRepository.findTopByDelivery_IdOrderByCreatedAtDesc(20L)).willReturn(Optional.of(msg2));
            given(chatMessageRepository.countByDelivery_IdAndSender_IdNotAndIsReadFalse(anyLong(), eq(1L))).willReturn(0L);

            List<ChatRoomListItemResponseDto> result = chatService.getChatRoomList(sender);

            assertThat(result.get(0).deliveryId()).isEqualTo(20L);
            assertThat(result.get(1).deliveryId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("메시지 없는 채팅방은 메시지 있는 채팅방보다 뒤에 정렬된다")
        void noMessageRoom_sortedLast() {
            Delivery d1 = mock(Delivery.class); // 메시지 있음
            Delivery d2 = mock(Delivery.class); // 메시지 없음
            given(d1.getId()).willReturn(10L);
            given(d1.getSender()).willReturn(sender);
            given(d1.getShipper()).willReturn(shipper);
            given(d2.getId()).willReturn(20L);
            given(d2.getSender()).willReturn(sender);
            given(d2.getShipper()).willReturn(shipper);

            ChatMessage msg = mock(ChatMessage.class);
            given(msg.getContent()).willReturn("안녕하세요");
            given(msg.getCreatedAt()).willReturn(LocalDateTime.now());

            given(chatRoomRepository.findAllActiveByAccount(eq(sender), any()))
                    .willReturn(List.of(chatRoom(200L, d2), chatRoom(100L, d1)));
            given(chatMessageRepository.findTopByDelivery_IdOrderByCreatedAtDesc(10L)).willReturn(Optional.of(msg));
            given(chatMessageRepository.findTopByDelivery_IdOrderByCreatedAtDesc(20L)).willReturn(Optional.empty());
            given(chatMessageRepository.countByDelivery_IdAndSender_IdNotAndIsReadFalse(anyLong(), eq(1L))).willReturn(0L);

            List<ChatRoomListItemResponseDto> result = chatService.getChatRoomList(sender);

            assertThat(result.get(0).deliveryId()).isEqualTo(10L);
            assertThat(result.get(1).deliveryId()).isEqualTo(20L);
        }

        @Test
        @DisplayName("참여 중인 채팅방이 없으면 빈 목록 반환")
        void noChatRooms_returnsEmpty() {
            given(chatRoomRepository.findAllActiveByAccount(eq(sender), any())).willReturn(List.of());

            List<ChatRoomListItemResponseDto> result = chatService.getChatRoomList(sender);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("deliveryGoodInfo가 없으면 itemName은 null")
        void noGoodInfo_itemNameIsNull() {
            given(delivery.getDeliveryGoodInfo()).willReturn(null);
            given(chatRoomRepository.findAllActiveByAccount(eq(sender), any()))
                    .willReturn(List.of(chatRoom(100L, delivery)));
            given(chatMessageRepository.findTopByDelivery_IdOrderByCreatedAtDesc(1L)).willReturn(Optional.empty());
            given(chatMessageRepository.countByDelivery_IdAndSender_IdNotAndIsReadFalse(1L, 1L)).willReturn(0L);

            List<ChatRoomListItemResponseDto> result = chatService.getChatRoomList(sender);

            assertThat(result.get(0).itemName()).isNull();
        }
    }

    // ──────────────────────────────────────────────
    // 접근 권한 검증
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("접근 권한 검증")
    class AccessValidation {

        @Test
        @DisplayName("WAIT 상태 배송건 접근 시 CHAT_NOT_AVAILABLE 예외 발생")
        void waitStatus_throwsChatNotAvailable() {
            given(delivery.getStatus()).willReturn(DeliveryState.WAIT);
            given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery));

            assertThatThrownBy(() -> chatService.getMessages(1L, sender))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("code", ChatErrorCode.CHAT_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("CANCEL 상태 배송건 접근 시 CHAT_NOT_AVAILABLE 예외 발생")
        void cancelStatus_throwsChatNotAvailable() {
            given(delivery.getStatus()).willReturn(DeliveryState.CANCEL);
            given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery));

            assertThatThrownBy(() -> chatService.getMessages(1L, sender))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("code", ChatErrorCode.CHAT_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("sender도 shipper도 아닌 유저 접근 시 FORBIDDEN_ACCESS 예외 발생")
        void outsider_throwsForbiddenAccess() {
            given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery));

            assertThatThrownBy(() -> chatService.getMessages(1L, outsider))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("code", ChatErrorCode.FORBIDDEN_ACCESS);
        }

        @Test
        @DisplayName("존재하지 않는 deliveryId 접근 시 DELIVERY_NOT_FOUND 예외 발생")
        void deliveryNotFound_throwsException() {
            given(deliveryRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.getMessages(999L, sender))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("code", ChatErrorCode.DELIVERY_NOT_FOUND);
        }
    }

    // ──────────────────────────────────────────────
    // 메시지 조회
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("메시지 조회")
    class GetMessages {

        @Test
        @DisplayName("전체 메시지 조회 시 상대방 메시지 읽음 처리 호출")
        void getMessages_marksAsRead() {
            given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery));
            given(chatMessageRepository.findAllResponseByDeliveryIdOrderByCreatedAtAsc(1L)).willReturn(List.of());

            chatService.getMessages(1L, sender);

            then(chatMessageRepository).should().markAllAsRead(1L, sender.getId());
        }

        @Test
        @DisplayName("polling 조회 결과가 비어있으면 읽음 처리하지 않음")
        void getMessagesAfter_doesNotMarkAsReadWhenNoNewMessages() {
            given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery));
            given(chatMessageRepository.findAllResponseByDeliveryIdAndIdGreaterThanOrderByIdAsc(1L, 3L))
                    .willReturn(List.of());

            chatService.getMessagesAfter(1L, 3L, sender);

            then(chatMessageRepository).should(never()).markAllAsRead(anyLong(), anyLong());
        }

        @Test
        @DisplayName("polling 조회 결과에 상대방 unread 메시지가 있으면 읽음 처리 호출")
        void getMessagesAfter_marksAsReadWhenUnreadPartnerMessageExists() {
            ChatMessageResponseDto unreadPartnerMessage = new ChatMessageResponseDto(
                    4L,
                    shipper.getId(),
                    "shipper닉네임",
                    "새 메시지",
                    false,
                    LocalDateTime.now());
            given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery));
            given(chatMessageRepository.findAllResponseByDeliveryIdAndIdGreaterThanOrderByIdAsc(1L, 3L))
                    .willReturn(List.of(unreadPartnerMessage));

            List<ChatMessageResponseDto> result = chatService.getMessagesAfter(1L, 3L, sender);

            then(chatMessageRepository).should().markAllAsRead(1L, sender.getId());
            assertThat(result.get(0).isRead()).isTrue();
        }

        @Test
        @DisplayName("전체 메시지 조회 결과 반환")
        void getMessages_returnsMappedDtos() {
            ChatMessageResponseDto message = new ChatMessageResponseDto(
                    1L,
                    sender.getId(),
                    "sender닉네임",
                    "안녕하세요",
                    false,
                    LocalDateTime.now());

            given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery));
            given(chatMessageRepository.findAllResponseByDeliveryIdOrderByCreatedAtAsc(1L)).willReturn(List.of(message));

            List<ChatMessageResponseDto> result = chatService.getMessages(1L, sender);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).content()).isEqualTo("안녕하세요");
        }
    }

    // ──────────────────────────────────────────────
    // 메시지 전송
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("메시지 전송")
    class SendMessage {

        @Test
        @DisplayName("메시지 저장 후 DTO 반환")
        void sendMessage_savesAndReturnsDto() {
            ChatMessage saved = mock(ChatMessage.class);
            given(saved.getId()).willReturn(1L);
            given(saved.getSender()).willReturn(sender);
            given(saved.getContent()).willReturn("테스트 메시지");

            ChatRoom chatRoom = ChatRoom.builder().id(10L).delivery(delivery).build();
            given(deliveryRepository.findByIdForUpdate(1L)).willReturn(Optional.of(delivery));
            given(chatRoomRepository.findByDeliveryId(1L)).willReturn(Optional.empty());
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(chatRoom);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(saved);

            ChatMessageSendResponseDto result = chatService.sendMessage(
                    1L, new ChatMessageRequestDto("테스트 메시지"), sender);

            assertThat(result.chatRoom().id()).isEqualTo(10L);
            assertThat(result.chatRoom().deliveryId()).isEqualTo(1L);
            assertThat(result.content()).isEqualTo("테스트 메시지");
            then(chatRoomRepository).should().save(any(ChatRoom.class));
            ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
            then(chatMessageRepository).should().save(messageCaptor.capture());
            assertThat(messageCaptor.getValue().isSystemMessage()).isFalse();
        }

        @Test
        @DisplayName("기존 채팅방이 있으면 새로 생성하지 않고 재사용")
        void sendMessage_reusesExistingChatRoom() {
            ChatRoom existingRoom = chatRoom(10L, delivery);
            ChatMessage saved = mock(ChatMessage.class);
            given(saved.getId()).willReturn(2L);
            given(saved.getSender()).willReturn(sender);
            given(saved.getContent()).willReturn("두 번째 메시지");
            given(deliveryRepository.findByIdForUpdate(1L)).willReturn(Optional.of(delivery));
            given(chatRoomRepository.findByDeliveryId(1L)).willReturn(Optional.of(existingRoom));
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(saved);

            ChatMessageSendResponseDto result = chatService.sendMessage(
                    1L, new ChatMessageRequestDto("두 번째 메시지"), sender);

            assertThat(result.chatRoom().id()).isEqualTo(10L);
            assertThat(result.content()).isEqualTo("두 번째 메시지");
            then(chatRoomRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("이미지 키가 있으면 업로드 검증 후 메시지에 저장")
        void sendMessage_withImageKey_validatesAndSavesImageKey() {
            String imageKey = "uploads/images/chat-image.png";
            ChatRoom existingRoom = chatRoom(10L, delivery);
            ChatMessage saved = mock(ChatMessage.class);
            given(saved.getId()).willReturn(3L);
            given(saved.getSender()).willReturn(sender);
            given(saved.getContent()).willReturn("이미지 첨부");
            given(saved.getImageKey()).willReturn(imageKey);
            given(deliveryRepository.findByIdForUpdate(1L)).willReturn(Optional.of(delivery));
            given(chatRoomRepository.findByDeliveryId(1L)).willReturn(Optional.of(existingRoom));
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(saved);

            ChatMessageSendResponseDto result = chatService.sendMessage(
                    1L, new ChatMessageRequestDto("이미지 첨부", imageKey), sender);

            ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
            then(s3Service).should().validateUploadedImage(imageKey);
            then(chatMessageRepository).should().save(messageCaptor.capture());
            assertThat(messageCaptor.getValue().getImageKey()).isEqualTo(imageKey);
            assertThat(result.imageKey()).isEqualTo(imageKey);
        }

        @Test
        @DisplayName("권한 없는 유저는 메시지 전송 불가")
        void sendMessage_outsider_throwsForbiddenAccess() {
            given(deliveryRepository.findByIdForUpdate(1L)).willReturn(Optional.of(delivery));

            assertThatThrownBy(() -> chatService.sendMessage(1L, new ChatMessageRequestDto("테스트"), outsider))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("code", ChatErrorCode.FORBIDDEN_ACCESS);

            then(chatMessageRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("배송 상태 이미지 전송")
    class SendDeliveryStatusMessage {

        @Test
        @DisplayName("채팅방이 없으면 생성하고 확정된 이미지 키로 메시지 저장")
        void createsRoomAndSavesImageMessage() {
            String imageKey = "delivery-images/pickup-final.png";
            given(chatRoomRepository.findByDeliveryId(1L)).willReturn(Optional.empty());
            given(chatRoomRepository.save(any(ChatRoom.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            chatService.sendDeliveryStatusMessage(
                    delivery,
                    shipper,
                    "전달자가 물품을 인수했어요!",
                    imageKey);

            ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
            then(chatRoomRepository).should().save(any(ChatRoom.class));
            then(chatMessageRepository).should().save(messageCaptor.capture());
            assertThat(messageCaptor.getValue().getDelivery()).isEqualTo(delivery);
            assertThat(messageCaptor.getValue().getSender()).isEqualTo(shipper);
            assertThat(messageCaptor.getValue().getContent()).isEqualTo("전달자가 물품을 인수했어요!");
            assertThat(messageCaptor.getValue().getImageKey()).isEqualTo(imageKey);
            assertThat(messageCaptor.getValue().isSystemMessage()).isTrue();
            then(s3Service).should(never()).validateUploadedImage(any());
        }

        @Test
        @DisplayName("이미지가 없어도 상태 메시지 저장")
        void savesStatusMessageWithoutImage() {
            ChatRoom existingRoom = chatRoom(10L, delivery);
            given(chatRoomRepository.findByDeliveryId(1L)).willReturn(Optional.of(existingRoom));

            chatService.sendDeliveryStatusMessage(
                    delivery,
                    shipper,
                    "전달자가 물품 전달을 완료했어요!",
                    null);

            ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
            then(chatMessageRepository).should().save(messageCaptor.capture());
            assertThat(messageCaptor.getValue().getContent())
                    .isEqualTo("전달자가 물품 전달을 완료했어요!");
            assertThat(messageCaptor.getValue().getImageKey()).isNull();
            assertThat(messageCaptor.getValue().isSystemMessage()).isTrue();
        }
    }

    @Nested
    @DisplayName("채팅방 나가기")
    class LeaveChatRoom {

        @Test
        @DisplayName("sender가 나가면 sender 퇴장 상태를 저장")
        void senderLeaves_savesLeftState() {
            ChatRoom chatRoom = chatRoom(10L, delivery);
            given(deliveryRepository.findByIdForUpdate(1L)).willReturn(Optional.of(delivery));
            given(chatRoomRepository.findByDeliveryId(1L)).willReturn(Optional.of(chatRoom));

            chatService.leaveChatRoom(1L, sender);

            assertThat(chatRoom.hasLeft(sender.getId())).isTrue();
            assertThat(chatRoom.hasLeft(shipper.getId())).isFalse();
            then(chatRoomRepository).should().save(chatRoom);
        }

        @Test
        @DisplayName("채팅방이 없으면 CHAT_ROOM_NOT_FOUND 예외 발생")
        void roomNotFound_throwsException() {
            given(deliveryRepository.findByIdForUpdate(1L)).willReturn(Optional.of(delivery));
            given(chatRoomRepository.findByDeliveryId(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.leaveChatRoom(1L, sender))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("code", ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("나간 사용자는 메시지를 다시 전송할 수 없음")
        void leftAccount_cannotSendMessage() {
            ChatRoom chatRoom = chatRoom(10L, delivery);
            chatRoom.leave(sender.getId());
            given(deliveryRepository.findByIdForUpdate(1L)).willReturn(Optional.of(delivery));
            given(chatRoomRepository.findByDeliveryId(1L)).willReturn(Optional.of(chatRoom));

            assertThatThrownBy(() -> chatService.sendMessage(
                    1L, new ChatMessageRequestDto("재입장 시도"), sender))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("code", ChatErrorCode.CHAT_ROOM_ALREADY_LEFT);

            then(chatMessageRepository).should(never()).save(any());
        }
    }

    // ──────────────────────────────────────────────
    // 채팅방 헤더 정보
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("채팅방 헤더 정보 조회")
    class GetChatRoomInfo {

        @Test
        @DisplayName("sender로 조회 시 partner는 shipper")
        void senderRequest_partnerIsShipper() {
            Place origin = mock(Place.class);
            Place dest = mock(Place.class);
            given(origin.getSubwayStationName()).willReturn("서울 강남구");
            given(dest.getSubwayStationName()).willReturn("부산 해운대구");
            given(delivery.getOrigin()).willReturn(origin);
            given(delivery.getDest()).willReturn(dest);

            given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery));

            ChatRoomInfoResponseDto result = chatService.getChatRoomInfo(1L, sender);

            assertThat(result.partnerNickname()).isEqualTo("shipper닉네임");
            assertThat(result.partnerPicture()).isEqualTo("shipper.jpg");
        }

        @Test
        @DisplayName("shipper로 조회 시 partner는 sender")
        void shipperRequest_partnerIsSender() {
            Place origin = mock(Place.class);
            Place dest = mock(Place.class);
            given(origin.getSubwayStationName()).willReturn("서울 강남구");
            given(dest.getSubwayStationName()).willReturn("부산 해운대구");
            given(delivery.getOrigin()).willReturn(origin);
            given(delivery.getDest()).willReturn(dest);

            given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery));
            ChatRoomInfoResponseDto result = chatService.getChatRoomInfo(1L, shipper);

            assertThat(result.partnerNickname()).isEqualTo("sender닉네임");
            assertThat(result.partnerPicture()).isEqualTo("sender.jpg");
        }

        @Test
        @DisplayName("Delivery name을 itemName으로 반환")
        void deliveryName_returnsItemName() {
            Place origin = mock(Place.class);
            Place dest = mock(Place.class);
            given(origin.getSubwayStationName()).willReturn("서울");
            given(dest.getSubwayStationName()).willReturn("부산");
            given(delivery.getOrigin()).willReturn(origin);
            given(delivery.getDest()).willReturn(dest);

            DeliveryGoodInfo goodInfo = mock(DeliveryGoodInfo.class);
            given(delivery.getDeliveryGoodInfo()).willReturn(goodInfo);
            given(goodInfo.getName()).willReturn("노트북");

            given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery));

            ChatRoomInfoResponseDto result = chatService.getChatRoomInfo(1L, sender);

            assertThat(result.itemName()).isEqualTo("노트북");
        }

        @Test
        @DisplayName("Delivery name이 없으면 itemName은 null")
        void withoutDeliveryName_itemNameIsNull() {
            Place origin = mock(Place.class);
            Place dest = mock(Place.class);
            given(origin.getSubwayStationName()).willReturn("서울");
            given(dest.getSubwayStationName()).willReturn("부산");
            given(delivery.getOrigin()).willReturn(origin);
            given(delivery.getDest()).willReturn(dest);

            given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery));

            ChatRoomInfoResponseDto result = chatService.getChatRoomInfo(1L, sender);

            assertThat(result.itemName()).isNull();
        }
    }

    // ──────────────────────────────────────────────
    // 안읽은 메시지 수
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("안읽은 메시지 수 조회")
    class GetUnreadCount {

        @Test
        @DisplayName("안읽은 메시지 수 반환")
        void returnsUnreadCount() {
            given(deliveryRepository.findById(1L)).willReturn(Optional.of(delivery));
            given(chatMessageRepository.countByDelivery_IdAndSender_IdNotAndIsReadFalse(1L, sender.getId())).willReturn(3L);

            long count = chatService.getUnreadCount(1L, sender);

            assertThat(count).isEqualTo(3L);
        }
    }

    private ChatRoom chatRoom(Long id, Delivery roomDelivery) {
        return ChatRoom.builder()
                .id(id)
                .delivery(roomDelivery)
                .build();
    }
}
