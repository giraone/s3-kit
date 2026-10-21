package com.giraone.s3kit.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class S3MicrometerMetricPublisher implements MetricPublisher {
    private static final String OPERATION_NAME = "OperationName";
    private static final String API_CALL_SUCCESSFUL = "ApiCallSuccessful";
    private static final String RETRY_COUNT = "RetryCount";
    private static final String HTTP_STATUS_CODE = "HttpStatusCode";
    // The total time taken to finish a request (inclusive of all retries)
    private static final String API_CALL_DURATION = "ApiCallDuration";
    // The time it takes to connect to the service, send the request, and receive the HTTP status code and header from the response
    private static final String SERVICE_CALL_DURATION = "ServiceCallDuration";
    private final MeterRegistry registry;
    // Gauges must be registered once and updated on every publish() call.
    // Micrometer keeps a weak reference to these objects, so they must be held as fields.
    private final AtomicInteger availableConcurrency = new AtomicInteger(0);
    private final AtomicInteger leasedConcurrency = new AtomicInteger(0);
    private final AtomicInteger maxConcurrency = new AtomicInteger(0);
    private final AtomicInteger pendingConcurrencyAcquires = new AtomicInteger(0);

    /**
     * Create a publisher for AWS S3 SDK metrics, that are published to io.micrometer registries.
     * @param registry io.micrometer registry for exposing metrics
     */
    public S3MicrometerMetricPublisher(MeterRegistry registry) {
        this.registry = registry;
        Gauge.builder("s3kit.http.available.concurrency", availableConcurrency::get)
            .description("The number of additional concurrent requests that the HTTP client supports without establishing new connections to the target server.")
            .register(registry);
        Gauge.builder("s3kit.http.leased.concurrency", leasedConcurrency::get)
            .description("The number of requests that the HTTP client currently executes.")
            .register(registry);
        Gauge.builder("s3kit.http.max.concurrency", maxConcurrency::get)
            .description("The maximum number of concurrent requests that the HTTP client supports.")
            .register(registry);
        Gauge.builder("s3kit.http.pending.concurrency.acquires", pendingConcurrencyAcquires::get)
            .description("The number of requests that wait for concurrency from the HTTP client.")
            .register(registry);
    }

    /**
     * Each S3-Api-Call generates a set of metrics. The full list of available metrics is
     * <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/metrics-list.html">here</a>.
     * One S3-Api-Call can have multiple S3-Request-Attempts.
     * This method is called **after each S3-Api-Call**.
     */
    @Override
    public void publish(MetricCollection apiCallMetrics) {
        publishSdkDuration(apiCallMetrics);
        publishHttpDuration(apiCallMetrics);
        publishConcurrency(apiCallMetrics);
    }

    /**
     * Measure duration of all S3-Api-Call (inclusive of all retries).
     * Tag S3-Api-Call Retries.
     * Tag Failed S3-Api-Calls, which reached max S3-Request-Attempts.
     */
    private void publishSdkDuration(MetricCollection apiCallMetrics) {
        if (contains(apiCallMetrics, List.of(OPERATION_NAME, API_CALL_DURATION, API_CALL_SUCCESSFUL, RETRY_COUNT))) {
            Timer.builder("s3kit.sdk.duration")
                .description("Duration of s3-API-Call (inclusive of all retries)")
                .tags(
                    OPERATION_NAME, (String) getValue(apiCallMetrics, OPERATION_NAME),
                    API_CALL_SUCCESSFUL, getValue(apiCallMetrics, API_CALL_SUCCESSFUL).toString(),
                    RETRY_COUNT, getValue(apiCallMetrics, RETRY_COUNT).toString()
                )
                .register(registry)
                .record((Duration) getValue(apiCallMetrics, API_CALL_DURATION));
        }
    }

    /**
     * Measure duration of all S3-Request-Attempts (retries count as separate attempts).
     */
    private void publishHttpDuration(MetricCollection apiCallMetrics) {
        if (contains(apiCallMetrics, List.of(OPERATION_NAME))) {
            for (MetricCollection apiCallAttemptMetrics : apiCallMetrics.children()) {
                if (contains(apiCallAttemptMetrics, List.of(HTTP_STATUS_CODE, SERVICE_CALL_DURATION))) {
                    Timer.builder("s3kit.http.duration")
                        .description("Duration of s3-Request-Attempts (retries will count as separate attempts)")
                        .tags(
                            OPERATION_NAME, (String) getValue(apiCallMetrics, OPERATION_NAME),
                            HTTP_STATUS_CODE, getValue(apiCallAttemptMetrics, HTTP_STATUS_CODE).toString()
                        )
                        .register(registry)
                        .record((Duration) getValue(apiCallAttemptMetrics, SERVICE_CALL_DURATION));
                }
            }
        }
    }

    /**
     * Concurrency metrics are reported by the HTTP client inside an "HttpClient" child
     * collection, which is itself a child of each "ApiCallAttempt" collection. The full path is:
     * ApiCall → ApiCallAttempt → HttpClient → concurrency metrics.
     * <p>
     * The gauge AtomicIntegers are registered once at construction time and updated here on
     * every publish() call, which avoids Micrometer's duplicate-registration warning.
     */
    private void publishConcurrency(MetricCollection apiCallMetrics) {
        for (MetricCollection attemptMetrics : apiCallMetrics.children()) {
            for (MetricCollection httpClientMetrics : attemptMetrics.children()) {
                if (contains(httpClientMetrics, List.of("ConcurrencyAcquireDuration"))) {
                    Timer.builder("s3kit.http.concurrency.acquire")
                        .description("The duration of time to acquire a channel from the connection pool.")
                        .register(registry)
                        .record((Duration) getValue(httpClientMetrics, "ConcurrencyAcquireDuration"));
                }
                if (contains(httpClientMetrics, List.of("AvailableConcurrency"))) {
                    availableConcurrency.set((Integer) getValue(httpClientMetrics, "AvailableConcurrency"));
                }
                if (contains(httpClientMetrics, List.of("LeasedConcurrency"))) {
                    leasedConcurrency.set((Integer) getValue(httpClientMetrics, "LeasedConcurrency"));
                }
                if (contains(httpClientMetrics, List.of("MaxConcurrency"))) {
                    maxConcurrency.set((Integer) getValue(httpClientMetrics, "MaxConcurrency"));
                }
                if (contains(httpClientMetrics, List.of("PendingConcurrencyAcquires"))) {
                    pendingConcurrencyAcquires.set((Integer) getValue(httpClientMetrics, "PendingConcurrencyAcquires"));
                }
            }
        }
    }

    private static boolean contains(MetricCollection requestMetrics, List<String> metricNames) {
        return requestMetrics.stream()
            .filter(metricRecord -> metricNames.contains(metricRecord.metric().name()))
            .count() == metricNames.size();
    }

    private static Object getValue(MetricCollection requestMetrics, String metricName) {
        return requestMetrics.stream()
            .filter(metricRecord -> metricRecord.metric().name().equals(metricName))
            .findFirst()
            .orElseThrow()
            .value();
    }

    @Override
    public void close() {
        // no close need
    }
}
