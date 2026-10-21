# s3kit - a useful S3 client lib based on AWS SDK

Java library for S3 access, based on the synchronous AWS SDK (with Apache5HttpClient) for Java (`S3Client`).
Large objects are **streamed** for upload and download and are never held completely in memory.

- Maven coordinates: `com.giraone.s3:s3kit:1.0.0-SNAPSHOT`
- Java module / base package: `com.giraone.s3kit`
- Java 25, Spring Boot 4.1.x; no Reactor / WebFlux

Install into the local Maven repository: `mvn clean install` (tests need Docker; they use `adobe/s3mock:5.2.3`).

```xml
<dependency>
  <groupId>com.giraone.s3</groupId>
  <artifactId>s3kit</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Usage

Import the configuration (`@Import(S3ClientConfiguration.class)`) and inject `S3Service`.
A `MeterRegistry` bean must be available.

```java
// Upload with known length (streamed)
s3Service.uploadSingleFile("bucket", "dir/file.bin", length, "application/octet-stream", inputStream);

// Upload of large/unknown length: part by part (multipart), one part in memory at a time
long bytes = s3Service.uploadMultipartFile("bucket", "dir/big.bin", "application/octet-stream", inputStream);

// Download as stream - must be closed
try (S3Download download = s3Service.fetch("bucket", "dir/big.bin")) {
    download.transferTo(outputStream);   // or read download.inputStream()
}
```

`S3Service` further offers `headObject`, `deleteObject`, `listObjects` (all pages), and bucket operations
(`headBucket`, `createBucket`, `createBucketIfNotExists`, `updateBucketLifecycleConfig`, `listBuckets`, `deleteBucket`).
All failures are reported as `S3CallFailedException` (with `getHttpStatusCode()`).
HTTP status results are plain `int` values (e.g. 200, 404).

## Configuration (prefix `s3-properties`)

| Property                         | Default        | Description                                                     |
|----------------------------------|----------------|-----------------------------------------------------------------|
| `endpoint`                       | -              | Endpoint override (e.g. S3Mock)                                 |
| `region`                         | `default`      | Region                                                          |
| `access-key`, `secret-key`       | -              | Static credentials; default AWS provider chain if blank         |
| `api-call-timeout`               | 60s            | Timeout of a call including retries                             |
| `api-call-attempt-timeout`       | 30s            | Timeout of a single attempt                                     |
| `socket-timeout`                 | 30s            | Socket timeout of the HTTP client                               |
| `connection-timeout`             | 2s             | Connect timeout                                                 |
| `connection-acquisition-timeout` | 60s            | Wait for a pooled connection                                    |
| `max-connections`                | min(2*CPU, 16) | HTTP connection pool size                                       |
| `max-attempts`                   | 1              | Total attempts (no retry by default)                            |
| `part-size`                      | 8 MiB          | Min size in bytes (min 5 MiB) for a part in a multipart request |
| `log-configuration-on-startup`   | false          | Log the configuration                                           |

## Metrics

`S3MicrometerMetricPublisher` publishes `s3kit.sdk.duration`, `s3kit.http.duration` and the HTTP client
concurrency gauges to Micrometer.

## Demo

See [s3-kit-usage](../s3-kit-usage) for a Spring Boot REST demo application using s3kit.
