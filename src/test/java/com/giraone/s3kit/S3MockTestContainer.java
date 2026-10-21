package com.giraone.s3kit;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Creates Adobe S3Mock containers for the S3 integration tests.
 */
public final class S3MockTestContainer {

    public static final String ACCESS_KEY = "testAccessKey";
    public static final String SECRET_KEY = "testSecretKey";

    private static final int HTTP_PORT = 9000;
    private static final DockerImageName IMAGE = DockerImageName.parse("docker.io/adobe/s3mock:5.2.3");

    private S3MockTestContainer() {
    }

    /**
     * Creates a container with a mapped HTTP port and waits for S3Mock to become ready.
     */
    public static GenericContainer<?> create() {
        return new GenericContainer<>(IMAGE)
            .withEnv("COM_ADOBE_TESTING_S3MOCK_HTTP_PORT", String.valueOf(HTTP_PORT))
            .withExposedPorts(HTTP_PORT)
            .waitingFor(Wait.forHttp("/favicon.ico").forPort(HTTP_PORT).forStatusCode(200));
    }

    /**
     * Returns the reachable HTTP endpoint for the given running container.
     */
    public static String endpoint(final GenericContainer<?> container) {
        return "http://" + container.getHost() + ":" + container.getMappedPort(HTTP_PORT);
    }
}
