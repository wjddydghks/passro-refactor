package com.passro.passrobackend.account.controller;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.exception.code.AccountSuccessCode;
import com.passro.passrobackend.global.response.APIResponse;
import com.passro.passrobackend.point.dto.PointHistoryResponseDto;
import com.passro.passrobackend.point.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/account")
@Tag(name = "포인트", description = "로그인한 사용자의 포인트 조회 API")
public class PointController {

    private final PointService pointService;

    @GetMapping("/points")
    @Operation(summary = "포인트 내역 조회", description = "현재 포인트와 포인트 증감 내역을 최신순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "포인트 내역 조회 성공", useReturnTypeSchema = true)
    public APIResponse<PointHistoryResponseDto> getPointHistory(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "account") Account account
    ) {
        return APIResponse.onSuccess(
                AccountSuccessCode.OK,
                pointService.getPointHistory(account.getId())
        );
    }
}
