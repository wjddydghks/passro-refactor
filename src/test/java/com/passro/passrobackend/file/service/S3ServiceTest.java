package com.passro.passrobackend.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.passro.passrobackend.file.exception.FileException;
import com.passro.passrobackend.file.exception.code.FileErrorCode;
import com.passro.passrobackend.global.configuration.S3Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    private static final String PNG_UPLOAD_KEY =
            "uploads/images/123e4567-e89b-12d3-a456-426614174000.png";
    private static final String JPEG_UPLOAD_KEY =
            "uploads/images/123e4567-e89b-12d3-a456-426614174000.jpg";

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Client s3Client;

    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        S3Properties properties = new S3Properties();
        properties.setBucket("test-bucket");
        s3Service = new S3Service(s3Presigner, s3Client, properties);
    }

    @Test
    void rejectNonImageBytesEvenWhenMetadataSaysPng() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willReturn(HeadObjectResponse.builder()
                        .contentType("image/png")
                        .contentLength(12L)
                        .build());
        given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .willReturn(ResponseBytes.fromByteArray(
                        GetObjectResponse.builder().build(),
                        "not-an-image".getBytes()));

        assertThatThrownBy(() -> s3Service.validateUploadedImage(PNG_UPLOAD_KEY))
                .isInstanceOf(FileException.class)
                .extracting(e -> ((FileException) e).getCode())
                .isEqualTo(FileErrorCode.INVALID_IMAGE_FORMAT);
    }

    @Test
    void copyValidatedUploadToImmutableFinalKeyAndDeleteTemporaryObject() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willReturn(HeadObjectResponse.builder()
                        .contentType("image/jpeg")
                        .contentLength(4L)
                        .build());
        given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .willReturn(ResponseBytes.fromByteArray(
                        GetObjectResponse.builder().build(),
                        new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}));

        String finalKey = s3Service.finalizeUploadedImage(JPEG_UPLOAD_KEY, "delivery-images/");

        assertThat(finalKey)
                .startsWith("delivery-images/")
                .endsWith(".jpg");
        verify(s3Client).copyObject(any(CopyObjectRequest.class));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void copyValidatedUploadToReportImagesDirectoryAndDeleteTemporaryObject() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willReturn(HeadObjectResponse.builder()
                        .contentType("image/jpeg")
                        .contentLength(4L)
                        .build());
        given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .willReturn(ResponseBytes.fromByteArray(
                        GetObjectResponse.builder().build(),
                        new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}));

        String finalKey = s3Service.finalizeUploadedImage(JPEG_UPLOAD_KEY, "report-images/");

        assertThat(finalKey)
                .startsWith("report-images/")
                .endsWith(".jpg");
        verify(s3Client).copyObject(any(CopyObjectRequest.class));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void copyValidatedUploadToMarketImagesDirectoryAndDeleteTemporaryObject() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willReturn(HeadObjectResponse.builder()
                        .contentType("image/jpeg")
                        .contentLength(4L)
                        .build());
        given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .willReturn(ResponseBytes.fromByteArray(
                        GetObjectResponse.builder().build(),
                        new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}));

        String finalKey = s3Service.finalizeUploadedImage(JPEG_UPLOAD_KEY, "market-images/");

        assertThat(finalKey)
                .startsWith("market-images/")
                .endsWith(".jpg");
        verify(s3Client).copyObject(any(CopyObjectRequest.class));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void rejectInvalidFinalDirectoryWhenFinalizingUploadedImage() {
        assertThatThrownBy(() -> s3Service.finalizeUploadedImage(JPEG_UPLOAD_KEY, "invalid-images/"))
                .isInstanceOf(FileException.class)
                .extracting(e -> ((FileException) e).getCode())
                .isEqualTo(FileErrorCode.INVALID_FILE_NAME);
    }
}
