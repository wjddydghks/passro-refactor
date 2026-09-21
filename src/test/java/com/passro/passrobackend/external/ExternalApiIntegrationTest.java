package com.passro.passrobackend.external;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.passro.passrobackend.file.exception.FileException;
import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.support.IntegrationTestSupport;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ExternalApiIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private S3Service s3Service;

    @Test
    void uploadAndDownloadPresignedUrlsAreReturned() throws Exception {
        mockMvc.perform(get("/file/{fileName}/upload", "profile.png"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", containsString("profile.png")))
                .andExpect(jsonPath("$.result", containsString("X-Amz-Signature")));

        mockMvc.perform(get("/file/{fileName}/download", "profile.png"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", containsString("profile.png")))
                .andExpect(jsonPath("$.result", containsString("X-Amz-Signature")));
    }

    @Test
    void invalidPresignedUrlArgumentsAreRejected() {
        assertThatThrownBy(() -> s3Service.getPresignedUploadUrl("", Duration.ofMinutes(10), null))
                .isInstanceOf(FileException.class);
        assertThatThrownBy(() -> s3Service.getPresignedDownloadUrl("profile.png", Duration.ofDays(8)))
                .isInstanceOf(FileException.class);
    }

    @Test
    void imageUploadUrlIsIssuedByFileApi() throws Exception {
        mockMvc.perform(post("/file/image/upload-url")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": "proof.jpg",
                                  "contentType": "image/jpeg",
                                  "fileSize": 1024
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.imageKey", containsString("uploads/images/")))
                .andExpect(jsonPath("$.result.imageKey", containsString(".jpg")))
                .andExpect(jsonPath("$.result.uploadUrl", containsString("X-Amz-Signature")));
    }

    @Test
    void imageUploadRejectsMismatchedExtensionAndContentType() throws Exception {
        mockMvc.perform(post("/file/image/upload-url")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": "proof.png",
                                  "contentType": "image/jpeg",
                                  "fileSize": 1024
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE400_1"));
    }

    @Test
    void subwaySearchFindsRouteNameContainingKeyword() throws Exception {
        mockMvc.perform(get("/subway/search").param("keyword", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUBWAY200_2"))
                .andExpect(jsonPath("$.result[0].id").isNumber())
                .andExpect(jsonPath("$.result[0].region").doesNotExist())
                .andExpect(jsonPath("$.result[*].routeName", hasItem("2호선")));
    }

    @Test
    void subwaySearchFindsStationNameContainingKeyword() throws Exception {
        mockMvc.perform(get("/subway/search").param("keyword", "강남"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[*].stationName", hasItem("강남")));
    }

    @Test
    void subwaySearchAcceptsHangulConsonantsAndVowels() throws Exception {
        mockMvc.perform(get("/subway/search").param("keyword", "ㄱ"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/subway/search").param("keyword", "ㅏ"))
                .andExpect(status().isOk());
    }

    @Test
    void subwaySearchRejectsInvalidKeyword() throws Exception {
        mockMvc.perform(get("/subway/search").param("keyword", "Gangnam!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400"));
    }
}
