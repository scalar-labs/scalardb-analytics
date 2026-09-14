/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.gradle

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetOutput

object GradleUtil {
    fun sourceSetOutput(
        project: Project,
        dependentProjectName: String,
        sourceSetName: String
    ): FileCollection =
        project.files(
            findProject(project, dependentProjectName)
                .flatMap { findExtension(it, JavaPluginExtension::class.java) }
                .flatMap { findSourceSet(it, sourceSetName) }
        )

    private fun findProject(project: Project, name: String): Provider<Project> =
        project.provider {
            requireNotNull(project.rootProject.findProject(name)) { "Project $name not found" }
        }

    private fun <T> findExtension(project: Project, extension: Class<T>): Provider<T> =
        project.provider {
            requireNotNull(project.extensions.findByType(extension)) {
                "Extension $extension not found"
            }
        }

    private fun findSourceSet(
        extension: JavaPluginExtension,
        sourceSetName: String
    ): Provider<SourceSetOutput> = extension.sourceSets.named(sourceSetName).map { it.output }
}
