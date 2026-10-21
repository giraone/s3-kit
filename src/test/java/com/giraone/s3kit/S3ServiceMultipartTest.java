package com.giraone.s3kit;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3ServiceMultipartTest extends AbstractS3ServiceTest {

    private static final String BUCKET = "multipart-test-bucket";
    private static final int MIB = 1024 * 1024;

    @Override
    @BeforeAll
    void init() {
        super.init();
        s3Service.createBucket(BUCKET);
    }

    /** Generates content on the fly, so the test itself never holds the complete content. */
    private static InputStream generated(final long size) {
        return new InputStream() {
            private long pos;

            @Override
            public int read() {
                return pos < size ? 'a' + (int) (pos++ % 26) : -1;
            }

            @Override
            public int read(final byte @NonNull [] b, final int off, final int len) {
                if (pos >= size) {
                    return -1;
                }
                final int n = (int) Math.min(len, size - pos);
                for (int i = 0; i < n; i++) {
                    b[off + i] = (byte) ('a' + (int) ((pos + i) % 26));
                }
                pos += n;
                return n;
            }
        };
    }

    @Test
    @DisplayName("Upload of 12 MiB uses 2 parts and can be downloaded again")
    void uploadTwoParts() throws IOException {
        // arrange
        final long size = 12L * MIB;
        // act
        final long result = s3Service.uploadMultipartFile(BUCKET, "two-parts.bin", DEFAULT_CONTENT_TYPE, generated(size));
        // assert
        assertThat(result).isEqualTo(size);
        try (InputStream in = s3Service.fetch(BUCKET, "two-parts.bin").inputStream();
             InputStream expected = generated(size)) {
            assertThat(in.readAllBytes()).isEqualTo(expected.readAllBytes());
        }
    }

    @Test
    @DisplayName("Upload with exactly a multiple of the part size")
    void uploadExactParts() {
        // arrange
        final long size = 16L * MIB;
        // act
        final long result = s3Service.uploadMultipartFile(BUCKET, "exact.bin", DEFAULT_CONTENT_TYPE, generated(size));
        // assert
        assertThat(result).isEqualTo(size);
        assertThat(s3Service.fetch(BUCKET, "exact.bin").contentLength()).isEqualTo(size);
    }

    @Test
    @DisplayName("Small content does not need multipart")
    void oneSmallPart() {
        // arrange
        final byte[] content = "Request".getBytes();
        // act
        final long result = s3Service.uploadMultipartFile(
            BUCKET, "small.txt", DEFAULT_CONTENT_TYPE, new ByteArrayInputStream(content));
        // assert
        assertThat(result).isEqualTo(content.length);
    }

    @Test
    @DisplayName("Empty content")
    void empty() {
        // act
        final long result = s3Service.uploadMultipartFile(
            BUCKET, "empty.txt", DEFAULT_CONTENT_TYPE, new ByteArrayInputStream(new byte[0]));
        // assert
        assertThat(result).isZero();
        assertThat(s3Service.headObject(BUCKET, "empty.txt")).isEqualTo(200);
    }

    @Test
    @DisplayName("Part size below 5 MiB is rejected")
    void partSizeTooSmall() {
        // act + assert
        assertThatThrownBy(() -> new S3ServiceAwsSdkImpl(s3Client, 1024))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
