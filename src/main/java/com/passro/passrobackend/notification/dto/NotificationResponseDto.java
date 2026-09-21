package com.passro.passrobackend.notification.dto;

import com.passro.passrobackend.notification.entity.Notification;
import com.passro.passrobackend.notification.enums.NotificationType;
import com.passro.passrobackend.notification.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(types = "object", description = "알림 응답 DTO")
public class NotificationResponseDto {

    @Schema(description = "알림 ID", example = "1")
    private Long notificationId;

    @Schema(description = "알림 종류 (GENERAL, DELIVERY)", example = "DELIVERY")
    private NotificationType type;

    @Schema(description = "알림 제목", example = "매칭 완료")
    private String title;

    @Schema(description = "알림 내용", example = "발송 요청이 매칭되었습니다.")
    private String content;

    @Schema(description = "이동할 자원 종류 (NONE, DELIVERY)", example = "DELIVERY")
    private ResourceType resourceType;

    @Schema(description = "자원 ID (resourceType 이 NONE 이면 null)", example = "123")
    private Long resourceId;

    @Schema(description = "확인 여부", example = "false")
    private boolean isRead;

    @Schema(description = "확인 시각 (미확인 시 null)", example = "2026-08-07T10:15:00")
    private LocalDateTime readAt;

    @Schema(description = "생성 시각", example = "2026-08-07T10:00:00")
    private LocalDateTime createdAt;

    public static NotificationResponseDto fromNotification(Notification n) {
        return NotificationResponseDto.builder()
                .notificationId(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .resourceType(n.getResourceType())
                .resourceId(n.getResourceId())
                .isRead(n.isRead())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
