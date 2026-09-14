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
    implementation(libs.guava)
    implementation(libs.slf4j)
}

testing.suites {
    withType(JvmTestSuite::class).configureEach {
        useJUnitJupiter(libs.versions.junit.jupiter)
        dependencies {
            implementation(project())
            implementation(libs.assertj.core)
            implementation(libs.slf4j.simple)
            implementation(libs.guava)
        }
    }
    register<JvmTestSuite>("integrationTest") {
        useJUnitJupiter(libs.versions.junit.jupiter)
        targets.all {
            dependencies {
                implementation(project())
                implementation(project(":api"))
                implementation(sourceSetOutput(project, ":api", "test"))
                implementation(libs.assertj.core)
                implementation(libs.slf4j.simple)
                implementation(libs.guava)
                implementation(libs.scalardb.schema.loader)
                implementation(libs.hikaricp)
                implementation.bundle(libs.bundles.testcontainers)
                implementation.bundle(libs.bundles.jdbc.drivers)
            }
            testTask.configure {
                environment(
                    System.getenv().filterKeys { key -> key.startsWith("ANALYTICS") })
            }
        }
    }
}

tasks.check {
    dependsOn(testing.suites.named("integrationTest"))
}
