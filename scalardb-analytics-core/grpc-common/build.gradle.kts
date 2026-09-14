/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("build-logic.java8-conventions")
    id("build-logic.publishing-maven-local")
}

dependencies {
    implementation(libs.grpc.netty)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.services)
    // GRPC generated code uses the @javax.annotation.Generated annotation which is not packaged
    // in the JDK 11 anymore
    compileOnly(libs.javax.annotation)
}

@Suppress("UnstableApiUsage")
testing.suites {
    named<JvmTestSuite>("test") {
        useJUnitJupiter(libs.versions.junit.jupiter)
        dependencies {
            implementation(project())
            implementation(libs.assertj.core)
            implementation(libs.mockito.core)
            implementation(libs.mockito.junit.jupiter)
            implementation(libs.grpc.testing)
        }
    }
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "scalardb-analytics-grpc-common"
}
