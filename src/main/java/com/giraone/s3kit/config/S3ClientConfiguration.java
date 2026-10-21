package com.giraone.s3kit.config;

import com.giraone.s3kit.S3Service;
import com.giraone.s3kit.S3ServiceAwsSdkImpl;
import com.giraone.s3kit.metrics.S3MicrometerMetricPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.utils.StringUtils;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(S3ClientConfigurationProperties.class)
public class S3ClientConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ClientConfiguration.class);

    @Bean
    public S3Service s3Service(
        final S3ClientConfigurationProperties s3props, final S3Client s3Client) {
        return new S3ServiceAwsSdkImpl(s3Client, s3props);
    }

    @Bean(destroyMethod = "close")
    public S3Client s3Client(
        final S3ClientConfigurationProperties s3props,
        final AwsCredentialsProvider credentialsProvider,
        final MeterRegistry meterRegistry) {

        System.setProperty(SdkSystemSetting.AWS_REQUEST_CHECKSUM_CALCULATION.property(), "WHEN_REQUIRED");
        final SdkHttpClient httpClient = Apache5HttpClient.builder()
            .socketTimeout(s3props.getSocketTimeout())
            .connectionTimeout(s3props.getConnectionTimeout())
            .connectionAcquisitionTimeout(s3props.getConnectionAcquisitionTimeout())
            .maxConnections(s3props.getMaxConnections())
            .build();
        S3ClientBuilder builder = S3Client.builder()
            .httpClient(httpClient)
            .region(Region.of(s3props.getRegion() == null ? "default" : s3props.getRegion()))
            .credentialsProvider(credentialsProvider)
            .forcePathStyle(true)
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                .apiCallTimeout(s3props.getApiCallTimeout())
                .apiCallAttemptTimeout(s3props.getApiCallAttemptTimeout())
                .addMetricPublisher(new S3MicrometerMetricPublisher(meterRegistry))
                .retryStrategy(AwsRetryStrategy.standardRetryStrategy()
                    .toBuilder()
                    .maxAttempts(s3props.getMaxAttempts())
                    .build())
                .build());
        if (s3props.getEndpoint() != null) {
            builder = builder.endpointOverride(URI.create(s3props.getEndpoint()));
        }
        final S3Client client = builder.build();
        LOGGER.debug("{} was built", client.getClass());
        return client;
    }

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(final S3ClientConfigurationProperties s3props) {
        if (StringUtils.isBlank(s3props.getAccessKey())) {
            return DefaultCredentialsProvider.builder().build();
        }
        return () -> AwsBasicCredentials.create(s3props.getAccessKey(), s3props.getSecretKey());
    }
}
