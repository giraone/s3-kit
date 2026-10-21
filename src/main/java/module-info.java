module com.giraone.s3kit {
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires micrometer.core;
    requires org.slf4j;
    requires jakarta.annotation;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.awscore;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.http;
    requires software.amazon.awssdk.metrics;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.retries;
    requires software.amazon.awssdk.retries.api;
    requires software.amazon.awssdk.services.s3;
    requires software.amazon.awssdk.utils;
    requires software.amazon.awssdk.http.apache5;

    exports com.giraone.s3kit;
    exports com.giraone.s3kit.config;
    exports com.giraone.s3kit.download;
    exports com.giraone.s3kit.exceptions;
    exports com.giraone.s3kit.metrics;
    exports com.giraone.s3kit.model;

    opens com.giraone.s3kit.config to spring.beans, spring.context;
}
