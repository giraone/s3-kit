package com.giraone.s3kit;

import com.giraone.s3kit.download.S3Download;
import com.giraone.s3kit.exceptions.S3CallFailedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3ServiceObjectTest extends AbstractS3ServiceTest {

    private static final String BUCKET = "object-test-bucket";

    @Override
    @BeforeAll
    void init() {
        super.init();
        s3Service.createBucket(BUCKET);
    }

    @Test
    @DisplayName("Upload and fetch a small object")
    void uploadAndFetch() throws IOException {
        // arrange
        final byte[] content = "Hello S3".getBytes(StandardCharsets.UTF_8);
        // act
        final String key = createNewObjectForTest(BUCKET, "dir/hello.txt", content);
        final S3Download download = s3Service.fetch(BUCKET + "/" + key);
        // assert
        assertThat(key).isEqualTo("dir/hello.txt");
        assertThat(download.status()).isEqualTo(200);
        assertThat(download.contentType()).isEqualTo(DEFAULT_CONTENT_TYPE);
        assertThat(download.contentLength()).isEqualTo(content.length);
        assertThat(download.fileName()).isEqualTo("hello.txt");
        assertThat(readAll(download)).isEqualTo(content);
    }

    @Test
    @DisplayName("Fetch can stream to an OutputStream")
    void fetchTransferTo() throws IOException {
        // arrange
        final byte[] content = new byte[1024 * 1024];
        java.util.Arrays.fill(content, (byte) 'x');
        createNewObjectForTest(BUCKET, "big.bin", content);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        // act
        final long copied = s3Service.fetch(BUCKET, "big.bin").transferTo(out);
        // assert
        assertThat(copied).isEqualTo(content.length);
        assertThat(out.toByteArray()).isEqualTo(content);
    }

    @Test
    @DisplayName("Fetch of a missing object fails with 404")
    void fetchMissing() {
        // act + assert
        assertThatThrownBy(() -> s3Service.fetch(BUCKET, "missing.txt"))
            .isInstanceOfSatisfying(S3CallFailedException.class,
                e -> assertThat(e.getHttpStatusCode()).isEqualTo(404));
    }

    @Test
    @DisplayName("headObject returns 200 or 404")
    void headObject() {
        // arrange
        createNewObjectForTest(BUCKET, "head.txt");
        // act
        final int found = s3Service.headObject(BUCKET, "head.txt");
        final int notFound = s3Service.headObject(BUCKET + "/nothing.txt");
        // assert
        assertThat(found).isEqualTo(200);
        assertThat(notFound).isEqualTo(404);
    }

    @Test
    @DisplayName("deleteObject removes the object")
    void deleteObject() {
        // arrange
        createNewObjectForTest(BUCKET, "delete.txt");
        // act
        final boolean deleted = s3Service.deleteObject(BUCKET, "delete.txt");
        // assert
        assertThat(deleted).isTrue();
        assertThat(s3Service.headObject(BUCKET, "delete.txt")).isEqualTo(404);
    }

    @Test
    @DisplayName("Key with leading slash is rejected")
    void leadingSlash() {
        // arrange
        final ByteArrayInputStream body = new ByteArrayInputStream(new byte[1]);
        // act + assert
        assertThatThrownBy(() -> s3Service.uploadSingleFile(BUCKET, "/abc", 1, DEFAULT_CONTENT_TYPE, body))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Forced failures")
    void forcedFailures() {
        // arrange
        final ByteArrayInputStream body = new ByteArrayInputStream(new byte[1]);
        s3Service.setForceFailOnS3Upload(true);
        s3Service.setForceFailOnS3Fetch(true);
        try {
            // act + assert
            assertThatThrownBy(() -> s3Service.uploadSingleFile(BUCKET, "x", 1, DEFAULT_CONTENT_TYPE, body))
                .isInstanceOf(S3CallFailedException.class);
            assertThatThrownBy(() -> s3Service.uploadMultipartFile(BUCKET, "x", DEFAULT_CONTENT_TYPE, body))
                .isInstanceOf(S3CallFailedException.class);
            assertThatThrownBy(() -> s3Service.fetch(BUCKET, "x")).isInstanceOf(S3CallFailedException.class);
        } finally {
            s3Service.setForceFailOnS3Upload(false);
            s3Service.setForceFailOnS3Fetch(false);
        }
    }

    @Test
    @DisplayName("splitBucketAndKey")
    void splitBucketAndKey() {
        // act
        final String[] pair = S3Service.splitBucketAndKey("bucket/a/b.txt");
        // assert
        assertThat(pair).containsExactly("bucket", "a/b.txt");
        assertThatThrownBy(() -> S3Service.splitBucketAndKey("/a")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> S3Service.splitBucketAndKey("a")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> S3Service.splitBucketAndKey("")).isInstanceOf(IllegalArgumentException.class);
    }
}
