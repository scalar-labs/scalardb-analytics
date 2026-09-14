/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("build-logic.java8-conventions")
    id("build-logic.publishing-maven-local")
}

dependencies {
    implementation(project(":api"))
    implementation(project(":grpc"))
    implementation(project(":grpc-common"))
    implementation(libs.grpc.netty)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    // GRPC generated code uses the @javax.annotation.Generated annotation which is not packaged
    // in the JDK 11 anymore
    compileOnly(libs.javax.annotation)
    // MapStruct is used in grpc module; add as compileOnly to suppress compiler warnings
    compileOnly(libs.mapstruct)
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
            // MapStruct is used in grpc module; add as compileOnly to suppress compiler warnings
            compileOnly(libs.mapstruct)
        }
    }
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "scalardb-analytics-client-sdk"
}
