/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    // "libs" is auto-detected from gradle/libs.versions.toml by Gradle convention.
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        maven("https://rnveach.github.io/checkstyle-backport-jre8/maven2") {
            name = "checkstyle-backport-jre8"
        }
        maven {
            url = uri("https://maven.pkg.github.com/scalar-labs/scalardb-sql")
            credentials {
                val user =
                    providers
                        .gradleProperty("gprUsername")
                        .orElse(providers.environmentVariable("GPR_USERNAME"))
                        .getOrNull()
                val pass =
                    providers
                        .gradleProperty("gprPassword")
                        .orElse(providers.environmentVariable("GPR_PASSWORD"))
                        .getOrNull()

                if (user != null) username = user
                if (pass != null) password = pass
            }
        }
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            name = "sonatype-snapshots"
        }
    }
}

rootProject.name = "scalardb-analytics"
include(
    "api",
    "domain",
    "lib",
    "repository",
    "repository-impl-spring-data-jdbc",
    "service",
    "usecase",
    "usecase-impl",
    "schema-resolver",
    "grpc",
    "grpc-common",
    "client-sdk",
    "datasource-scalardb",
    ":client:cli",
    ":server:base",
    ":server:app",
    ":tools:error-code-doc-generator",
)
