/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
    // The convention plugins defined here cannot apply themselves, so the meta-build carries its
    // own quality gates. They cover Kotlin only, which is the sole language in this build.
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.spotbugs.plugin)
    implementation(libs.errorprone.plugin)
    implementation(libs.spotless.plugin)
    implementation(libs.lombok.plugin)
    implementation(libs.license.report.plugin)

    // Workaround to access the version catalog in the pre-compiled script plugin
    // See: https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

spotless {
    kotlin {
        target("src/main/kotlin/**/*.kt")
        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle()
    }
}

detekt {
    buildUponDefaultConfig = true
}
