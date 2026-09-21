package com.passro.passrobackend.file.service;

import com.passro.passrobackend.file.exception.FileException;
import com.passro.passrobackend.file.exception.code.FileErrorCode;
import com.passro.passrobackend.file.dto.ImageUploadResponseDto;
import com.passro.passrobackend.global.configuration.S3Properties;
import java.net.URL;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.passro.passrobackend.global.exception.APIException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class S3Service {

	private static final Duration DEFAULT_SIGNATURE_DURATION = Duration.ofMinutes(10);
	private static final Duration MAX_SIGNATURE_DURATION = Duration.ofDays(7);
	private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
	private static final Map<String, Set<String>> ALLOWED_IMAGE_TYPES = Map.of(
			"image/jpeg", Set.of("jpg", "jpeg"),
			"image/png", Set.of("png"),
			"image/webp", Set.of("webp")
	);
	private static final Pattern UPLOAD_IMAGE_KEY_PATTERN = Pattern.compile(
			"uploads/images/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png|webp)");
	private static final Pattern FINAL_IMAGE_KEY_PATTERN = Pattern.compile(
			"(delivery-images|report-images|market-images)/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png|webp)");

	private final S3Presigner s3Presigner;
	private final S3Client s3Client;
	private final S3Properties s3Properties;

	public S3Service(S3Presigner s3Presigner, S3Client s3Client, S3Properties s3Properties) {
		this.s3Presigner = s3Presigner;
		this.s3Client = s3Client;
		this.s3Properties = s3Properties;
	}

	public URL getPresignedUploadUrl(String objectKey) {
		return getPresignedUploadUrl(objectKey, DEFAULT_SIGNATURE_DURATION, null);
	}

	public ImageUploadResponseDto createImageUploadUrl(
			String fileName,
			String contentType,
			long fileSize
	) {
		String normalizedContentType = contentType == null
				? ""
				: contentType.toLowerCase(Locale.ROOT);
		String extension = validateAndGetImageExtension(fileName, normalizedContentType, fileSize);
		String imageKey = "uploads/images/" + UUID.randomUUID() + "." + extension;
		URL uploadUrl = getPresignedUploadUrl(imageKey, normalizedContentType, fileSize);
		return new ImageUploadResponseDto(imageKey, uploadUrl.toString());
	}

	public URL getPresignedUploadUrl(String objectKey, String contentType, long contentLength) {
		validate(objectKey, DEFAULT_SIGNATURE_DURATION);
		if (contentLength <= 0) {
			throw new IllegalArgumentException("contentLength must be positive");
		}

		try {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(s3Properties.getBucket())
					.key(objectKey)
					.contentType(contentType)
					.contentLength(contentLength)
					.build();
			PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
					.signatureDuration(DEFAULT_SIGNATURE_DURATION)
					.putObjectRequest(putObjectRequest)
					.build();
			return s3Presigner.presignPutObject(presignRequest).url();
		} catch (Exception e) {
			throw new FileException(FileErrorCode.FILE_UPLOAD_FAILED);
		}
	}

	public URL getPresignedUploadUrl(String objectKey, Duration signatureDuration, String contentType) {
        try {
            validate(objectKey, signatureDuration);
            PutObjectRequest.Builder putObjectRequestBuilder = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(objectKey);

            if (StringUtils.hasText(contentType)) {
                putObjectRequestBuilder.contentType(contentType);
            }

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(signatureDuration)
                    .putObjectRequest(putObjectRequestBuilder.build())
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            return presignedRequest.url();
        } catch (Exception e) {
            throw new FileException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
	}

	public void validateUploadedImage(String objectKey) {
		if (!isSupportedImageKey(objectKey)) {
			throw new FileException(FileErrorCode.INVALID_FILE_NAME);
		}
		validateUploadedImage(objectKey, ALLOWED_IMAGE_TYPES.keySet(), MAX_IMAGE_SIZE);
	}

	public String finalizeUploadedImage(String uploadKey, String finalDirectory) {
		if (!StringUtils.hasText(uploadKey) || !UPLOAD_IMAGE_KEY_PATTERN.matcher(uploadKey).matches()) {
			throw new FileException(FileErrorCode.INVALID_FILE_NAME);
		}
		if (!StringUtils.hasText(finalDirectory)
				|| !(finalDirectory.equals("delivery-images/")
				|| finalDirectory.equals("report-images/")
				|| finalDirectory.equals("market-images/"))) {
			throw new FileException(FileErrorCode.INVALID_FILE_NAME);
		}

		validateUploadedImage(uploadKey, ALLOWED_IMAGE_TYPES.keySet(), MAX_IMAGE_SIZE);

		String extension = uploadKey.substring(uploadKey.lastIndexOf('.') + 1);
		String finalKey = finalDirectory + UUID.randomUUID() + "." + extension;
		try {
			s3Client.copyObject(CopyObjectRequest.builder()
					.bucket(s3Properties.getBucket())
					.copySource(s3Properties.getBucket() + "/" + uploadKey)
					.key(finalKey)
					.build());
			s3Client.deleteObject(DeleteObjectRequest.builder()
					.bucket(s3Properties.getBucket())
					.key(uploadKey)
					.build());
			return finalKey;
		} catch (Exception e) {
			throw new FileException(FileErrorCode.FILE_UPLOAD_FAILED);
		}
	}

	private void validateUploadedImage(String objectKey, Set<String> allowedContentTypes, long maxSize) {
		try {
			validate(objectKey, DEFAULT_SIGNATURE_DURATION);
			HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
					.bucket(s3Properties.getBucket())
					.key(objectKey)
					.build());
			String contentType = response.contentType() == null
					? ""
					: response.contentType().toLowerCase(Locale.ROOT);
			if (!allowedContentTypes.contains(contentType)) {
				throw new FileException(FileErrorCode.INVALID_IMAGE_FORMAT);
			}
			if (response.contentLength() == null || response.contentLength() <= 0 || response.contentLength() > maxSize) {
				throw new FileException(FileErrorCode.INVALID_FILE_SIZE);
			}
			validateImageSignature(objectKey, contentType);
		} catch (FileException e) {
			throw e;
		} catch (Exception e) {
			throw new FileException(FileErrorCode.FILE_NOT_FOUND);
		}
	}

	private void validateImageSignature(String objectKey, String contentType) {
		ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
				.bucket(s3Properties.getBucket())
				.key(objectKey)
				.range("bytes=0-11")
				.build());
		byte[] bytes = response.asByteArray();

		boolean valid = switch (contentType) {
			case "image/jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
			case "image/png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
			case "image/webp" -> startsWith(bytes, 0x52, 0x49, 0x46, 0x46)
					&& matchesAt(bytes, 8, 0x57, 0x45, 0x42, 0x50);
			default -> false;
		};
		if (!valid) {
			throw new FileException(FileErrorCode.INVALID_IMAGE_FORMAT);
		}
	}

	private boolean startsWith(byte[] bytes, int... signature) {
		return matchesAt(bytes, 0, signature);
	}

	private boolean matchesAt(byte[] bytes, int offset, int... signature) {
		if (bytes.length < offset + signature.length) {
			return false;
		}
		for (int i = 0; i < signature.length; i++) {
			if ((bytes[offset + i] & 0xFF) != signature[i]) {
				return false;
			}
		}
		return true;
	}

	private boolean isSupportedImageKey(String objectKey) {
		return StringUtils.hasText(objectKey)
				&& (UPLOAD_IMAGE_KEY_PATTERN.matcher(objectKey).matches()
				|| FINAL_IMAGE_KEY_PATTERN.matcher(objectKey).matches());
	}

	private String validateAndGetImageExtension(String fileName, String contentType, long fileSize) {
		if (fileSize <= 0 || fileSize > MAX_IMAGE_SIZE) {
			throw new FileException(FileErrorCode.INVALID_FILE_SIZE);
		}

		Set<String> extensions = ALLOWED_IMAGE_TYPES.get(contentType);
		if (extensions == null) {
			throw new FileException(FileErrorCode.INVALID_IMAGE_FORMAT);
		}

		if (!StringUtils.hasText(fileName)) {
			throw new FileException(FileErrorCode.INVALID_FILE_NAME);
		}
		String normalizedFileName = fileName.trim().toLowerCase(Locale.ROOT);
		int dotIndex = normalizedFileName.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == normalizedFileName.length() - 1) {
			throw new FileException(FileErrorCode.INVALID_FILE_NAME);
		}

		String extension = normalizedFileName.substring(dotIndex + 1);
		if (!extensions.contains(extension)) {
			throw new FileException(FileErrorCode.INVALID_IMAGE_FORMAT);
		}
		return extension;
	}

	public URL getPresignedDownloadUrl(String objectKey) {
		return getPresignedDownloadUrl(objectKey, DEFAULT_SIGNATURE_DURATION);
	}

	public String getPresignedDownloadUrlString(String objectKey) {
		if (!StringUtils.hasText(objectKey)) {
			return null;
		}
		return getPresignedDownloadUrl(objectKey).toString();
	}

	public URL getPresignedDownloadUrl(String objectKey, Duration signatureDuration) {
		try {
            validate(objectKey, signatureDuration);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(signatureDuration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url();
        }catch (Exception e) {
            throw new FileException(FileErrorCode.FILE_NOT_FOUND);
        }
	}

	private void validate(String objectKey, Duration signatureDuration) {
		if (!StringUtils.hasText(s3Properties.getBucket())) {
			throw new IllegalStateException("aws.s3.bucket is required");
		}

		if (!StringUtils.hasText(objectKey)) {
			throw new IllegalArgumentException("objectKey is required");
		}

		if (signatureDuration == null || signatureDuration.isNegative() || signatureDuration.isZero()) {
			throw new IllegalArgumentException("signatureDuration must be positive");
		}

		if (signatureDuration.compareTo(MAX_SIGNATURE_DURATION) > 0) {
			throw new IllegalArgumentException("signatureDuration cannot exceed 7 days");
		}
	}
}
