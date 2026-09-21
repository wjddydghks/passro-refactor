package com.passro.passrobackend.account.controller;

import com.passro.passrobackend.account.dto.authDTO.AuthReqDTO;
import com.passro.passrobackend.account.dto.authDTO.AuthResDTO;
import com.passro.passrobackend.account.exception.code.AccountSuccessCode;
import com.passro.passrobackend.account.service.AccountService;
import com.passro.passrobackend.account.service.AuthService;
import com.passro.passrobackend.account.service.MailSenderService;
import com.passro.passrobackend.account.service.VerificationCodeService;
import com.passro.passrobackend.global.code.BaseSuccessCode;
import com.passro.passrobackend.global.configuration.security.CustomUserDetails;
import com.passro.passrobackend.global.response.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.passro.passrobackend.global.configuration.SwaggerErrorExamples.*;
import static com.passro.passrobackend.global.configuration.SwaggerSuccessExamples.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/auth")
@Tag(name = "인증", description = "이메일 인증 및 회원가입 API")
public class AuthController {

    private final AccountService accountService;
    private final MailSenderService mailSenderService;
    private final VerificationCodeService verificationCodeService;
    private final AuthService authService;

    @GetMapping("/nickname/check")
    @Operation(summary = "닉네임 중복 확인", description = "닉네임이 사용 가능한지 확인합니다. true이면 사용 가능합니다.")
    public APIResponse<Boolean> checkNickname(
            @RequestParam
            @NotBlank(message = "닉네임을 입력하세요.")
            @Size(max = 20, message = "닉네임은 20자 이하여야 합니다.")
            String nickname) {
        return APIResponse.onSuccess(AccountSuccessCode.OK, accountService.isNicknameAvailable(nickname));
    }

    @GetMapping("/mail/check")
    @Operation(summary = "이메일 중복 확인", description = "이메일이 사용 가능한지 확인합니다. true이면 사용 가능합니다.")
    public APIResponse<Boolean> checkEmail(
            @RequestParam
            @NotBlank(message = "이메일 입력하세요.")
            String mail) {
        return APIResponse.onSuccess(AccountSuccessCode.OK, accountService.isMailAvailable(mail));
    }

