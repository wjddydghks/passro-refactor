package com.passro.passrobackend.notification.repository;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 특정 계정의 알림 목록 (최신순, 페이지네이션)
    Page<Notification> findAllByAccountOrderByCreatedAtDesc(Account account, Pageable pageable);

    // 특정 계정의 미확인 알림 수
    long countByAccountAndIsReadFalse(Account account);

    // 특정 계정의 미확인 알림을 모두 확인 처리 (bulk update, 반환값 = 처리된 개수)
    @Modifying
    @Query("update Notification n set n.isRead = true, n.readAt = :readAt " +
            "where n.account = :account and n.isRead = false")
    long markAllAsReadByAccount(@Param("account") Account account,
                                @Param("readAt") LocalDateTime readAt);

    // 특정 계정의 알림 전체 삭제 (반환값 = 삭제된 개수)
    @Modifying
    @Query("delete from Notification n where n.account = :account")
    long deleteAllByAccount(@Param("account") Account account);
}
