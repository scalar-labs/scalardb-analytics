/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.scalar.db.analytics.gradle.licenseText

plugins {
    id("build-logic.java8-conventions")
    id("build-logic.license-notices")
    application
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":api"))
    implementation(project(":grpc-common"))
    implementation(project(":client-sdk"))
    implementation(libs.slf4j)
    implementation(libs.slf4j.simple)
    implementation(libs.guava)
    implementation(libs.picocli)
    implementation(libs.owner)
}

application {
    applicationName = "scalardb-analytics"
    mainClass = "com.scalar.db.analytics.client.Main"
}

tasks.startScripts {
    applicationName = "scalardb-analytics"
}

// Helper function to access test source sets from other projects
fun sourceSetOutput(
    project: Project,
    path: String,
    sourceSet: String,
): FileCollection =
    project
        .project(path)
        .extensions
        .getByType(SourceSetContainer::class.java)
        .named(sourceSet)
        .get()
        .output

@Suppress("UnstableApiUsage")
testing.suites {
    named<JvmTestSuite>("test") {
        useJUnitJupiter(libs.versions.junit.jupiter)
        dependencies {
            implementation(project())
            implementation(libs.assertj.core)
            implementation(libs.mockito.core)
            implementation(libs.system.stubs.core)
            implementation(libs.system.stubs.jupiter)
        }
    }
    register<JvmTestSuite>("integrationTest") {
        useJUnitJupiter(libs.versions.junit.jupiter)
        targets.all {
            dependencies {
                implementation(project())
                implementation(project(":api"))
                implementation(project(":client-sdk"))
                implementation(libs.assertj.core)
                implementation(libs.jackson.databind)
                implementation(libs.picocli)
                implementation(libs.postgresql)
                implementation(sourceSetOutput(project, ":api", "test"))
                implementation.bundle(libs.bundles.testcontainers)
            }
        }
    }
}

tasks.check {
    dependsOn(testing.suites.named("integrationTest"))
}

tasks.named<Test>("integrationTest") {
    maxParallelForks = 1
}

tasks.shadowJar {
    archiveBaseName.set("scalardb-analytics-cli")
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "com.scalar.db.analytics.client.Main"
    }
    mergeServiceFiles()
    // Apache-2.0 section 4(a) requires giving every recipient a copy of the License, and
    // publishing the image that carries this JAR is distribution. It goes to the archive root
    // rather than META-INF, because bundled dependencies already contribute META-INF/LICENSE
    // entries that would collapse this one by first-wins.
    from(licenseText("LICENSE"))
    // Merging every dependency into one JAR collapses their META-INF/NOTICE entries by
    // first-wins, so the JAR carries an aggregate notice generated from exactly the
    // dependencies it bundles. The CLI does not bundle Oracle JDBC, so the FUTC text that
    // the server distributions carry does not apply here.
    from(tasks.named("generateLicenseReport")) {
        include("THIRD-PARTY-NOTICES.txt")
        into("META-INF")
    }
}
