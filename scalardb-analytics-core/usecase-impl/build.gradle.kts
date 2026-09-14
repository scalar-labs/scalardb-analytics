/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("build-logic.java-common-conventions")
    alias(libs.plugins.spring.dependency.management)
}

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
