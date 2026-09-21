package com.passro.passrobackend.notification.controller;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.global.response.APIResponse;
import com.passro.passrobackend.notification.code.NotificationErrorCode;
import com.passro.passrobackend.notification.code.NotificationSuccessCode;
import com.passro.passrobackend.notification.dto.NotificationResponseDto;
import com.passro.passrobackend.notification.dto.UnreadCountResponseDto;
import com.passro.passrobackend.notification.exception.NotificationException;
import com.passro.passrobackend.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "알림", description = "인앱 알림 조회/확인/삭제 API")
public class NotificationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "내 알림 목록 조회", description = "로그인한 사용자의 알림 목록을 최신순으로 조회합니다.")
    public APIResponse<Page<NotificationResponseDto>> getMyNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePagination(page, size);
        return APIResponse.onSuccess(NotificationSuccessCode.OK,
                notificationService.getMyNotifications(account, page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "미확인 알림 수 조회", description = "로그인한 사용자의 아직 확인하지 않은 알림 수를 반환합니다.")
    public APIResponse<UnreadCountResponseDto> getUnreadCount(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account) {
        return APIResponse.onSuccess(NotificationSuccessCode.UNREAD_COUNT_OK,
                notificationService.getUnreadCount(account));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "알림 확인 처리", description = "개별 알림을 확인 상태로 변경합니다.")
    public APIResponse<NotificationResponseDto> markAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "알림 ID", example = "1") @PathVariable Long id) {
        return APIResponse.onSuccess(NotificationSuccessCode.READ_OK,
                notificationService.markAsRead(account, id));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "전체 알림 확인 처리", description = "로그인한 사용자의 미확인 알림을 모두 확인 상태로 변경합니다.")
    public APIResponse<Long> markAllAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account) {
        long updatedCount = notificationService.markAllAsRead(account);
        return APIResponse.onSuccess(NotificationSuccessCode.READ_ALL_OK, updatedCount);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "알림 삭제", description = "개별 알림을 삭제합니다.")
    public APIResponse<Void> deleteNotification(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account,
            @Parameter(description = "알림 ID", example = "1") @PathVariable Long id) {
        notificationService.deleteNotification(account, id);
        return APIResponse.onSuccess(NotificationSuccessCode.DELETED, null);
    }

    @DeleteMapping
    @Operation(summary = "전체 알림 삭제", description = "로그인한 사용자의 모든 알림을 삭제합니다.")
    public APIResponse<Long> deleteAllNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "account") Account account) {
        long deletedCount = notificationService.deleteAllNotifications(account);
        return APIResponse.onSuccess(NotificationSuccessCode.DELETED_ALL, deletedCount);
    }

    // 페이지네이션 요청 값 검증
    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new NotificationException(NotificationErrorCode.INVALID_PAGINATION);
        }
    }
}
