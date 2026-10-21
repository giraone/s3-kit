package com.giraone.s3kit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractS3ServiceTest {

    protected static final String DEFAULT_CONTENT_TYPE = "text/plain";

    // One container for all test classes. It is not stopped explicitly, because this may not work on Testcontainers cloud.
    private static GenericContainer<?> s3Container;

    protected S3Client s3Client;
    protected S3Service s3Service;

    @BeforeAll
    void init() {
        s3Client = S3Client.builder()
            .forcePathStyle(true)
            .region(Region.of("default"))
            .endpointOverride(URI.create(endpoint()))
            .credentialsProvider(() -> AwsBasicCredentials.create(
                S3MockTestContainer.ACCESS_KEY, S3MockTestContainer.SECRET_KEY))
            .build();
        s3Service = new S3ServiceAwsSdkImpl(s3Client);
    }

    private static synchronized String endpoint() {
        if (s3Container == null) {
            s3Container = S3MockTestContainer.create();
            s3Container.start();
        }
        return S3MockTestContainer.endpoint(s3Container);
    }

    protected String createNewObjectForTest(final String bucketName, final String key, final byte[] bytes) {
        return s3Service.uploadSingleFile(
            bucketName, key, bytes.length, DEFAULT_CONTENT_TYPE, new ByteArrayInputStream(bytes));
    }

    protected String createNewObjectForTest(final String bucketName, final String key) {
        return createNewObjectForTest(bucketName, key, "Simple-Test-Content".getBytes(StandardCharsets.UTF_8));
    }

    protected byte[] readAll(final com.giraone.s3kit.download.S3Download download) throws IOException {
        try (download) {
            return download.inputStream().readAllBytes();
        }
    }
}
