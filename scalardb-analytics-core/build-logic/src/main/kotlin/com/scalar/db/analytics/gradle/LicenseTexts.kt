/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.gradle

import org.gradle.api.Project
import org.gradle.api.file.RegularFile

/**
 * Resolves a license text that a distribution must carry, failing the build when it is absent.
 *
 * Gradle expands a missing copy source into an empty file tree, so referencing the file directly
 * would let a move or a rename drop a legally required text from the archive without any failure.
 */
fun Project.licenseText(name: String): RegularFile {
    val file = rootProject.layout.projectDirectory.file("licenses/$name")
    require(file.asFile.isFile) {
        "Missing license text that a distribution must carry: ${file.asFile}"
    }
    return file
}
