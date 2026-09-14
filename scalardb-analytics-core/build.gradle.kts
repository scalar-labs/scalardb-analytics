/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("build-logic.license-reporting")
}

// Included builds contribute no tasks to the consuming build, so the meta-build's own quality
// gates have to be attached explicitly for `./gradlew check` to reach them.
tasks.register("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
