/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("build-logic.spring-boot-platform")
    id("build-logic.publishing-maven-local")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":api"))
    implementation(project(":domain"))
    implementation(project(":usecase"))
    implementation(project(":service"))
    implementation(project(":repository"))
    implementation(project(":schema-resolver"))
    implementation(project(":datasource-scalardb"))
    implementation(libs.guava)
    implementation(libs.caffeine)
    implementation(libs.slf4j)
    implementation(libs.spring.security.crypto)
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

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "scalardb-analytics-usecase-impl"
}
