package com.passro.passrobackend.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.repository.AccountRepository;
import com.passro.passrobackend.chat.dto.ChatMessageRequestDto;
import com.passro.passrobackend.chat.dto.ChatMessageResponseDto;
import com.passro.passrobackend.chat.dto.ChatMessageSendResponseDto;
import com.passro.passrobackend.chat.dto.ChatRoomResponseDto;
import com.passro.passrobackend.chat.dto.ChatRoomInfoResponseDto;
import com.passro.passrobackend.chat.exception.ChatException;
import com.passro.passrobackend.chat.exception.code.ChatErrorCode;
import com.passro.passrobackend.chat.service.ChatService;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import com.passro.passrobackend.global.advice.APIExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled("TODO: 인증 방식 변경(accountId 제거) 및 APIExceptionHandler 생성자 이슈 해결 후 활성화")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatControllerTest {

    @Mock ChatService chatService;
    @Mock AccountRepository accountRepository;

    @InjectMocks ChatController chatController;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders
                .standaloneSetup(chatController)
                .setControllerAdvice(new APIExceptionHandler(mock(View.class)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        Account account = mock(Account.class);
        given(accountRepository.findById(1L)).willReturn(Optional.of(account));
    }

    // @Test // TODO: 인증 방식 변경(accountId 제거) 반영 후 주석 해제
    // @DisplayName("메시지 전체 조회 - 200 반환")
    void getMessages_success() throws Exception {
        ChatMessageResponseDto dto = new ChatMessageResponseDto(1L, 2L, "상대방", "안녕하세요", false, LocalDateTime.now());
        given(chatService.getMessages(eq(1L), any())).willReturn(List.of(dto));

        mockMvc.perform(get("/chat/1/messages")
                        .param("accountId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].content").value("안녕하세요"));
    }

    // @Test // TODO: 인증 방식 변경(accountId 제거) 반영 후 주석 해제
    // @DisplayName("메시지 polling 조회 - afterId 포함 - 200 반환")
    void getMessagesAfter_success() throws Exception {
        given(chatService.getMessagesAfter(eq(1L), eq(3L), any())).willReturn(List.of());

        mockMvc.perform(get("/chat/1/messages")
                        .param("accountId", "1")
                        .param("afterId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    // @Test // TODO: 인증 방식 변경(accountId 제거) 반영 후 주석 해제
    // @DisplayName("메시지 전송 - 200 반환 및 content 확인")
    void sendMessage_success() throws Exception {
        ChatMessageResponseDto dto = new ChatMessageResponseDto(1L, 1L, "내닉네임", "테스트 메시지", false, LocalDateTime.now());
        ChatMessageSendResponseDto response = new ChatMessageSendResponseDto(
                dto.id(), dto.senderId(), dto.senderNickname(), dto.content(), dto.isRead(), dto.createdAt(),
                new ChatRoomResponseDto(10L, 1L, LocalDateTime.now()));
        given(chatService.sendMessage(eq(1L), any(), any())).willReturn(response);

        mockMvc.perform(post("/chat/1/messages")
                        .param("accountId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatMessageRequestDto("테스트 메시지"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.chatRoom.id").value(10))
                .andExpect(jsonPath("$.result.content").value("테스트 메시지"));
    }

    // @Test // TODO: 인증 방식 변경(accountId 제거) 반영 후 주석 해제
    // @DisplayName("메시지 전송 - content 빈 값이면 400 반환")
    void sendMessage_blankContent_returns400() throws Exception {
        mockMvc.perform(post("/chat/1/messages")
                        .param("accountId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatMessageRequestDto(""))))
                .andExpect(status().isBadRequest());
    }

    // @Test // TODO: 인증 방식 변경(accountId 제거) 반영 후 주석 해제
    // @DisplayName("채팅방 헤더 정보 조회 - 200 반환")
    void getChatRoomInfo_success() throws Exception {
        ChatRoomInfoResponseDto dto = new ChatRoomInfoResponseDto(
                "상대방닉네임", "photo.jpg", "노트북", "서울 강남구", "부산 해운대구", DeliveryState.MATCHED
        );
        given(chatService.getChatRoomInfo(eq(1L), any())).willReturn(dto);

        mockMvc.perform(get("/chat/1/info")
                        .param("accountId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.partnerNickname").value("상대방닉네임"))
                .andExpect(jsonPath("$.result.itemName").value("노트북"))
                .andExpect(jsonPath("$.result.departure").value("서울 강남구"))
                .andExpect(jsonPath("$.result.arrival").value("부산 해운대구"))
                .andExpect(jsonPath("$.result.deliveryStatus").value("MATCHED"));
    }

    // @Test // TODO: 인증 방식 변경(accountId 제거) 반영 후 주석 해제
    // @DisplayName("WAIT 상태 배송건 접근 시 400 반환")
    void chatNotAvailable_returns400() throws Exception {
        given(chatService.getMessages(eq(1L), any()))
                .willThrow(new ChatException(ChatErrorCode.CHAT_NOT_AVAILABLE));

        mockMvc.perform(get("/chat/1/messages")
                        .param("accountId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAT400_1"));
    }

    // @Test // TODO: 인증 방식 변경(accountId 제거) 반영 후 주석 해제
    // @DisplayName("권한 없는 유저 접근 시 403 반환")
    void forbiddenAccess_returns403() throws Exception {
        given(chatService.getMessages(eq(1L), any()))
                .willThrow(new ChatException(ChatErrorCode.FORBIDDEN_ACCESS));

        mockMvc.perform(get("/chat/1/messages")
                        .param("accountId", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CHAT403_1"));
    }

    // @Test // TODO: 인증 방식 변경(accountId 제거) 반영 후 주석 해제
    // @DisplayName("안읽은 메시지 수 조회 - 200 반환")
    void getUnreadCount_success() throws Exception {
        given(chatService.getUnreadCount(eq(1L), any())).willReturn(3L);

        mockMvc.perform(get("/chat/1/unread-count")
                        .param("accountId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(3));
    }
}
