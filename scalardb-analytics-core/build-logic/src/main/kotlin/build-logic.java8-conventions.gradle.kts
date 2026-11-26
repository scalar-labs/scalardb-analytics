/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("build-logic.java-common-conventions")
}

// These client-facing modules target Java 8 for broad compatibility. They inherit the modern
// compiler toolchain from java-common-conventions (so current Error Prone / NullAway tooling can
// run) and constrain the bytecode and platform API to Java 8 via --release; tests run on a real
// Java 8 runtime to keep genuine Java 8 compatibility validation.
tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
    // Without -parameters, javac 21+ writes MethodParameters entries without names for the
    // implicit parameters of enums (valueOf and the constructor). Scala 2.13.8, which the Spark
    // 3.5 connector builds with, crashes reading them ("bad constant pool index: 0", see
    // scala/bug#12783), so the connector could not use any of these enums. -parameters gives
    // every entry a name.
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    )
}
