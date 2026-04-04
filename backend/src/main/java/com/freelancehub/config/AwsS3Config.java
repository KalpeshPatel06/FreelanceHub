package com.freelancehub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS S3 Configuration
 *
 * Creates a single S3Client bean shared across the application.
 *
 * Authentication:
 *   DefaultCredentialsProvider automatically reads credentials from:
 *   1. Environment variables: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
 *   2. ~/.aws/credentials file (for local dev)
 *   3. EC2 instance IAM role (best practice for production)
 *
 * Best Practice for EC2:
 *   Instead of storing AWS keys in environment variables on EC2,
 *   attach an IAM Role to your EC2 instance with S3 permissions.
 *   The SDK picks up credentials automatically from the instance metadata.
 */
@Configuration
public class AwsS3Config {

    @Value("${app.aws.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
