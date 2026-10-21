package com.giraone.s3kit.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "s3-properties", ignoreUnknownFields = false)
public class S3ClientConfigurationProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ClientConfigurationProperties.class);

    /** The minimum size of a part in a multipart upload (5 MiB), required by S3. */
    public static final int MIN_PART_SIZE = 5 * 1024 * 1024;
    /** The default size of a part in a multipart upload (8 MiB). */
    public static final int DEFAULT_PART_SIZE = 8 * 1024 * 1024;

    private static final int DEFAULT_MAX_ATTEMPTS = 1;
    private static final int DEFAULT_API_CALL_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_API_CALL_ATTEMPT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 2;
    private static final int DEFAULT_CONNECTION_ACQUISITION_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_SOCKET_TIMEOUT_SECONDS = 30;

    private boolean logConfigurationOnStartup;
    private String endpoint;
    private String region;
    private String accessKey;
    private String secretKey;
    /** Timeout for the entire call, including all retry attempts. */
    private Duration apiCallTimeout = Duration.ofSeconds(DEFAULT_API_CALL_TIMEOUT_SECONDS);
    /** Timeout for a single HTTP attempt. */
    private Duration apiCallAttemptTimeout = Duration.ofSeconds(DEFAULT_API_CALL_ATTEMPT_TIMEOUT_SECONDS);
    /** Socket read/write timeout of the HTTP client. */
    private Duration socketTimeout = Duration.ofSeconds(DEFAULT_SOCKET_TIMEOUT_SECONDS);
    private Duration connectionTimeout = Duration.ofSeconds(DEFAULT_CONNECTION_TIMEOUT_SECONDS);
    private Duration connectionAcquisitionTimeout = Duration.ofSeconds(DEFAULT_CONNECTION_ACQUISITION_TIMEOUT_SECONDS);
    /** Maximum number of pooled HTTP connections. */
    private int maxConnections = Math.min(Runtime.getRuntime().availableProcessors() * 2, 16);
    /** Maximum number of total attempts (including the initial attempt). */
    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
    /** Size in bytes of one part of a multipart upload. Only one part is held in memory. Minimum is 5 MiB. */
    private int partSize = DEFAULT_PART_SIZE;

    @PostConstruct
    void initialize() {
        if (logConfigurationOnStartup) {
            LOGGER.info("{}", this);
        }
    }

    public boolean isLogConfigurationOnStartup() {
        return logConfigurationOnStartup;
    }

    public void setLogConfigurationOnStartup(final boolean logConfigurationOnStartup) {
        this.logConfigurationOnStartup = logConfigurationOnStartup;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(final String endpoint) {
        this.endpoint = endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(final String region) {
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(final String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(final String secretKey) {
        this.secretKey = secretKey;
    }

    public Duration getApiCallTimeout() {
        return apiCallTimeout;
    }

    public void setApiCallTimeout(final Duration apiCallTimeout) {
        this.apiCallTimeout = apiCallTimeout;
    }

    public Duration getApiCallAttemptTimeout() {
        return apiCallAttemptTimeout;
    }

    public void setApiCallAttemptTimeout(final Duration apiCallAttemptTimeout) {
        this.apiCallAttemptTimeout = apiCallAttemptTimeout;
    }

    public Duration getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(final Duration socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(final Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Duration getConnectionAcquisitionTimeout() {
        return connectionAcquisitionTimeout;
    }

    public void setConnectionAcquisitionTimeout(final Duration connectionAcquisitionTimeout) {
        this.connectionAcquisitionTimeout = connectionAcquisitionTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(final int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(final int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getPartSize() {
        return partSize;
    }

    public void setPartSize(final int partSize) {
        this.partSize = partSize;
    }

    @Override
    public String toString() {
        return "S3ClientConfigurationProperties{" +
            "logConfigurationOnStartup=" + logConfigurationOnStartup +
            ", endpoint='" + endpoint + '\'' +
            ", region='" + region + '\'' +
            ", accessKey.length=" + (accessKey != null ? accessKey.length() : "null") +
            ", secretKey.length=" + (secretKey != null ? secretKey.length() : "null") +
            ", apiCallTimeout=" + apiCallTimeout +
            ", apiCallAttemptTimeout=" + apiCallAttemptTimeout +
            ", socketTimeout=" + socketTimeout +
            ", connectionTimeout=" + connectionTimeout +
            ", connectionAcquisitionTimeout=" + connectionAcquisitionTimeout +
            ", maxConnections=" + maxConnections +
            ", maxAttempts=" + maxAttempts +
            ", partSize=" + partSize +
            '}';
    }
}
