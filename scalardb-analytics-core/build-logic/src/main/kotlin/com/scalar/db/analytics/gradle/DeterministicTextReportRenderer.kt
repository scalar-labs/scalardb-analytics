/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.gradle

import com.github.jk1.license.LicenseReportExtension
import com.github.jk1.license.ProjectData
import com.github.jk1.license.render.ReportRenderer
import com.github.jk1.license.render.TextReportRenderer
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern

/** Renders a text report without the generation timestamp that would change on every run. */
class DeterministicTextReportRenderer(private val fileName: String) : ReportRenderer {
    private val delegate = TextReportRenderer(fileName)

    override fun render(source: ProjectData) {
        delegate.render(source)

        val extension = source.project.extensions.getByType(LicenseReportExtension::class.java)
        val output = Paths.get(extension.absoluteOutputDir, fileName)
        try {
            val report = String(Files.readAllBytes(output), StandardCharsets.UTF_8)
            val markerIndex = report.lastIndexOf(GENERATED_AT_MARKER)
            check(markerIndex >= 0) { "The dependency license report has no generation timestamp" }
            var deterministicReport =
                report.substring(0, markerIndex + 1).replace("\r\n", "\n").replace('\r', '\n')
            deterministicReport = TRAILING_WHITESPACE.matcher(deterministicReport).replaceAll("")
            deterministicReport = TRAILING_NEWLINES.matcher(deterministicReport).replaceFirst("\n")
            Files.write(output, deterministicReport.toByteArray(StandardCharsets.UTF_8))
        } catch (e: IOException) {
            throw IllegalStateException(
                "Failed to make the dependency license report deterministic",
                e,
            )
        }
    }

    private companion object {
        const val GENERATED_AT_MARKER = "\nThis report was generated at "
        val TRAILING_WHITESPACE: Pattern = Pattern.compile("[\\t ]+$", Pattern.MULTILINE)
        val TRAILING_NEWLINES: Pattern = Pattern.compile("\\n+$")
    }
}
