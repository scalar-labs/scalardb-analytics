/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    java
    // Disable the checkstyle plugin for now because the backport version disappears from the maven
    // central repository unexpectedly.
    // checkstyle

    // Unfortunately, the type-safe accessors of the version catalog are not available in
    // the `plugins` block of the pre-compiled script plugin.
    // See: https://github.com/gradle/gradle/issues/15383
    id("com.github.spotbugs")
    id("net.ltgt.errorprone")
    id("com.diffplug.spotless")
    id("io.freefair.lombok")
}

group = "com.scalar-labs"
// Note: Ideally, main branch should use 4.0.0-SNAPSHOT and v3 branch should use 3.x.0-SNAPSHOT.
// However, since there are currently no v4-specific features, we use 3.x.0-SNAPSHOT on main branch.
version = "3.20.0-SNAPSHOT"

val libs = the<LibrariesForLibs>()

// Keep Spring Boot's dependency management aligned with ScalarDB's MariaDB driver version.
extra["mariadb.version"] = libs.versions.mariadb.get()

// Default toolchain for internal/server-side modules: the latest LTS in use across the project.
// Java 8 client modules opt into Java 8 *target* via build-logic.java8-conventions (compiled with
// this same modern JDK using --release). Error Prone also requires JDK 21+ to run.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Use the same Java version defined in the toolchain to compile Scala
tasks.withType<ScalaCompile> {
    scalaCompileOptions.apply {
        additionalParameters = additionalParameters.orEmpty().plus("-release:${java.toolchain.languageVersion.get()}");
    }
}

dependencies {
    // checkstyle(libs.checkstyle)
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
    // JSpecify annotations (@NullMarked / @Nullable) are used for null-safety across every module,
    // so provide them from the common convention rather than per-module.
    implementation(libs.jspecify)
    // Jackson BOM for version management (Spring Boot BOM modules get this transitively,
    // but non-Spring modules need it explicitly)
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
    // gRPC BOM keeps the whole gRPC artifact family on a single version. Without it, only the
    // artifacts pinned in libs.versions.toml get our version while the rest (grpc-api/util/context)
    // are downgraded by google-cloud-storage's transitive libraries-bom, causing a version skew.
    implementation(platform(libs.grpc.bom))
}

// Custom JvmTestSuites (e.g. integrationTest) have their own classpaths that do not inherit the
// main implementation, so the gRPC BOM must be applied to every suite as well; otherwise the
// version-less grpc-* dependencies declared in those suites cannot be resolved.
testing.suites.withType<JvmTestSuite>().configureEach {
    dependencies {
        implementation(platform(libs.grpc.bom))
    }
}

// checkstyle {
//     toolVersion = libs.versions.checkstyle.get()
//     configFile = rootProject.file("config/checkstyle/checkstyle.xml")
// }

spotbugs {
    toolVersion.set(libs.versions.spotbugs.asProvider().get())
    ignoreFailures.set(true)
    showStackTraces.set(true)
    effort.set(Effort.DEFAULT)
    reportLevel.set(Confidence.DEFAULT)
    maxHeapSize.set("1g")
    extraArgs.set(listOf("-nested:false"))
    jvmArgs.set(listOf("-Duser.language=en"))
    excludeFilter.set(rootProject.file("config/spotbugs/excludes.xml"))
}

spotless {
    java {
        target("src/*/java/**/*.java")
        licenseHeader(
            """
            /*
             * Copyright Scalar, Inc.
             * SPDX-License-Identifier: Apache-2.0
             */
            """.trimIndent(),
        )
        importOrder()
        removeUnusedImports()
        googleJavaFormat(libs.versions.googleJavaFormat.get())
    }
    scala {
        scalafmt()
    }
}

lombok {
    version.set(libs.versions.lombok.asProvider().get())
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:all")
    // Required by Error Prone when running on JDK 21 so that type-use annotations are attached to
    // symbols (also a prerequisite for NullAway's JSpecify mode). Harmless on older JDKs.
    options.compilerArgs.add("-XDaddTypeAnnotationsToSymbol=true")
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        // Exclude generated sources from error-prone checks
        excludedPaths.set(".*/generated/.*")
        // Error Prone's StringConcatToTextBlock check throws an internal exception
        // (NoSuchElementException from Iterables.getLast) on Lombok @Builder-generated code;
        // disable it until fixed upstream.
        disable("StringConcatToTextBlock")
        // This project uses switch statements with enums many times. So, we force the developer to
        // handle all cases explicitly by raising an error in the case of missing cases.
        error("MissingCasesInEnumSwitch")
        // NullAway: JSpecify-based null-safety checking. Only code under @NullMarked scope is
        // checked (OnlyNullMarked), with JSpecify semantics enabled (JSpecifyMode).
        error("NullAway")
        option("NullAway:OnlyNullMarked", "true")
        option("NullAway:JSpecifyMode", "true")
        // Treat assertions from the test assertion libraries in use (AssertJ, JUnit) as null checks,
        // so that a value asserted non-null can be dereferenced afterwards.
        option("NullAway:HandleTestAssertionLibraries", "true")
        // Fields populated by a framework after instantiation are treated as initialized rather
        // than requiring constructor init: Spring Data JDBC entity columns (@Column) and picocli
        // command inputs (@Option / @Parameters / @ParentCommand / @ArgGroup).
        option(
            "NullAway:ExcludedFieldAnnotations",
            listOf(
                    "org.springframework.data.relational.core.mapping.Column",
                    "picocli.CommandLine.Option",
                    "picocli.CommandLine.Parameters",
                    "picocli.CommandLine.ParentCommand",
                    "picocli.CommandLine.ArgGroup",
                )
                .joinToString(","),
        )
    }
}
