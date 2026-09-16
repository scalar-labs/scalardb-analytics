/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("build-logic.java-common-conventions")
    id("build-logic.publishing-maven-local")
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
    implementation(libs.guava)
    implementation(libs.slf4j)
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "scalardb-analytics-repository"
}
