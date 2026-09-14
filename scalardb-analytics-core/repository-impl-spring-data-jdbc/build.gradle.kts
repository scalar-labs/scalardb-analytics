/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("build-logic.java-common-conventions")
    alias(libs.plugins.spring.dependency.management)
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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}")
    }
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":api"))
    implementation(project(":domain"))
    implementation(project(":repository"))

    // Spring Boot baseline
    implementation(libs.spring.boot.starter.data.jdbc)
    implementation(libs.spring.jdbc)
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.jackson.databind)
    implementation(libs.spring.boot.jackson2)

    // ScalarDB SQL integration
    implementation(libs.scalardb.sql.spring.data)
    implementation(libs.scalardb.sql.direct.mode)
    implementation(libs.scalardb.schema.loader) {
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }
    implementation(libs.scalardb.core) {
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }

    // Logging
    implementation(libs.slf4j)

    // Nullness annotations

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}

testing.suites {
    withType(JvmTestSuite::class).configureEach {
        useJUnitJupiter(libs.versions.junit.jupiter)
        dependencies {
            implementation(project())
            implementation(libs.assertj.core)
        }
    }
    register<JvmTestSuite>("integrationTest") {
        useJUnitJupiter(libs.versions.junit.jupiter)
        targets.all {
            dependencies {
                implementation(project())
                implementation(project(":lib"))
                implementation(project(":api"))
                implementation(project(":domain"))
                implementation(project(":repository"))
                implementation(libs.spring.boot.starter.test)
                implementation(libs.spring.boot.starter.data.jdbc)
                implementation(libs.assertj.core)
                implementation(libs.jackson.databind)
                implementation(libs.junit.platform.suite.api)
                implementation(libs.scalardb.core) {
                    exclude(group = "org.slf4j", module = "slf4j-simple")
                }
                implementation(libs.scalardb.schema.loader) {
                    exclude(group = "org.slf4j", module = "slf4j-simple")
                }
                runtimeOnly(libs.junit.platform.suite.engine)

                // Import TestImages from api test sources
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
    // Integration tests share a single static PostgreSQLContainer (see
    // AbstractScalarDbIntegrationTest), so we keep execution single-threaded
    // to avoid concurrent schema mutations.
    maxParallelForks = 1
}
