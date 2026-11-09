/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    id("build-logic.java-common-conventions")
}

val libs = the<LibrariesForLibs>()

// The Spring Boot BOM is imported as a Gradle-native platform, the same way as the gRPC and Jackson
// BOMs in java-common-conventions, rather than through the io.spring.dependency-management plugin.
// A platform only contributes version constraints that take part in ordinary conflict resolution,
// so the highest of the BOM, the version catalog and the transitive versions wins unless the catalog
// pins a version strictly, and no BOM silently downgrades another (e.g. Spring Boot's gRPC or
// Jackson below the imported BOMs).
// Use `platform` rather than `enforcedPlatform` for the same reason, and because an enforced
// platform would force its versions on consumers of the published modules.
dependencies {
    implementation(platform(libs.spring.boot.bom))
}

// Custom JvmTestSuites (e.g. integrationTest) do not inherit the main implementation, so they need
// the platform as well to resolve the version-less Spring dependencies declared in them.
testing.suites.withType<JvmTestSuite>().configureEach {
    dependencies {
        implementation(platform(libs.spring.boot.bom))
    }
}
