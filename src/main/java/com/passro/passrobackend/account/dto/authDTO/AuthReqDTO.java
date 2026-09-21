package com.passro.passrobackend.account.dto.authDTO;

import com.passro.passrobackend.account.validation.PasswordComplexity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

public class AuthReqDTO {

    @Getter
    public static class Signup {
        @NotBlank(message = "이메일을 입력하세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String mail;

        @NotBlank(message = "변경할 비밀번호를 입력하세요.")
        @Size(min = 6, max = 20, message = "비밀번호는 6자 이상 20자 이하여야 합니다.")
        @PasswordComplexity
        private String password;

        @NotBlank(message = "닉네임을 입력하세요.")
        @Size(max = 20, message = "닉네임은 20자 이하여야 합니다.")
        private String nickname;

        @NotBlank(message = "이름을 입력하세요.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        private String name;

        @NotBlank(message = "전화번호를 입력하세요.")
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
        private String phoneNumber;

        @NotNull(message = "생년월일을 입력하세요.")
        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        private LocalDate birth;

        private Long point;
        private String picture;

        @NotNull(message = "출발역 Place ID를 입력하세요.")
        @Positive(message = "출발역 Place ID는 양수여야 합니다.")
        private Long sourceStationId;

        @NotNull(message = "도착역 Place ID를 입력하세요.")
        @Positive(message = "도착역 Place ID는 양수여야 합니다.")
        private Long destinationStationId;

        private List<@NotNull(message = "경유역 Place ID는 null일 수 없습니다.")
                @Positive(message = "경유역 Place ID는 양수여야 합니다.") Long> wayPoints;
    }


    @Getter
    public static class WayPoint{
        private String stationName;
        private String routeName;
    }


    @Getter
    public static class SendMail{
        @NotBlank(message = "이메일을 입력하세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String mail;

        private boolean student;
    }

    @Getter
    public static class ConfirmCode{
        private String mail;

        @NotBlank(message="인증 코드를 입력하세요")
        @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 숫자 6자리여야 합니다.")
        private String code;
    }

    @Getter
    public static class Login {

        @NotBlank(message = "이메일을 입력하세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String mail;

        @NotBlank(message = "비밀번호를 입력하세요.")
        @Size(min = 6, max = 20, message = "비밀번호는 6자 이상 20자 이하여야 합니다.")
        private String password;
    }

    @Getter
    public static class ReIssue{

        @NotBlank
        private String refreshToken;
    }

    @Getter
    public static class FindId{
        @NotBlank(message = "이름을 입력하세요.")
        private String name;

        @NotBlank(message = "전화번호를 입력하세요.")
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
        private String phoneNumber;
    }

    @Getter
    public static class FindPassword{
        @NotBlank(message = "이름을 입력하세요.")
        private String name;

        @NotBlank(message = "전화번호를 입력하세요.")
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
        private String phoneNumber;
        
        @NotBlank(message = "이메일을 입력하세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String mail;
    }
}
