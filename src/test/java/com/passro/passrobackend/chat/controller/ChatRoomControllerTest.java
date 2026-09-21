package com.passro.passrobackend.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.chat.dto.ChatPartnerDto;
import com.passro.passrobackend.chat.dto.ChatRoomListItemResponseDto;
import com.passro.passrobackend.chat.service.ChatService;
import com.passro.passrobackend.global.advice.APIExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.View;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatRoomControllerTest {

    @Mock ChatService chatService;
    @InjectMocks ChatRoomController chatRoomController;

    MockMvc mockMvc;
    ObjectMapper objectMapper;
    Account mockAccount;

    @BeforeEach
    void setUp() {
        mockAccount = mock(Account.class);

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders
                .standaloneSetup(chatRoomController)
                .setCustomArgumentResolvers(authPrincipalResolver(mockAccount))
                .setControllerAdvice(new APIExceptionHandler(mock(View.class)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    /**
     * @AuthenticationPrincipal(expression = "account") 를 standalone MockMvc에서 처리하기 위한
     * 커스텀 ArgumentResolver. 실제 SecurityContext 없이 mock Account를 주입한다.
     */
    private HandlerMethodArgumentResolver authPrincipalResolver(Account account) {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().isAssignableFrom(Account.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return account;
            }
        };
    }

    @Test
    @DisplayName("채팅방 목록 조회 - 200 반환 및 목록 확인")
    void getChatRoomList_success() throws Exception {
        ChatPartnerDto partner = new ChatPartnerDto(2L, "배달기사", "profile.jpg");
        LocalDateTime lastAt = LocalDateTime.of(2025, 1, 1, 12, 0);
        ChatRoomListItemResponseDto item = new ChatRoomListItemResponseDto(
                10L, partner, "노트북", "안녕하세요", lastAt, 2L
        );

        given(chatService.getChatRoomList(any())).willReturn(List.of(item));

        mockMvc.perform(get("/chat/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result[0].deliveryId").value(10))
                .andExpect(jsonPath("$.result[0].partner.id").value(2))
                .andExpect(jsonPath("$.result[0].partner.nickname").value("배달기사"))
                .andExpect(jsonPath("$.result[0].partner.picture").value("profile.jpg"))
                .andExpect(jsonPath("$.result[0].itemName").value("노트북"))
                .andExpect(jsonPath("$.result[0].lastMessage").value("안녕하세요"))
                .andExpect(jsonPath("$.result[0].unreadCount").value(2));
    }

    @Test
    @DisplayName("채팅방 목록 조회 - 참여 중인 채팅방 없으면 빈 배열 반환")
    void getChatRoomList_empty() throws Exception {
        given(chatService.getChatRoomList(any())).willReturn(List.of());

        mockMvc.perform(get("/chat/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result").isEmpty());
    }

    @Test
    @DisplayName("채팅방 목록 조회 - 여러 채팅방 목록 반환")
    void getChatRoomList_multipleRooms() throws Exception {
        ChatPartnerDto partner1 = new ChatPartnerDto(2L, "배달기사A", null);
        ChatPartnerDto partner2 = new ChatPartnerDto(3L, "배달기사B", null);

        List<ChatRoomListItemResponseDto> items = List.of(
                new ChatRoomListItemResponseDto(10L, partner1, "노트북", "최신 메시지", LocalDateTime.now(), 1L),
                new ChatRoomListItemResponseDto(20L, partner2, "의자", "오래된 메시지", LocalDateTime.now().minusHours(1), 0L)
        );

        given(chatService.getChatRoomList(any())).willReturn(items);

        mockMvc.perform(get("/chat/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(2))
                .andExpect(jsonPath("$.result[0].deliveryId").value(10))
                .andExpect(jsonPath("$.result[1].deliveryId").value(20));
    }

    @Test
    @DisplayName("채팅방 목록 조회 - lastMessage와 itemName이 null이어도 200 반환")
    void getChatRoomList_nullFields_stillReturns200() throws Exception {
        ChatPartnerDto partner = new ChatPartnerDto(2L, "상대방", null);
        ChatRoomListItemResponseDto item = new ChatRoomListItemResponseDto(
                10L, partner, null, null, null, 0L
        );

        given(chatService.getChatRoomList(any())).willReturn(List.of(item));

        mockMvc.perform(get("/chat/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].itemName").doesNotExist())
                .andExpect(jsonPath("$.result[0].lastMessage").doesNotExist())
                .andExpect(jsonPath("$.result[0].lastMessageAt").doesNotExist())
                .andExpect(jsonPath("$.result[0].unreadCount").value(0));
    }
}