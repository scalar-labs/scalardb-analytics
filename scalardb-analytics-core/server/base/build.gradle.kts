/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("build-logic.java-common-conventions")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":api"))
    implementation(project(":domain"))
    implementation(project(":repository"))
    implementation(project(":repository-impl-spring-data-jdbc"))
    implementation(project(":service"))
    implementation(project(":usecase"))
    implementation(project(":usecase-impl"))
    implementation(project(":grpc"))
    implementation(project(":schema-resolver"))

    // Spring Boot
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.data.jdbc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.jackson2)
    implementation(libs.spring.security.crypto)
    implementation(libs.javax.annotation)
    implementation(libs.jakarta.annotation)
    
    // MapStruct
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    // gRPC with Spring Boot
    implementation(libs.grpc.netty)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.core)
    implementation(libs.grpc.services)
    implementation(libs.proto.google.common.protos)

    // CLI
    implementation(libs.picocli)
    // Database drivers for multi-database support
    runtimeOnly(libs.bundles.jdbc.drivers11)
    runtimeOnly(libs.bundles.jdbc.drivers.extra)

    // Cache
    implementation(libs.caffeine)


}

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

@Suppress("UnstableApiUsage")
testing.suites {
    named<JvmTestSuite>("test") {
        useJUnitJupiter(libs.versions.junit.jupiter)
        dependencies {
            implementation(project())
            implementation(libs.assertj.core)
            implementation(libs.mockito.core)
            implementation(libs.grpc.inprocess)
            implementation(libs.grpc.testing)
            implementation(libs.spring.boot.starter.test)
        }
    }
    register<JvmTestSuite>("integrationTest") {
        useJUnitJupiter(libs.versions.junit.jupiter)
        targets.all {
            dependencies {
                implementation(project())
                implementation(project(":api"))
                implementation(project(":domain"))
                implementation(project(":usecase"))
                implementation(project(":usecase-impl"))
                implementation(project(":service"))
                implementation(libs.spring.boot.starter.test)
                implementation(libs.assertj.core)
                implementation(libs.testcontainers.core)
                implementation(libs.testcontainers.junit.jupiter)
                implementation(libs.testcontainers.postgresql)
                implementation(libs.bouncycastle.bcpkix)
                runtimeOnly(libs.postgresql)
            }
        }
    }
}

tasks.check {
    dependsOn(testing.suites.named("integrationTest"))
}

tasks.named<Test>("integrationTest") {
    maxParallelForks = 1
}
