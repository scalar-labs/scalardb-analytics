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
    implementation(project(":lib"))
    implementation(project(":api"))
    implementation(project(":domain"))
    implementation(libs.guava)
    implementation(libs.slf4j)
}
