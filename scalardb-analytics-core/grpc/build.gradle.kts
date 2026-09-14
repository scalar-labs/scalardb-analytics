/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

import build.buf.gradle.GENERATED_DIR

plugins {
    id("build-logic.java8-conventions")
    id("build-logic.publishing-maven-local")
    alias(libs.plugins.buf.gradle.plugin)
}

buf {
    toolVersion = libs.versions.buf.tool.get()
}

dependencies {
    implementation(project(":api"))
    implementation(libs.grpc.netty)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.services)
    // GRPC generated code uses the @javax.annotation.Generated annotation which is not packaged
    // in the JDK 11 anymore
    compileOnly(libs.javax.annotation)

    // MapStruct
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
}

sourceSets.main {
    java.srcDir(layout.buildDirectory.dir("bufbuild/$GENERATED_DIR/java"))
}

tasks.compileJava {
    dependsOn(tasks.bufGenerate)
}

@Suppress("UnstableApiUsage")
testing.suites {
    named<JvmTestSuite>("test") {
        useJUnitJupiter(libs.versions.junit.jupiter)
        dependencies {
            implementation(project())
            implementation(libs.assertj.core)
            implementation(libs.jqwik)
        }
    }
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "scalardb-analytics-grpc"
}
