package com.giraone.s3kit.download;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class S3DownloadTest {

    @Test
    void fileName() {
        // arrange
        final S3Download download = new S3Download("b", "a/b/c.txt", 200, "text/plain", 1L, new ByteArrayInputStream(new byte[0]));
        // act + assert
        assertThat(download.fileName()).isEqualTo("c.txt");
    }

    @Test
    void fileNameWithoutFolder() {
        // arrange
        final S3Download download = new S3Download("b", "c.txt", 200, null, null, new ByteArrayInputStream(new byte[0]));
        // act + assert
        assertThat(download.fileName()).isEqualTo("c.txt");
    }

    @Test
    void transferTo() throws IOException {
        // arrange
        final S3Download download = new S3Download("b", "k", 200, null, 3L, new ByteArrayInputStream(new byte[]{1, 2, 3}));
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        // act
        final long n = download.transferTo(out);
        // assert
        assertThat(n).isEqualTo(3);
        assertThat(out.toByteArray()).containsExactly(1, 2, 3);
    }
}
