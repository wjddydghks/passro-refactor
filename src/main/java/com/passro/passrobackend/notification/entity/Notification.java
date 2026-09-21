package com.passro.passrobackend.notification.entity;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.global.entity.BaseEntity;
import com.passro.passrobackend.notification.enums.NotificationType;
import com.passro.passrobackend.notification.enums.ResourceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 알림 수신자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // 알림 종류
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    // 클릭 시 이동할 자원 종류 (NONE 이면 이동 없음)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType resourceType;

    // 자원 ID (resourceType 이 NONE 이면 NULL)
    private Long resourceId;

    // 확인 여부
    @Column(nullable = false)
    private boolean isRead;

    // 확인 시각
    private LocalDateTime readAt;

    // 확인 처리
    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }
}
