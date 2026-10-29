# grpc_health_probe for Kubernetes:
# - https://kubernetes.io/blog/2018/10/01/health-checking-grpc-servers-on-kubernetes/
# The official image is a multi-platform index, so it serves both platforms that the Makefile
# builds for. You can confirm the digest with:
#   docker buildx imagetools inspect ghcr.io/grpc-ecosystem/grpc-health-probe:v0.4.60
FROM ghcr.io/grpc-ecosystem/grpc-health-probe:v0.4.60@sha256:2d433b978644e40eb2b2887dbd0af3a19e470514a2176a97cad1971f13bdb171 AS grpc-health-probe

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

# Build the server distribution
# Mount secrets to avoid exposing credentials in image metadata
RUN --mount=type=secret,id=gpr_username \
    --mount=type=secret,id=gpr_password \
    GPR_USERNAME=$(cat /run/secrets/gpr_username 2>/dev/null || echo "") \
    GPR_PASSWORD=$(cat /run/secrets/gpr_password 2>/dev/null || echo "") \
    ./gradlew :server:app:installDist --no-daemon

# Runtime stage
# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:21.0.12.1_1-jdk-resolute@sha256:f2a85508ee36d3066e304c6c3eec038e9cb14e5f9d5874038fc48448cd8805c3

LABEL org.opencontainers.image.licenses="Apache-2.0"

# netcat lets testcontainers check the ports this image exposes
RUN export DEBIAN_FRONTEND=noninteractive \
 && apt-get update \
 && apt-get upgrade -y --no-install-recommends \
 && apt-get install -y --no-install-recommends netcat-openbsd \
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

COPY --from=grpc-health-probe /ko-app/grpc-health-probe /usr/local/bin/grpc_health_probe

WORKDIR /scalardb-analytics-server

# The application files stay owned by root so that the runtime user cannot modify them.
COPY --from=builder /workspace/server/app/build/install/scalardb-analytics-server/ /scalardb-analytics-server/

# Temporary fix for https://emudhra.we-have-a-problem.com/
# Users can run `java CheckCerts` in the WORKDIR to confirm that the compromised CA certificates
# are not trusted. We can remove this COPY after we update to the fixed version of Java.
COPY --from=builder /workspace/check-certs/CheckCerts.class \
     /workspace/docker/tmp-fix-ca-compromise/CheckCerts.java \
     ./

USER 201:201

EXPOSE 11051

ENTRYPOINT ["/scalardb-analytics-server/bin/scalardb-analytics-server"]

CMD ["start", "--config", "/scalardb-analytics-server/server.properties"]
