/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.ReportRenderer
import com.scalar.db.analytics.gradle.DeterministicTextReportRenderer

plugins {
    id("com.github.jk1.dependency-license-report")
}

// A distributed artifact must attribute exactly the third-party code it bundles, so this
// report covers only this project's own runtime classpath. The repository-wide report that
// `checkLicense` uses for policy enforcement stays on the root project.
licenseReport {
    configurations = arrayOf("runtimeClasspath")
    projects = arrayOf(project)
    excludeOwnGroup = true
    renderers =
        arrayOf<ReportRenderer>(DeterministicTextReportRenderer("THIRD-PARTY-NOTICES.txt"))
    filters = arrayOf<DependencyFilter>(LicenseBundleNormalizer())
}

// Policy enforcement belongs to the root project, whose report is the union of every module and
// therefore a superset of this one. This project configures no allowed-licenses file and keeps the
// JAR-embedded license data that a notice needs, so a check running here would answer a different
// question from the one the policy asks.
tasks.named("checkLicense") {
    enabled = false
}
