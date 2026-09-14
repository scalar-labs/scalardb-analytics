/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.gradle

import com.github.jk1.license.ProjectData
import com.github.jk1.license.filter.DependencyFilter

/** Keeps POM-derived license information and removes data extracted from dependency JARs. */
class PomOnlyFilter : DependencyFilter {
    override fun filter(source: ProjectData): ProjectData {
        for (configuration in source.configurations) {
            for (module in configuration.dependencies) {
                module.manifests.clear()
                module.licenseFiles.clear()
            }
        }
        return source
    }
}
