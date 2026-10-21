package com.giraone.s3kit.model;

import java.util.List;

/**
 * Result of a list objects call.
 *
 * @param httpStatus the HTTP status code of the (first) response
 * @param keys       all matching S3 keys
 */
public record ListObjectsResult(int httpStatus, List<String> keys) {
}
