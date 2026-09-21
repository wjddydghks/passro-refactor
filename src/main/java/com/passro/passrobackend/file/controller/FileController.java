package com.passro.passrobackend.file.controller;

import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.file.exception.code.FileSuccessCode;
import com.passro.passrobackend.file.dto.ImageUploadRequestDto;
import com.passro.passrobackend.file.dto.ImageUploadResponseDto;
import com.passro.passrobackend.global.response.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;

import static com.passro.passrobackend.global.configuration.SwaggerErrorExamples.FILE_NOT_FOUND;
import static com.passro.passrobackend.global.configuration.SwaggerErrorExamples.FILE_UPLOAD_FAILED;
import static com.passro.passrobackend.global.configuration.SwaggerSuccessExamples.FILE_URL;

@RequestMapping("/file")
@AllArgsConstructor
@RestController
@Tag(name = "파일", description = "S3 파일 업로드 및 다운로드 URL 발급 API")
public class FileController {
    private final S3Service s3Service;

    @PostMapping("/image/upload-url")
    @Operation(summary = "이미지 업로드 URL 발급",
            description = "JPEG, PNG, WebP 이미지를 직접 업로드할 수 있는 10분 유효 사전 서명 URL을 발급합니다.")
    public APIResponse<ImageUploadResponseDto> getImageUploadUrl(
            @Valid @RequestBody ImageUploadRequestDto request) {
        return APIResponse.onSuccess(
                FileSuccessCode.OK,
                s3Service.createImageUploadUrl(
                        request.getFileName(),
                        request.getContentType(),
                        request.getFileSize()));
    }

    @GetMapping("/image/download-url")
    @Operation(summary = "이미지 다운로드 URL 발급",
            description = "S3 이미지 키로 10분 유효한 다운로드 URL을 발급합니다.")
    public APIResponse<String> getImageDownloadUrl(@RequestParam String imageKey) {
        s3Service.validateUploadedImage(imageKey);
        return APIResponse.onSuccess(
                FileSuccessCode.OK,
                s3Service.getPresignedDownloadUrl(imageKey).toString());
    }

    @GetMapping("{fileName}/upload")
    @Operation(summary = "파일 업로드 URL 발급", description = "파일을 직접 업로드할 수 있는 10분 유효한 사전 서명 URL을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업로드 URL 발급 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "FILE200_1", summary = "업로드 URL 발급 성공", value = FILE_URL))),
            @ApiResponse(responseCode = "500", description = "업로드 URL 발급 실패",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "FILE500_1", summary = "업로드 URL 발급 실패", value = FILE_UPLOAD_FAILED)))
    })
    public APIResponse<String> getUploadUrl(
            @Parameter(description = "S3에 저장할 파일 이름", example = "user-1.png") @PathVariable String fileName) {
        URL url = s3Service.getPresignedUploadUrl(fileName);
        return APIResponse.onSuccess(FileSuccessCode.OK, url.toString());
    }

    @GetMapping("{fileName}/download")
    @Operation(summary = "파일 다운로드 URL 발급", description = "파일을 직접 다운로드할 수 있는 10분 유효한 사전 서명 URL을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "다운로드 URL 발급 성공", useReturnTypeSchema = true,
                    content = @Content(examples = @ExampleObject(name = "FILE200_1", summary = "다운로드 URL 발급 성공", value = FILE_URL))),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = APIResponse.class),
                            examples = @ExampleObject(name = "FILE404_1", summary = "파일 정보 없음", value = FILE_NOT_FOUND)))
    })
    public APIResponse<String> getDownloadUrl(
            @Parameter(description = "S3에 저장된 파일 이름", example = "user-1.png") @PathVariable String fileName) {
        URL url = s3Service.getPresignedDownloadUrl(fileName);
        return APIResponse.onSuccess(FileSuccessCode.OK, url.toString());
    }
}
