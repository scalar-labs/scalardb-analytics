/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.scalar.db.analytics.gradle.licenseText

plugins {
    id("build-logic.java-common-conventions")
    id("build-logic.license-notices")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(project(":api"))
    implementation(project(":server:base"))
    implementation(project(":lib"))
    implementation(libs.spring.boot.starter)
    implementation(libs.picocli)
}

application {
    applicationName = "scalardb-analytics-server"
    mainClass = "com.scalar.db.analytics.server.Main"
}

tasks.startScripts {
    applicationName = "scalardb-analytics-server"
}

// The image and the archives ship every runtime dependency as a JAR under lib/, so they
// carry an aggregate notice generated from exactly what this distribution bundles. The
// Oracle FUTC text is included because the distribution bundles Oracle JDBC. The Apache-2.0
// text is included because Apache-2.0 section 4(a) requires giving every recipient a copy of
// the License, and publishing an image or an archive is distribution.
distributions.named("main") {
    contents {
        from(licenseText("LICENSE"))
        from(tasks.named("generateLicenseReport")) {
            include("THIRD-PARTY-NOTICES.txt")
        }
        from(licenseText("ORACLE-FUTC.txt"))
    }
}
