package com.passro.passrobackend.account.dto.accountDTO;

import com.passro.passrobackend.account.exception.AccountException;
import com.passro.passrobackend.account.exception.code.AccountErrorCode;
import com.passro.passrobackend.account.validation.PasswordComplexity;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

public class AccountReqDTO {

    @Getter
    public static class EditMyInfo {
        @NotBlank(message = "변경할 닉네임을 입력하세요.")
        @Pattern(
                regexp = "^[가-힣a-zA-Z0-9]+$",
                message = "닉네임은 완성된 한글, 영문, 숫자만 입력 가능합니다."
        )
        private String nickname;

        @NotBlank(message = "변경할 이름을 입력하세요.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        private String name;

        @NotNull(message = "변경할 생년월일을 입력하세요.")
        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        private LocalDate birth;

        @NotBlank(message = "변경할 전화번호를 입력하세요.")
        @Pattern(
                regexp = "^01[0-9]-\\d{3,4}-\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)"
        )
        private String phoneNumber;

        private String picture;

        @NotNull(message = "변경할 출발지를 입력하세요.")
        private Long startPlaceId;

        @NotNull(message = "변경할 도착지를 입력하세요.")
        private Long destinationPlaceId;
        private List<@NotNull(message = "경유역 Place ID는 null일 수 없습니다.")
        @Positive(message = "경유역 Place ID는 양수여야 합니다.") Long> wayPoints;
    }

    @Getter
    public static class EditPassword {
        @NotBlank(message = "현재 비밀번호를 입력하세요.")
        @Size(min = 6, max = 20, message = "비밀번호는 6자 이상 20자 이하여야 합니다.")
        @PasswordComplexity
        private String nowPassword;

        @NotBlank(message = "변경할 비밀번호를 입력하세요.")
        @Size(min = 6, max = 20, message = "비밀번호는 6자 이상 20자 이하여야 합니다.")
        @PasswordComplexity
        private String editPassword;
    }
}
