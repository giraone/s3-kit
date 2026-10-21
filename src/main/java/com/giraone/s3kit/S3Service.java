package com.giraone.s3kit;

import com.giraone.s3kit.download.S3Download;
import com.giraone.s3kit.model.ListObjectsResult;

import java.io.InputStream;
import java.util.List;

/**
 * Non-reactive S3 service. Large objects are streamed: uploads read from an {@link InputStream}
 * and downloads are returned as {@link S3Download} wrapping an {@link InputStream}.
 * All methods throw {@link com.giraone.s3kit.exceptions.S3CallFailedException} on failures.
 */
public interface S3Service {

    String PATH_DELIMITER = "/";

    // -- FETCH ----------------------------------------------------------------------------------------------------------

    /**
     * Download an object. The caller must close the result.
     */
    S3Download fetch(String bucketName, String key);

    /**
     * Download an object, given as "bucket/key". The caller must close the result.
     */
    S3Download fetch(String bucketNameAndKey);

    // -- UPLOAD ---------------------------------------------------------------------------------------------------------

    /**
     * Stores an object with known length in a single request, streaming from the given input stream.
     * The input stream is not closed.
     *
     * @return the key on success
     */
    String uploadSingleFile(String bucketName, String key, long contentLength, String contentType, InputStream body);

    /**
     * Stores an object of unknown or large length with a multipart upload. The stream is read part by part,
     * so only one part is held in memory. The input stream is not closed.
     *
     * @return the total size of the uploaded object in bytes
     */
    long uploadMultipartFile(String bucketName, String key, String contentType, InputStream body);

    // -- OBJECT MANAGEMENT ----------------------------------------------------------------------------------------------

    /**
     * @return true on success
     */
    boolean deleteObject(String bucketName, String key);

    /**
     * @return 200 if the object exists, 404 if not found
     */
    int headObject(String bucketName, String key);

    int headObject(String bucketNameAndKey);

    /**
     * List objects with automatic pagination over all pages.
     */
    ListObjectsResult listObjects(String bucketName, String prefix);

    // -- BUCKET MANAGEMENT ----------------------------------------------------------------------------------------------

    /**
     * @return 200 if the bucket exists, 404 if not found
     */
    int headBucket(String bucketName);

    boolean createBucket(String bucketName);

    /**
     * @return true if newly created, false if it already existed
     */
    boolean createBucketIfNotExists(String bucketName);

    boolean updateBucketLifecycleConfig(String bucketName, int retentionDays);

    List<String> listBuckets();

    boolean deleteBucket(String bucketName);

    // -- TEST HELPERS ---------------------------------------------------------------------------------------------------

    /** ONLY USE IN UNIT TESTS. Forces upload calls to fail without calling S3. */
    void setForceFailOnS3Upload(boolean newValue);

    /** ONLY USE IN UNIT TESTS. Forces fetch calls to fail without calling S3. */
    void setForceFailOnS3Fetch(boolean newValue);

    // -- UTILITIES ------------------------------------------------------------------------------------------------------

    static String[] splitBucketAndKey(final String bucketAndKey) {
        if (bucketAndKey == null || bucketAndKey.isEmpty()) {
            throw new IllegalArgumentException("bucketAndKey may not be null or empty!");
        }
        if (bucketAndKey.charAt(0) == '/') {
            throw new IllegalArgumentException("bucketAndKey must not start with /!");
        }
        final int firstSlash = bucketAndKey.indexOf('/');
        if (firstSlash <= 0) {
            throw new IllegalArgumentException("bucketAndKey \"" + bucketAndKey + "\" must be separated by slash!");
        }
        return new String[]{bucketAndKey.substring(0, firstSlash), bucketAndKey.substring(firstSlash + 1)};
    }
}
