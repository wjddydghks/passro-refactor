package com.passro.passrobackend.global.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .addServersItem(new Server()
                        .url("https://passro.suplitter.com")
                        .description("배포 서버"))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("로컬 서버"))
                .tags(tags())
                .info(info());
    }

    @Bean
    public OpenApiCustomizer tagOrderCustomizer() {
        return openApi -> openApi.setTags(tags());
    }

    private List<Tag> tags() {
        return List.of(
                tag("인증", "이메일 인증 및 회원가입 API"),
                tag("계정", "프로필 조회 및 수정 API"),
                tag("포인트", "로그인한 사용자의 포인트 조회 API"),
                tag("마켓", "포인트 상품 조회 및 구매 API"),
                tag("지하철", "지하철역 검색 및 최단 경로 탐색 API"),
                tag("발송자", "발송자의 배송 요청 조회 및 관리 API"),
                tag("전달자", "전달자의 배송 조회 및 배송 상태 변경 API"),
                tag("채팅", "배송 발송자와 전달자 간 채팅 API"),
                tag("배송 문의", "특정 배송 건에 대한 문의 작성 및 조회 API"),
                tag("리뷰", "전달자 리뷰 작성 및 평점 조회 API"),
                tag("문의(공통)", "배송과 무관한 공통 문의 API"),
                tag("파일", "S3 파일 업로드 및 다운로드 URL 발급 API"));
    }

    private Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }

    private Info info() {
        return new Info()
                .title("Passro API")
                .version("1.0.0")
                .description("Passro API Documentation");
    }
}
