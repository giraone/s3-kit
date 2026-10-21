package com.giraone.s3kit;

import com.giraone.s3kit.exceptions.S3CallFailedException;
import com.giraone.s3kit.model.ListObjectsResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3ServiceBucketTest extends AbstractS3ServiceTest {

    @Test
    void bucketLifecycle() {
        // act + assert
        assertThat(s3Service.headBucket("bucket-lifecycle")).isEqualTo(404);
        assertThat(s3Service.createBucketIfNotExists("bucket-lifecycle")).isTrue();
        assertThat(s3Service.createBucketIfNotExists("bucket-lifecycle")).isFalse();
        assertThat(s3Service.headBucket("bucket-lifecycle")).isEqualTo(200);
        assertThat(s3Service.listBuckets()).contains("bucket-lifecycle");
        assertThat(s3Service.updateBucketLifecycleConfig("bucket-lifecycle", 7)).isTrue();
        assertThat(s3Service.deleteBucket("bucket-lifecycle")).isTrue();
        assertThat(s3Service.headBucket("bucket-lifecycle")).isEqualTo(404);
    }

    @Test
    void deleteMissingBucketFails() {
        // act + assert
        assertThatThrownBy(() -> s3Service.deleteBucket("no-such-bucket"))
            .isInstanceOf(S3CallFailedException.class);
    }

    @Test
    void listObjectsWithPrefix() {
        // arrange
        s3Service.createBucket("list-bucket");
        createNewObjectForTest("list-bucket", "a/1.txt");
        createNewObjectForTest("list-bucket", "a/2.txt");
        createNewObjectForTest("list-bucket", "b/3.txt");
        // act
        final ListObjectsResult all = s3Service.listObjects("list-bucket", null);
        final ListObjectsResult onlyA = s3Service.listObjects("list-bucket", "a/");
        // assert
        assertThat(all.httpStatus()).isEqualTo(200);
        assertThat(all.keys()).containsExactlyInAnyOrder("a/1.txt", "a/2.txt", "b/3.txt");
        assertThat(onlyA.keys()).containsExactlyInAnyOrder("a/1.txt", "a/2.txt");
    }

    @Test
    void listObjectsPaginates() {
        // arrange
        s3Service.createBucket("page-bucket");
        for (int i = 0; i < 1100; i++) {
            createNewObjectForTest("page-bucket", String.format("k/%04d", i));
        }
        // act
        final ListObjectsResult result = s3Service.listObjects("page-bucket", "k/");
        // assert
        assertThat(result.keys()).hasSize(1100);
    }
}
