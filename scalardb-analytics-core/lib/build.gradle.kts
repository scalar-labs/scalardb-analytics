/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("build-logic.java8-conventions")
    id("build-logic.publishing-maven-local")
}

dependencies {
    implementation(libs.owner)
    implementation(libs.slf4j)
}

@Suppress("UnstableApiUsage")
testing.suites {
    named<JvmTestSuite>("test") {
        useJUnitJupiter(libs.versions.junit.jupiter)
        dependencies {
            implementation(libs.assertj.core)
        }
    }
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "scalardb-analytics-lib"
}
