package com.passro.passrobackend.account.controller;

import com.passro.passrobackend.account.dto.accountDTO.AccountReqDTO;
import com.passro.passrobackend.account.dto.accountDTO.AccountResDTO;
import com.passro.passrobackend.account.exception.code.AccountSuccessCode;
import com.passro.passrobackend.account.service.AccountService;
import com.passro.passrobackend.account.service.MailSenderService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.passro.passrobackend.global.configuration.SwaggerErrorExamples.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "계정", description = "프로필 조회 및 수정")
public class AccountController {

    private final AccountService accountService;
    private final MailSenderService mailSenderService;

    @GetMapping("/mypage/shipper")
    @Operation(summary = "배송기사 마이페이지 조회", description = "마이페이지를 배송기사 기준으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배송기사 마이페이지 조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "ACCOUNT404_1", summary = "계정 없음", value = ACCOUNT_NOT_FOUND)))
    })
    public ResponseEntity<APIResponse<AccountResDTO.ShipperMyPage>> shipperPage(@AuthenticationPrincipal CustomUserDetails userDetails){
        BaseSuccessCode code = AccountSuccessCode.OK;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .body(APIResponse.onSuccess(code, accountService.myShipperPage(userDetails.getAccountId())));
    }

    @GetMapping("/mypage/sender")
    @Operation(summary = "발송자 마이페이지 조회", description = "마이페이지를 발송자 기준으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송자 마이페이지 조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "ACCOUNT404_1", summary = "계정 없음", value = ACCOUNT_NOT_FOUND)))
    })
    public ResponseEntity<APIResponse<AccountResDTO.SenderMyPage>> senderPage(@AuthenticationPrincipal CustomUserDetails userDetails){
        BaseSuccessCode code = AccountSuccessCode.OK;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .body(APIResponse.onSuccess(code, accountService.mySenderPage(userDetails.getAccountId())));
    }

    @GetMapping("/mypage/student-certification")
    @Operation(summary = "학생 인증 여부 조회", description = "로그인한 사용자의 학생 인증 여부를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "학생 인증 여부 조회 성공", useReturnTypeSchema = true)
    public APIResponse<Boolean> getStudentCertificationStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return APIResponse.onSuccess(
                AccountSuccessCode.OK,
                accountService.isStudentCertified(userDetails.getAccountId()));
    }

    @PatchMapping("/mypage/edit/myInfo")
    @Operation(summary = "마이페이지 수정", description = "프로필 이미지, 닉네임, 이름, 생일, 전화번호, 출발지/도착지, 경유지를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마이페이지 수정 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패, 닉네임 중복 또는 전화번호 중복",
                    content = @Content(schema = @Schema(implementation = APIResponse.class), examples = {
                            @ExampleObject(name = "COMMON400", summary = "요청 값 검증 실패", value = COMMON_VALIDATION),
                            @ExampleObject(name = "ACCOUNT400_6", summary = "닉네임 중복", value = ACCOUNT_DUPLICATE_NICKNAME),
                            @ExampleObject(name = "ACCOUNT400_7", summary = "전화번호 중복", value = ACCOUNT_DUPLICATE_PHONE_NUMBER)
                    })),
            @ApiResponse(responseCode = "404", description = "계정, 출발지/도착지 또는 경유지 역 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class), examples = {
                            @ExampleObject(name = "ACCOUNT404_1", summary = "계정 없음", value = ACCOUNT_NOT_FOUND),
                            @ExampleObject(name = "Account404_2", summary = "지하철역 없음", value = ACCOUNT_NOT_FOUND_SUBWAY)
                    })),
            @ApiResponse(responseCode = "429", description = "정보 수정 제한(쿨다운)",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "ACCOUNT429_1", summary = "잠시 후 다시 시도", value = ACCOUNT_TOO_FAST)))
    })
    public ResponseEntity<APIResponse<Void>> editNickname(@Valid @RequestBody AccountReqDTO.EditMyInfo dto, @AuthenticationPrincipal CustomUserDetails userDetails){
        BaseSuccessCode code = AccountSuccessCode.OK;
        accountService.editMyInfo(dto, userDetails.getAccountId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .body(APIResponse.onSuccess(code, null));
    }

    @PatchMapping("/mypage/edit/password")
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인한 뒤 새로운 비밀번호로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 또는 현재 비밀번호와 동일한 비밀번호로 변경 시도",
                    content = @Content(schema = @Schema(implementation = APIResponse.class), examples = {
                            @ExampleObject(name = "COMMON400", summary = "요청 값 검증 실패", value = COMMON_VALIDATION),
                            @ExampleObject(name = "ACCOUNT400_9", summary = "현재 비밀번호와 동일", value = ACCOUNT_SAME_PASSWORD)
                    })),
            @ApiResponse(responseCode = "401", description = "현재 비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "ACCOUNT401_1", summary = "현재 비밀번호 불일치", value = ACCOUNT_INVALID_CREDENTIALS))),
            @ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "ACCOUNT404_1", summary = "계정 없음", value = ACCOUNT_NOT_FOUND))),
            @ApiResponse(responseCode = "429", description = "비밀번호 변경 제한(쿨다운)",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "ACCOUNT429_1", summary = "잠시 후 다시 시도", value = ACCOUNT_TOO_FAST)))
    })
    public APIResponse<Void> editPassword(@Valid @RequestBody AccountReqDTO.EditPassword dto, @AuthenticationPrincipal CustomUserDetails userDetails){
        BaseSuccessCode code = AccountSuccessCode.OK;
        accountService.editPassword(dto, userDetails.getAccountId());
        return APIResponse.onSuccess(code, null);
    }
}
