package com.passro.passrobackend.account.controller;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.global.response.APIResponse;
import com.passro.passrobackend.point.dto.PointHistoryResponseDto;
import com.passro.passrobackend.point.dto.PointDeliveryResponseDto;
import com.passro.passrobackend.point.dto.PointLogResponseDto;
import com.passro.passrobackend.point.enums.PointIncrementReason;
import com.passro.passrobackend.point.service.PointService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointControllerTest {

    @Mock
    private PointService pointService;

    @InjectMocks
    private PointController pointController;

    @Test
    void returnsPointHistoryForAuthenticatedAccount() {
        Account account = Account.builder().id(1L).build();
        PointLogResponseDto pointLog = PointLogResponseDto.builder()
                .delivery(PointDeliveryResponseDto.builder().id(100L).build())
                .incrementReason(PointIncrementReason.DELIVERY_PAYMENT)
                .deltaPoint(-1800L)
                .beforePoint(5000L)
                .afterPoint(3200L)
                .build();
        PointHistoryResponseDto history = PointHistoryResponseDto.builder()
                .currentPoint(3200L)
                .pointLogs(List.of(pointLog))
                .build();
        given(pointService.getPointHistory(1L)).willReturn(history);

        APIResponse<PointHistoryResponseDto> response = pointController.getPointHistory(account);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getResult()).isSameAs(history);
        assertThat(response.getResult().getCurrentPoint()).isEqualTo(3200L);
        verify(pointService).getPointHistory(1L);
    }
}
