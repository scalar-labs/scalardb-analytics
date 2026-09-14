/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("build-logic.java-common-conventions")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(project(":api"))
    implementation(project(":lib"))
    implementation(project(":domain"))
    implementation(project(":repository"))
    implementation(libs.slf4j)
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
        }
    }
}
