/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

import java.net.URI

plugins {
    `maven-publish`
    signing
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            url.set("https://scalardb.scalar-labs.com/docs/latest/scalardb-analytics/")
            scm {
                url.set("https://github.com/scalar-labs/scalardb-analytics")
            }
            licenses {
                license {
                    name.set("Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("hiroyuki")
                    name.set("Hiroyuki Yamada")
                    email.set("hiroyuki.yamada@scalar-labs.com")
                }
                developer {
                    id.set("aki")
                    name.set("Akihiro Okuno")
                    email.set("akihiro.okuno@scalar-labs.com")
                }
            }
        }
        // Sign only if signing properties are available
        if (project.hasProperty("signingKey") || project.hasProperty("signing.keyId")) {
            signing.sign(this)
        }
    }

    repositories {
        maven {
            val releasesRepoUrl = "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://central.sonatype.com/repository/maven-snapshots/"
            url =
                URI.create(
                    if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl,
                )
            name = "ossrh"
            credentials(PasswordCredentials::class)
        }
    }
}

// Configure signing with in-memory PGP keys if available
signing {
    val signingKey: String? by project
    val signingPassword: String? by project

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}
