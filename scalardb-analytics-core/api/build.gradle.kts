/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("build-logic.java8-conventions")
    id("build-logic.publishing-maven-local")
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.jackson.databind)
    implementation(libs.slf4j)
    implementation(libs.guava)
}

@Suppress("UnstableApiUsage")
testing.suites {
    named<JvmTestSuite>("test") {
        useJUnitJupiter(libs.versions.junit.jupiter)
        dependencies {
            implementation(libs.assertj.core)
            implementation(libs.jqwik)
            implementation(libs.testcontainers.core)
            implementation(libs.hikaricp)
        }
    }
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "scalardb-analytics-api"
}
