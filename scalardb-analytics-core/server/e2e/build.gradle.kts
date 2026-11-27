/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("build-logic.spring-boot-platform")
}

// This module holds only the integrationTest suite and produces no artifact.
tasks.jar {
    enabled = false
}

@Suppress("UnstableApiUsage")
testing.suites {
    register<JvmTestSuite>("integrationTest") {
        useJUnitJupiter(libs.versions.junit.jupiter)
        targets.all {
            dependencies {
                implementation(project(":server:base"))
                implementation(project(":grpc"))
                implementation(project(":api"))
                implementation(libs.spring.boot.starter.test)
                implementation(libs.assertj.core)
                implementation(libs.testcontainers.core)
                implementation(libs.testcontainers.junit.jupiter)
                implementation(libs.testcontainers.postgresql)
                implementation(libs.grpc.netty)
                implementation(libs.grpc.stub)
                implementation(libs.grpc.protobuf)
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
