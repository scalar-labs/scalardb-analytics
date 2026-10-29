# Build stage
# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:21.0.12.1_1-jdk-resolute@sha256:f2a85508ee36d3066e304c6c3eec038e9cb14e5f9d5874038fc48448cd8805c3 AS builder

WORKDIR /workspace

# Copy all source files
COPY . .

# Temporary fix for https://emudhra.we-have-a-problem.com/
# Compile CheckCerts.java so that the runtime stage can include CheckCerts.class. Users can run
# `java CheckCerts` in the container to confirm that the compromised CA certificates are not
# trusted by the actual runtime environment. The class must run on the runtime stage's Java (21).
# We can remove this RUN after we update to the fixed version of Java.
RUN javac --release 21 -d /workspace/check-certs docker/tmp-fix-ca-compromise/CheckCerts.java

# Build the CLI fat JAR
RUN ./gradlew :client:cli:shadowJar --no-daemon

# Runtime stage
# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:21.0.12.1_1-jre-resolute@sha256:c010ef3a101af9fa953f2a844238292c1772ae224ab65f0afdd3997c86b05f90

LABEL org.opencontainers.image.licenses="Apache-2.0"

RUN export DEBIAN_FRONTEND=noninteractive \
 && apt-get update \
 && apt-get upgrade -y --no-install-recommends \
 && apt-get autoremove -y \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd -r --gid 201 scalardb \
 && useradd -r --uid 201 -g scalardb -s /usr/sbin/nologin scalardb

# Temporary fix for https://emudhra.we-have-a-problem.com/
# We can remove this RUN after we update to the fixed version of Java.
RUN keytool -delete -cacerts -storepass changeit -alias "cn_emsign_ecc_root_ca__g3,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]" \
 && keytool -delete -cacerts -storepass changeit -alias "cn_emsign_root_ca__g1,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]" \
 && keytool -delete -cacerts -storepass changeit -alias "cn_emsign_ecc_root_ca__c3,o_emudhra_inc,ou_emsign_pki,c_us [jdk]" \
 && keytool -delete -cacerts -storepass changeit -alias "cn_emsign_root_ca__c1,o_emudhra_inc,ou_emsign_pki,c_us [jdk]"

WORKDIR /scalardb-analytics-cli

# Copy the fat JAR from builder
# The application files stay owned by root so that the runtime user cannot modify them.
COPY --from=builder /workspace/client/cli/build/libs/scalardb-analytics-cli-*-all.jar /scalardb-analytics-cli/scalardb-analytics-cli.jar

# Temporary fix for https://emudhra.we-have-a-problem.com/
# Users can run `java CheckCerts` in the WORKDIR to confirm that the compromised CA certificates
# are not trusted. We can remove this COPY after we update to the fixed version of Java.
COPY --from=builder /workspace/check-certs/CheckCerts.class \
     /workspace/docker/tmp-fix-ca-compromise/CheckCerts.java \
     ./

USER 201:201

ENTRYPOINT ["java", "-jar", "/scalardb-analytics-cli/scalardb-analytics-cli.jar"]

CMD ["--help"]
