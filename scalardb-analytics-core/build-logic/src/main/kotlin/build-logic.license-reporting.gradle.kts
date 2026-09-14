/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.ReportRenderer
import com.scalar.db.analytics.gradle.DeterministicTextReportRenderer
import com.scalar.db.analytics.gradle.PomOnlyFilter

plugins {
    id("com.github.jk1.dependency-license-report")
}

licenseReport {
    configurations = arrayOf("runtimeClasspath")
    projects = arrayOf(project) + subprojects
    excludeOwnGroup = true
    renderers =
        arrayOf<ReportRenderer>(DeterministicTextReportRenderer("THIRD-PARTY-NOTICES.txt"))
    // A dependency passes the check when any one of its licenses is allowed, and the checker
    // collects names from JAR manifests and JAR-embedded license files as well as from POM
    // metadata. A permissive text bundled in a JAR would therefore let a dependency through on a
    // license its POM never declares. Restricting the check to the POM-declared values closes that
    // gap. This report is an intermediate result for `checkLicense` and is never published; the
    // notices that artifacts package come from `build-logic.license-notices`, which must keep the
    // embedded data and therefore omits this filter.
    filters = arrayOf<DependencyFilter>(PomOnlyFilter(), LicenseBundleNormalizer())
    // The policy lives at the repository root because the Mill build for the Spark connector reads
    // the same file; keeping one copy keeps the two builds answerable to one policy. Only
    // `checkLicense` reads it, and the Docker builds never run that task, so the path resolving
    // outside their build context does not affect them.
    allowedLicensesFile = rootProject.projectDir.parentFile.resolve("allowed-licenses.json")
}