    @PostMapping("/mail/send")
    @Operation(summary = "인증 메일 발송", description = "이메일로 6자리 인증 코드를 발송합니다. 인증 코드는 5분 동안 유효합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 메일 발송 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "ACCOUNT200_1", summary = "인증 메일 발송 성공", value = ACCOUNT_OK))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 이메일 또는 이미 가입된 이메일",
                    content = @Content(schema = @Schema(implementation = APIResponse.class), examples = {
                            @ExampleObject(name = "COMMON400", summary = "요청 값 검증 실패", value = COMMON_VALIDATION),
                            @ExampleObject(name = "ACCOUNT400_4", summary = "이메일 중복", value = ACCOUNT_DUPLICATE_MAIL),
                            @ExampleObject(name = "ACCOUNT400_6", summary = "대학교 이메일이 아님", value = ACCOUNT_INVALID_EMAIL_DOMAIN)
                    })),
            @ApiResponse(responseCode = "429", description = "인증 메일 재발송 제한",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "ACCOUNT429_1", summary = "인증 메일 재발송 제한", value = ACCOUNT_TOO_FAST)))
    })
    public APIResponse<Void> mailSend(@Valid @RequestBody AuthReqDTO.SendMail dto){
        BaseSuccessCode code = AccountSuccessCode.OK;
        mailSenderService.sendMailMessageSignUpOrShipperSelect(dto);
        return APIResponse.onSuccess(code, null);
    }

    @PostMapping("/mail/confirm")
    @Operation(summary = "이메일 인증 코드 확인", description = "이메일로 발송된 6자리 인증 코드를 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 인증 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "ACCOUNT200_1", summary = "이메일 인증 성공", value = ACCOUNT_OK))),
            @ApiResponse(responseCode = "400", description = "인증 코드 불일치 또는 만료",
                    content = @Content(schema = @Schema(implementation = APIResponse.class), examples = {
                            @ExampleObject(name = "COMMON400", summary = "요청 값 검증 실패", value = COMMON_VALIDATION),
                            @ExampleObject(name = "ACCOUNT400_1", summary = "인증 코드 만료", value = ACCOUNT_MAIL_CODE_EXPIRED),
                            @ExampleObject(name = "ACCOUNT400_2", summary = "인증 코드 불일치", value = ACCOUNT_MAIL_CODE_MISMATCH)
                    }))
    })
    public APIResponse<Void> confirmCode(@Valid @RequestBody AuthReqDTO.ConfirmCode dto, @AuthenticationPrincipal CustomUserDetails userDetails){
        BaseSuccessCode code = AccountSuccessCode.OK;
        verificationCodeService.confirmCode(dto);
        return APIResponse.onSuccess(code, null);
    }

    @PostMapping("/mail/confirm/University")
    @Operation(summary = "이메일 인증 코드 확인", description = "이메일로 발송된 6자리 인증 코드를 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 인증 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "ACCOUNT200_1", summary = "이메일 인증 성공", value = ACCOUNT_OK))),
            @ApiResponse(responseCode = "400", description = "인증 코드 불일치 또는 만료",
                    content = @Content(schema = @Schema(implementation = APIResponse.class), examples = {
                            @ExampleObject(name = "COMMON400", summary = "요청 값 검증 실패", value = COMMON_VALIDATION),
                            @ExampleObject(name = "ACCOUNT400_1", summary = "인증 코드 만료", value = ACCOUNT_MAIL_CODE_EXPIRED),
                            @ExampleObject(name = "ACCOUNT400_2", summary = "인증 코드 불일치", value = ACCOUNT_MAIL_CODE_MISMATCH)
                    }))
    })
    public APIResponse<Void> confirmUniversityCode(@Valid @RequestBody AuthReqDTO.ConfirmCode dto, @AuthenticationPrincipal CustomUserDetails userDetails){
        BaseSuccessCode code = AccountSuccessCode.OK;
        verificationCodeService.confirmUniversityMailCode(dto, userDetails.getAccountId());
        return APIResponse.onSuccess(code, null);
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일 인증을 완료한 사용자의 계정을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "ACCOUNT200_1", summary = "회원가입 성공", value = ACCOUNT_OK))),
            @ApiResponse(responseCode = "400", description = "이메일 미인증, 이메일 중복 또는 닉네임 중복",
                    content = @Content(schema = @Schema(implementation = APIResponse.class), examples = {
                            @ExampleObject(name = "COMMON400", summary = "요청 값 검증 실패", value = COMMON_VALIDATION),
                            @ExampleObject(name = "ACCOUNT400_3", summary = "이메일 미인증", value = ACCOUNT_MAIL_NOT_CONFIRMED),
                            @ExampleObject(name = "ACCOUNT400_4", summary = "이메일 중복", value = ACCOUNT_DUPLICATE_MAIL),
                            @ExampleObject(name = "ACCOUNT400_6", summary = "닉네임 중복", value = ACCOUNT_DUPLICATE_NICKNAME)
                    }))
    })
    public APIResponse<Void> signup(@Valid @RequestBody AuthReqDTO.Signup dto){
        BaseSuccessCode code = AccountSuccessCode.OK;
        authService.signup(dto);
        return APIResponse.onSuccess(code, null);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 Access/Refresh Token을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AuthResDTO.TokenResponse.class),
                            examples = @ExampleObject(name = "ACCOUNT200_1", summary = "로그인 성공", value = ACCOUNT_LOGIN_SUCCESS))),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "COMMON400", summary = "요청 값 검증 실패", value = COMMON_VALIDATION))),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "ACCOUNT401_1", summary = "인증 실패", value = ACCOUNT_INVALID_CREDENTIALS)))
    })
    public ResponseEntity<APIResponse<AuthResDTO.TokenResponse>> login(@Valid @RequestBody AuthReqDTO.Login dto){
        BaseSuccessCode code = AccountSuccessCode.OK;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .body(APIResponse.onSuccess(code, authService.login(dto)));
    }

    @DeleteMapping("/logout")
    @Operation(summary = "로그아웃", description = "저장된 Refresh Token을 삭제하여 로그아웃 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "ACCOUNT200_1", summary = "로그아웃 성공", value = ACCOUNT_OK)))
    })
    public APIResponse<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails){
        BaseSuccessCode code = AccountSuccessCode.OK;
        authService.logout(userDetails.getAccountId());
        return APIResponse.onSuccess(code, null);
    }
    @PostMapping("/find/id")
    @Operation(summary = "아이디(이메일) 찾기", description = "이름과 전화번호로 본인 확인 후, 가입된 이메일을 해당 이메일로 발송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "요청 처리 성공(계정 존재 여부와 무관하게 동일하게 응답)", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "ACCOUNT200_1", summary = "요청 처리 성공", value = ACCOUNT_OK))),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "COMMON400", summary = "요청 값 검증 실패", value = COMMON_VALIDATION)))
    })
    public APIResponse<Void> findId(@Valid @RequestBody AuthReqDTO.FindId dto) {
        BaseSuccessCode code = AccountSuccessCode.OK;
        authService.findId(dto);
        return APIResponse.onSuccess(code, null);
    }

    @PostMapping("/find/password")
    @Operation(summary = "비밀번호 찾기", description = "이름, 전화번호, 이메일로 본인 확인 후 임시 비밀번호를 해당 이메일로 발송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "요청 처리 성공(계정 존재 여부와 무관하게 동일하게 응답)", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "ACCOUNT200_1", summary = "요청 처리 성공", value = ACCOUNT_OK))),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "COMMON400", summary = "요청 값 검증 실패", value = COMMON_VALIDATION)))
    })
    public APIResponse<Void> findPassword(@Valid @RequestBody AuthReqDTO.FindPassword dto) {
        BaseSuccessCode code = AccountSuccessCode.OK;
        authService.findPassword(dto);
        return APIResponse.onSuccess(code, null);
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새로운 Access/Refresh Token을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
                    content = @Content(schema = @Schema(implementation = AuthResDTO.TokenResponse.class),
                            examples = @ExampleObject(name = "ACCOUNT200_1", summary = "토큰 재발급 성공", value = ACCOUNT_REISSUE_SUCCESS))),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "COMMON400", summary = "요청 값 검증 실패", value = COMMON_VALIDATION))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "ACCOUNT401_2", summary = "리프레시 토큰 오류", value = ACCOUNT_INVALID_REFRESH_TOKEN)))
    })
    public ResponseEntity<APIResponse<AuthResDTO.TokenResponse>> reissue(@Valid @RequestBody AuthReqDTO.ReIssue dto){
        BaseSuccessCode code = AccountSuccessCode.OK;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .body(APIResponse.onSuccess(code, authService.reissueToken(dto)));
    }
}
