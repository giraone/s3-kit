package com.giraone.s3kit.exceptions;

import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Response;

import java.io.Serial;

public class S3CallFailedException extends RuntimeException {

    private static final int INTERNAL_SERVER_ERROR = 500;

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * HTTP Code or -1 on any non HTTP related other errors
     */
    private final int httpStatusCode;

    public S3CallFailedException(SdkResponse response) {
        super(buildMessage(response));
        this.httpStatusCode = buildCode(response);
    }

    public S3CallFailedException(SdkResponse response, String message) {
        super(message + ": " + buildMessage(response));
        this.httpStatusCode = buildCode(response);
    }

    public S3CallFailedException(int httpStatusCode, String message) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    public S3CallFailedException(int httpStatusCode, String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
    }

    public S3CallFailedException(String message, S3Exception cause) {
        super(message, cause);
        this.httpStatusCode = cause.statusCode();
    }

    public S3CallFailedException(SdkServiceException cause) {
        super(cause.getMessage(), cause);
        this.httpStatusCode = cause.statusCode();
    }

    public S3CallFailedException(SdkException cause) {
        super(cause.getMessage(), cause);
        this.httpStatusCode = INTERNAL_SERVER_ERROR;
    }

    /**
     * HTTP Code or -1 on any non HTTP related other errors
     */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    // Helper used to check return codes from an API call
    public static void onErrorThrow(S3Response response, String additionalMessage, Object... args) {

        final SdkHttpResponse sdkResponse = response.sdkHttpResponse();
        if (sdkResponse != null && sdkResponse.isSuccessful()) {
            return;
        }
        throw new S3CallFailedException(response, String.format(additionalMessage, args));
    }

    public static void onErrorThrow(S3Response response) {

        final SdkHttpResponse sdkResponse = response.sdkHttpResponse();
        if (sdkResponse != null && sdkResponse.isSuccessful()) {
            return;
        }
        throw new S3CallFailedException(response);
    }

    public static <T> T returnOrThrow(T returnValue, S3Response response) {

        final SdkHttpResponse sdkResponse = response.sdkHttpResponse();
        if (sdkResponse != null && sdkResponse.isSuccessful()) {
            return returnValue;
        }
        throw new S3CallFailedException(response);
    }

    private static String buildMessage(SdkResponse response) {
        if (response == null) {
            return "Error: SDK response was null";
        } else {
            final SdkHttpResponse httpResponse = response.sdkHttpResponse();
            if (httpResponse != null) {
                return httpResponse.statusText().orElse("Error " + httpResponse.statusCode());
            } else {
                return "Error: No HTTP response returned for " + response.getClass().getName();
            }
        }
    }

    private static int buildCode(SdkResponse response) {
        if (response == null) {
            return INTERNAL_SERVER_ERROR;
        } else {
            final SdkHttpResponse httpResponse = response.sdkHttpResponse();
            if (httpResponse != null) {
                return httpResponse.statusCode();
            } else {
                return INTERNAL_SERVER_ERROR;
            }
        }
    }
}
