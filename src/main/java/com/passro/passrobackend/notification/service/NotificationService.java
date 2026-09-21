package com.passro.passrobackend.notification.service;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.notification.code.NotificationErrorCode;
import com.passro.passrobackend.notification.dto.NotificationResponseDto;
import com.passro.passrobackend.notification.dto.UnreadCountResponseDto;
import com.passro.passrobackend.notification.entity.Notification;
import com.passro.passrobackend.notification.enums.NotificationType;
import com.passro.passrobackend.notification.enums.ResourceType;
import com.passro.passrobackend.notification.exception.NotificationException;
import com.passro.passrobackend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 알림 발행 (다른 도메인 서비스가 호출)
     */
    @Transactional
    public Notification publish(Account recipient,
                                NotificationType type,
                                String title,
                                String content,
                                ResourceType resourceType,
                                Long resourceId) {
        ResourceType effectiveResourceType =
                resourceType != null ? resourceType : ResourceType.NONE;
        Long effectiveResourceId =
                effectiveResourceType == ResourceType.NONE ? null : resourceId;

        Notification notification = Notification.builder()
                .account(recipient)
                .type(type)
                .title(title)
                .content(content)
                .resourceType(effectiveResourceType)
                .resourceId(effectiveResourceId)
                .isRead(false)
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getMyNotifications(Account account, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findAllByAccountOrderByCreatedAtDesc(account, pageable)
                .map(NotificationResponseDto::fromNotification);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponseDto getUnreadCount(Account account) {
        long count = notificationRepository.countByAccountAndIsReadFalse(account);
        return UnreadCountResponseDto.builder().unreadCount(count).build();
    }

    @Transactional
    public NotificationResponseDto markAsRead(Account account, Long notificationId) {
        Notification notification = findOwnedNotification(account, notificationId);
        notification.markAsRead();
        return NotificationResponseDto.fromNotification(notification);
    }

    /**
     * 내 모든 미확인 알림을 확인 처리 (bulk update, 반환값 = 처리된 개수)
     * 엔티티를 로드하지 않고 DB 레벨에서 한 번에 처리하여 메모리 사용량과 성능 최적화
     */
    @Transactional
    public long markAllAsRead(Account account) {
        return notificationRepository.markAllAsReadByAccount(account, LocalDateTime.now());
    }

    @Transactional
    public void deleteNotification(Account account, Long notificationId) {
        Notification notification = findOwnedNotification(account, notificationId);
        notificationRepository.delete(notification);
    }

    /**
     * 내 모든 알림 삭제 (반환값 = 삭제된 개수)
     */
    @Transactional
    public long deleteAllNotifications(Account account) {
        return notificationRepository.deleteAllByAccount(account);
    }

    // 본인 소유 확인 헬퍼
    private Notification findOwnedNotification(Account account, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOT_FOUND));
        if (!notification.getAccount().getId().equals(account.getId())) {
            throw new NotificationException(NotificationErrorCode.FORBIDDEN);
        }
        return notification;
    }
}
