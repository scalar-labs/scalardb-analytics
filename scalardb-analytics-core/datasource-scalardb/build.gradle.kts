/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.scalar.db.analytics.gradle.GradleUtil.sourceSetOutput

plugins {
    id("build-logic.java-common-conventions")
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":api"))
    implementation(project(":schema-resolver"))
    implementation(libs.guava)
    implementation(libs.slf4j)
    implementation(libs.scalardb.core)
}

testing.suites {
    register<JvmTestSuite>("integrationTest") {
        useJUnitJupiter(libs.versions.junit.jupiter)
        targets.all {
            dependencies {
                implementation(project())
                implementation(project(":api"))
                implementation(project(":schema-resolver"))
                implementation(sourceSetOutput(project, ":api", "test"))
                implementation(libs.assertj.core)
                implementation(libs.slf4j.simple)
                implementation(libs.guava)
                implementation(libs.scalardb.schema.loader)
                implementation.bundle(libs.bundles.testcontainers)
                implementation.bundle(libs.bundles.jdbc.drivers)
            }
        }
    }
}

tasks.check {
    dependsOn(testing.suites.named("integrationTest"))
}
