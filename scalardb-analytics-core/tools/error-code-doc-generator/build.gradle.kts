/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("build-logic.java-common-conventions")
    application
}

dependencies {
    implementation(project(":api"))
}

application {
    mainClass.set("com.scalar.db.analytics.tools.errorcodedoc.ErrorCodeDocGeneratorMain")
}

@Suppress("UnstableApiUsage")
testing.suites {
    named<JvmTestSuite>("test") {
        useJUnitJupiter(libs.versions.junit.jupiter)
        dependencies {
            implementation(project())
            implementation(project(":api"))
            implementation(libs.assertj.core)
        }
    }
}
