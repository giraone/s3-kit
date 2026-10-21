package com.giraone.s3kit;

import com.giraone.s3kit.config.S3ClientConfigurationProperties;
import com.giraone.s3kit.download.S3Download;
import com.giraone.s3kit.exceptions.S3CallFailedException;
import com.giraone.s3kit.model.ListObjectsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.LifecycleRuleFilter;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Implementation of {@link S3Service} based on the synchronous AWS SDK {@link S3Client}.
 */
public class S3ServiceAwsSdkImpl implements S3Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ServiceAwsSdkImpl.class);

    private static final int HTTP_OK = 200;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_INTERNAL_SERVER_ERROR = 500;

    private final S3Client s3Client;
    private final int partSize;

    private volatile boolean failOnS3Upload;
    private volatile boolean failOnS3Fetch;

    public S3ServiceAwsSdkImpl(final S3Client s3Client) {
        this(s3Client, S3ClientConfigurationProperties.DEFAULT_PART_SIZE);
    }

    public S3ServiceAwsSdkImpl(final S3Client s3Client, final S3ClientConfigurationProperties s3props) {
        this(s3Client, s3props.getPartSize());
    }

    S3ServiceAwsSdkImpl(final S3Client s3Client, final int partSize) {
        if (partSize < S3ClientConfigurationProperties.MIN_PART_SIZE) {
            throw new IllegalArgumentException("partSize must be at least "
                + S3ClientConfigurationProperties.MIN_PART_SIZE + " bytes, but was " + partSize);
        }
        this.s3Client = s3Client;
        this.partSize = partSize;
    }

    // -- FETCH ----------------------------------------------------------------------------------------------------------

    @Override
    public S3Download fetch(final String bucketName, final String key) {

        LOGGER.debug("fetch: bucketName={}, key={}", bucketName, key);
        if (bucketName == null || bucketName.isEmpty()) {
            throw new IllegalArgumentException("bucketName may not be null or empty!");
        }
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key may not be null or empty!");
        }
        if (failOnS3Fetch) {
            throw new S3CallFailedException(HTTP_INTERNAL_SERVER_ERROR, "Forced S3 Exception by failOnS3Fetch");
        }
        final GetObjectRequest request = GetObjectRequest.builder().bucket(bucketName).key(key).build();
        try {
            final ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(request);
            final GetObjectResponse response = stream.response();
            final int status = response.sdkHttpResponse() == null ? HTTP_OK : response.sdkHttpResponse().statusCode();
            return new S3Download(bucketName, key, status, response.contentType(), response.contentLength(), stream);
        } catch (SdkServiceException e) {
            LOGGER.warn("S3 call for /{}/{} failed with {}", bucketName, key, e.getMessage());
            throw new S3CallFailedException(e);
        } catch (SdkException e) {
            throw new S3CallFailedException(e);
        }
    }

    @Override
    public S3Download fetch(final String bucketNameAndKey) {
        final String[] pair = S3Service.splitBucketAndKey(bucketNameAndKey);
        return fetch(pair[0], pair[1]);
    }

    // -- UPLOAD ---------------------------------------------------------------------------------------------------------

    @Override
    public String uploadSingleFile(
        final String bucketName, final String key, final long contentLength, final String contentType, final InputStream body) {

        LOGGER.debug("uploadSingleFile: bucketName={}, key={}, contentType={}, contentLength={}",
            bucketName, key, contentType, contentLength);
        validateKey(key);
        if (failOnS3Upload) {
            throw new S3CallFailedException(HTTP_INTERNAL_SERVER_ERROR, "Forced S3 Exception by failOnS3Upload");
        }
        final PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentLength(contentLength)
            .contentType(contentType)
            .build();
        try {
            final var response = s3Client.putObject(request, RequestBody.fromInputStream(body, contentLength));
            return S3CallFailedException.returnOrThrow(key, response);
        } catch (SdkServiceException e) {
            LOGGER.error("S3 call uploadSingleFile: bucketName={}, key={} failed.", bucketName, key, e);
            throw new S3CallFailedException(e);
        } catch (SdkException e) {
            LOGGER.error("S3 call uploadSingleFile: bucketName={}, key={} failed.", bucketName, key, e);
            throw new S3CallFailedException(e);
        }
    }

    @Override
    public long uploadMultipartFile(
        final String bucketName, final String key, final String contentType, final InputStream body) {

        LOGGER.debug("uploadMultipartFile: bucketName={}, key={}, contentType={}", bucketName, key, contentType);
        validateKey(key);
        if (failOnS3Upload) {
            throw new S3CallFailedException(HTTP_INTERNAL_SERVER_ERROR, "Forced S3 Exception by failOnS3Upload");
        }
        try {
            final byte[] firstPart = body.readNBytes(partSize);
            if (firstPart.length < partSize) {
                // everything fits into one part: no multipart upload needed
                uploadSingleFile(bucketName, key, firstPart.length, contentType, new ByteArrayInputStream(firstPart));
                return firstPart.length;
            }
            return uploadParts(bucketName, key, contentType, body, firstPart);
        } catch (IOException e) {
            throw new S3CallFailedException(HTTP_INTERNAL_SERVER_ERROR, "Reading the upload stream failed", e);
        }
    }

    private long uploadParts(
        final String bucketName, final String key, final String contentType,
        final InputStream body, final byte[] firstPart) {

        final CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
            .bucket(bucketName).key(key).contentType(contentType).build();
        final String uploadId;
        try {
            uploadId = s3Client.createMultipartUpload(createRequest).uploadId();
        } catch (SdkServiceException e) {
            throw new S3CallFailedException(e);
        } catch (SdkException e) {
            throw new S3CallFailedException(e);
        }
        try {
            final List<CompletedPart> parts = new ArrayList<>();
            long total = 0;
            byte[] current = firstPart;
            int partNo = 1;
            while (current.length > 0) {
                LOGGER.debug("uploadPart: bucket={}, key={}, uploadId={}, partNo={}, size={}",
                    bucketName, key, uploadId, partNo, current.length);
                final UploadPartRequest partRequest = UploadPartRequest.builder()
                    .bucket(bucketName).key(key).uploadId(uploadId).partNumber(partNo).contentLength((long) current.length)
                    .build();
                final String eTag = s3Client.uploadPart(partRequest, RequestBody.fromBytes(current)).eTag();
                parts.add(CompletedPart.builder().partNumber(partNo).eTag(eTag).build());
                total += current.length;
                partNo++;
                current = current.length < partSize ? new byte[0] : body.readNBytes(partSize);
            }
            final CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(bucketName).key(key).uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                .build();
            S3CallFailedException.onErrorThrow(s3Client.completeMultipartUpload(completeRequest));
            return total;
        } catch (RuntimeException | IOException e) {
            LOGGER.error("S3 call uploadMultipartFile: bucketName={}, key={} failed.", bucketName, key, e);
            abortQuietly(bucketName, key, uploadId);
            if (e instanceof SdkServiceException sse) {
                throw new S3CallFailedException(sse);
            } else if (e instanceof SdkException se) {
                throw new S3CallFailedException(se);
            } else if (e instanceof IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
            throw (RuntimeException) e;
        }
    }

    private void abortQuietly(final String bucketName, final String key, final String uploadId) {
        try {
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                .bucket(bucketName).key(key).uploadId(uploadId).build());
        } catch (SdkException e) {
            LOGGER.warn("abortMultipartUpload uploadId={} failed", uploadId, e);
        }
    }

    // -- DELETE / HEAD / LIST -------------------------------------------------------------------------------------------

    @Override
    public boolean deleteObject(final String bucketName, final String key) {
        LOGGER.debug("deleteObject: bucketName={}, key={}", bucketName, key);
        final DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(bucketName).key(key).build();
        return call(() -> {
            S3CallFailedException.onErrorThrow(s3Client.deleteObject(request),
                "object \"/%s/%s\" not deleted ", bucketName, key);
            return true;
        });
    }

    @Override
    public int headObject(final String bucketName, final String key) {
        LOGGER.debug("headObject: bucketName={}, key={}", bucketName, key);
        final HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucketName).key(key).build();
        try {
            return statusOf(s3Client.headObject(request).sdkHttpResponse());
        } catch (S3Exception e) {
            if (e.statusCode() == HTTP_NOT_FOUND) {
                return HTTP_NOT_FOUND;
            }
            throw new S3CallFailedException(e);
        } catch (SdkException e) {
            throw new S3CallFailedException(e);
        }
    }

    @Override
    public int headObject(final String bucketNameAndKey) {
        final String[] pair = S3Service.splitBucketAndKey(bucketNameAndKey);
        return headObject(pair[0], pair[1]);
    }

    @Override
    public ListObjectsResult listObjects(final String bucketName, final String prefix) {
        final ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder().bucket(bucketName);
        if (prefix != null) {
            builder.prefix(prefix);
        }
        return call(() -> {
            final List<String> keys = new ArrayList<>();
            int status = HTTP_OK;
            boolean first = true;
            for (final ListObjectsV2Response page : s3Client.listObjectsV2Paginator(builder.build())) {
                if (first) {
                    status = statusOf(page.sdkHttpResponse());
                    first = false;
                }
                if (page.hasContents()) {
                    page.contents().stream().map(S3Object::key).forEach(keys::add);
                }
            }
            LOGGER.debug("S3 listObjects for /{}/{} returned {} keys", bucketName, prefix, keys.size());
            return new ListObjectsResult(status, List.copyOf(keys));
        });
    }

    // -- BUCKET MANAGEMENT ----------------------------------------------------------------------------------------------

    @Override
    public int headBucket(final String bucketName) {
        final HeadBucketRequest request = HeadBucketRequest.builder().bucket(bucketName).build();
        try {
            final int status = statusOf(s3Client.headBucket(request).sdkHttpResponse());
            LOGGER.debug("headBucket: bucket=\"{}\" statusCode={}", bucketName, status);
            return status;
        } catch (S3Exception e) {
            if (e.statusCode() == HTTP_NOT_FOUND) {
                return HTTP_NOT_FOUND;
            }
            throw new S3CallFailedException(e);
        } catch (SdkException e) {
            throw new S3CallFailedException(e);
        }
    }

    @Override
    public boolean createBucketIfNotExists(final String bucketName) {
        final int status = headBucket(bucketName);
        if (status == HTTP_NOT_FOUND) {
            return createBucket(bucketName);
        }
        return status != HTTP_OK && status != 201;
    }

    @Override
    public boolean createBucket(final String bucketName) {
        LOGGER.debug("createBucket \"{}\"", bucketName);
        final CreateBucketRequest request = CreateBucketRequest.builder()
            .bucket(bucketName)
            .createBucketConfiguration(builder -> builder.locationConstraint(""))
            .build();
        return call(() -> {
            S3CallFailedException.onErrorThrow(s3Client.createBucket(request), "bucket_not_created %s", bucketName);
            LOGGER.info("createBucket: bucket=\"{}\" CREATED", bucketName);
            return true;
        });
    }

    @Override
    public boolean updateBucketLifecycleConfig(final String bucketName, final int retentionDays) {
        LOGGER.debug("updateBucketLifecycleConfig \"{}\" with retention {} days", bucketName, retentionDays);
        final LifecycleRule lifecycleRule = LifecycleRule.builder()
            .id("bucket_retention_policy")
            .status(ExpirationStatus.ENABLED)
            .filter(LifecycleRuleFilter.builder().build())
            .expiration(builder -> builder.days(retentionDays))
            .build();
        final PutBucketLifecycleConfigurationRequest request = PutBucketLifecycleConfigurationRequest.builder()
            .bucket(bucketName)
            .lifecycleConfiguration(builder -> builder.rules(lifecycleRule))
            .build();
        return call(() -> {
            S3CallFailedException.onErrorThrow(s3Client.putBucketLifecycleConfiguration(request),
                "lifecycle_not_updated %s", bucketName);
            LOGGER.info("updateBucketLifecycleConfig: bucket=\"{}\" UPDATED", bucketName);
            return true;
        });
    }

    @Override
    public List<String> listBuckets() {
        return call(() -> s3Client.listBuckets().buckets().stream()
            .map(Bucket::name)
            .toList());
    }

    @Override
    public boolean deleteBucket(final String bucketName) {
        final DeleteBucketRequest request = DeleteBucketRequest.builder().bucket(bucketName).build();
        return call(() -> {
            S3CallFailedException.onErrorThrow(s3Client.deleteBucket(request), "bucket_not_deleted %s", bucketName);
            return true;
        });
    }

    // -- TEST HELPERS ---------------------------------------------------------------------------------------------------

    @Override
    public void setForceFailOnS3Upload(final boolean newValue) {
        failOnS3Upload = newValue;
    }

    @Override
    public void setForceFailOnS3Fetch(final boolean newValue) {
        failOnS3Fetch = newValue;
    }

    // -- HELPERS --------------------------------------------------------------------------------------------------------

    private static <T> T call(final Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (SdkServiceException e) {
            throw new S3CallFailedException(e);
        } catch (SdkException e) {
            throw new S3CallFailedException(e);
        }
    }

    private static int statusOf(final SdkHttpResponse response) {
        return response != null ? response.statusCode() : 503;
    }

    private static void validateKey(final String key) {
        if (key.startsWith(PATH_DELIMITER)) {
            throw new IllegalArgumentException("An S3 key should not start with a slash (/), but \"" + key + "\" does!");
        }
    }
}
