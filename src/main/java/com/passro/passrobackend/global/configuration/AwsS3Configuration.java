package com.passro.passrobackend.global.configuration;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class AwsS3Configuration {

    private final S3Properties s3Properties;

    public AwsS3Configuration(S3Properties s3Properties) {
        this.s3Properties = s3Properties;
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                        .pathStyleAccessEnabled(s3Properties.isPathStyleAccessEnabled())
                        .build());

        if (StringUtils.hasText(s3Properties.getEndpoint())) {
            builder.endpointOverride(URI.create(s3Properties.getEndpoint()));
        }

        return builder.build();
    }

    @Bean(destroyMethod = "close")
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                        .pathStyleAccessEnabled(s3Properties.isPathStyleAccessEnabled())
                        .build());

        if (StringUtils.hasText(s3Properties.getEndpoint())) {
            builder.endpointOverride(URI.create(s3Properties.getEndpoint()));
        }
        return builder.build();
    }

    private AwsCredentialsProvider credentialsProvider() {
        if (StringUtils.hasText(s3Properties.getAccessKey()) && StringUtils.hasText(s3Properties.getSecretKey())) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3Properties.getAccessKey(), s3Properties.getSecretKey())
            );
        }

        return DefaultCredentialsProvider.create();
    }
}

