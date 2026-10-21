package com.giraone.s3kit.exceptions;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.*;

class S3CallFailedExceptionTest {

    @Test
    void getHttpStatus404() {

        // arrange
        final SdkHttpResponse sdkHttpResponse = SdkHttpResponse.builder()
            .statusCode(404)
            .statusText("NOT FOUND")
            .build();
        final SdkResponse sdkResponse = VoidSdkResponse.builder().sdkHttpResponse(sdkHttpResponse).build();
        // act
        S3CallFailedException exception = new S3CallFailedException(sdkResponse);
        // assert
        assertThat(exception.getHttpStatusCode()).isEqualTo(404);
        assertThat(exception.getMessage()).isNotEmpty().contains("NOT FOUND");
    }

    @Test
    void getHttpStatusNoHttpResponse() {

        // arrange
        final SdkResponse sdkResponse = VoidSdkResponse.builder().build();
        // act
        S3CallFailedException exception = new S3CallFailedException(sdkResponse);
        // assert
        assertThat(exception.getHttpStatusCode()).isEqualTo(500);
        assertThat(exception.getMessage()).isNotEmpty().contains("Error: No HTTP response returned");
    }

    @Test
    void getHttpStatusNoResponse() {

        // act
        S3CallFailedException exception = new S3CallFailedException((SdkResponse) null);
        // assert
        assertThat(exception.getHttpStatusCode()).isEqualTo(500);
        assertThat(exception.getMessage()).isNotEmpty().contains("Error: SDK response was null");
    }

    @Test
    void onErrorThrowDoesNotThrowExceptionForSuccess() {

        // arrange
        final GetObjectResponse response = createGetObjectResponse(200);
        // act - must not throw an exception
        assertThatNoException().isThrownBy(() -> S3CallFailedException.onErrorThrow(response));
    }

    @Test
    void onErrorThrowExceptionForFailure() {

        // arrange
        final GetObjectResponse response = createGetObjectResponse(404, "NOT FOUND");
        // act/assert
        assertThatThrownBy(() -> S3CallFailedException.onErrorThrow(response))
            .isInstanceOf(S3CallFailedException.class)
            .hasMessage("NOT FOUND");
    }

    @Test
    void constructorResponseAndAdditionalMessage() {

        // arrange
        final SdkResponse sdkResponse = VoidSdkResponse.builder()
            .sdkHttpResponse(SdkHttpResponse.builder().statusCode(503).statusText("SERVICE UNAVAILABLE").build())
            .build();
        // act
        final S3CallFailedException exception = new S3CallFailedException(sdkResponse, "Failed to read object");
        // assert
        assertThat(exception.getHttpStatusCode()).isEqualTo(503);
        assertThat(exception.getMessage()).isEqualTo("Failed to read object: SERVICE UNAVAILABLE");
    }

    @Test
    void constructorResponseWithoutStatusText() {

        // arrange
        final SdkResponse sdkResponse = VoidSdkResponse.builder()
            .sdkHttpResponse(SdkHttpResponse.builder().statusCode(418).build())
            .build();
        // act
        final S3CallFailedException exception = new S3CallFailedException(sdkResponse);
        // assert
        assertThat(exception.getHttpStatusCode()).isEqualTo(418);
        assertThat(exception.getMessage()).isEqualTo("Error 418");
    }

    @Test
    void constructorHttpStatusCodeAndMessage() {

        // act
        final S3CallFailedException exception = new S3CallFailedException(409, "Conflict");
        // assert
        assertThat(exception.getHttpStatusCode()).isEqualTo(409);
        assertThat(exception.getMessage()).isEqualTo("Conflict");
    }

    @Test
    void constructorHttpStatusCodeMessageAndCause() {

        // arrange
        final IllegalStateException cause = new IllegalStateException("boom");
        // act
        final S3CallFailedException exception = new S3CallFailedException(504, "Gateway timeout", cause);
        // assert
        assertThat(exception.getHttpStatusCode()).isEqualTo(504);
        assertThat(exception.getMessage()).isEqualTo("Gateway timeout");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructorMessageAndS3Exception() {

        // arrange
        final S3Exception cause = (S3Exception) S3Exception.builder()
            .statusCode(403)
            .message("Forbidden")
            .build();
        // act
        final S3CallFailedException exception = new S3CallFailedException("Cannot access object", cause);
        // assert
        assertThat(exception.getHttpStatusCode()).isEqualTo(403);
        assertThat(exception.getMessage()).isEqualTo("Cannot access object");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructorSdkServiceException() {

        // arrange
        final SdkServiceException cause = S3Exception.builder()
            .statusCode(429)
            .message("Too many requests")
            .build();
        // act
        final S3CallFailedException exception = new S3CallFailedException(cause);
        // assert
        assertThat(exception.getHttpStatusCode()).isEqualTo(429);
        assertThat(exception.getMessage()).isEqualTo("Too many requests");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructorSdkExceptionUsesInternalServerError() {

        // arrange
        final SdkClientException cause = SdkClientException.create("Client side failure");
        // act
        final S3CallFailedException exception = new S3CallFailedException(cause);
        // assert
        assertThat(exception.getHttpStatusCode()).isEqualTo(500);
        assertThat(exception.getMessage()).isEqualTo("Client side failure");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void onErrorThrowWithAdditionalMessageThrowsForFailure() {

        // arrange
        final GetObjectResponse response = createGetObjectResponse(404, "NOT FOUND");
        // act/assert
        assertThatThrownBy(() -> S3CallFailedException.onErrorThrow(response, "Object %s not available", "abc.txt"))
            .isInstanceOf(S3CallFailedException.class)
            .hasMessage("Object abc.txt not available: NOT FOUND");
    }

    @Test
    void onErrorThrowWithAdditionalMessageDoesNotThrowForSuccess() {

        // arrange
        final GetObjectResponse response = createGetObjectResponse(200, "OK");
        // act/assert
        assertThatNoException().isThrownBy(() -> S3CallFailedException.onErrorThrow(response, "ignored %s", "message"));
    }

    @Test
    void returnOrThrowReturnsValueForSuccess() {

        // arrange
        final String expected = "payload";
        final GetObjectResponse response = createGetObjectResponse(200, "OK");
        // act
        final String actual = S3CallFailedException.returnOrThrow(expected, response);
        // assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void returnOrThrowThrowsForFailure() {

        // arrange
        final GetObjectResponse response = createGetObjectResponse(500, "SERVER ERROR");
        // act/assert
        assertThatThrownBy(() -> S3CallFailedException.returnOrThrow("ignored", response))
            .isInstanceOf(S3CallFailedException.class)
            .hasMessage("SERVER ERROR");
    }

    @SuppressWarnings("SameParameterValue")
    private GetObjectResponse createGetObjectResponse(int statusCode) {
        final SdkHttpFullResponse sdkHttpResponse = SdkHttpResponse.builder().statusCode(statusCode).build();
        return (GetObjectResponse) GetObjectResponse.builder().sdkHttpResponse(sdkHttpResponse).build();
    }

    private GetObjectResponse createGetObjectResponse(int statusCode, String statusText) {
        final SdkHttpFullResponse sdkHttpResponse = SdkHttpResponse.builder().statusCode(statusCode).statusText(statusText).build();
        return (GetObjectResponse) GetObjectResponse.builder().sdkHttpResponse(sdkHttpResponse).build();
    }
}
