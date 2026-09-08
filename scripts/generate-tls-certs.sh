#!/bin/bash

# Script to generate TLS certificates for ScalarDB Analytics server and clients
# This generates self-signed certificates for development and testing purposes

set -e

# Configuration
CERT_DIR="./certs"
VALIDITY_DAYS=365
CURVE="prime256v1"
COUNTRY="US"
STATE="California"
LOCALITY="San Francisco"
ORGANIZATION="ScalarDB Analytics"
ORG_UNIT="Development"
COMMON_NAME="localhost"

# Create certificate directory
mkdir -p "$CERT_DIR"
CERT_DIR_ABS="$(realpath "$CERT_DIR")"

echo "Generating TLS certificates in $CERT_DIR..."

# Generate CA private key
echo "1. Generating CA private key..."
openssl ecparam -genkey -name $CURVE -out "$CERT_DIR/ca-key.pem"

# Generate CA certificate
echo "2. Generating CA certificate..."
openssl req -new -x509 -sha256 \
    -key "$CERT_DIR/ca-key.pem" \
    -out "$CERT_DIR/ca-cert.pem" \
    -days $VALIDITY_DAYS \
    -subj "/C=$COUNTRY/ST=$STATE/L=$LOCALITY/O=$ORGANIZATION/OU=$ORG_UNIT/CN=ScalarDB Analytics CA"

# Generate server private key (named curve, PKCS#8 format; netty requires PKCS#8)
# Note: `openssl genpkey -algorithm EC` encodes the curve as explicit EC parameters on
# LibreSSL (e.g. macOS), and Java (JCE) only accepts named curves ("Only named
# ECParameters supported"), so the server fails to start with TLS. Using
# `ecparam -genkey -name` keeps the curve named, then `pkcs8 -topk8` converts the SEC1
# key to PKCS#8 while preserving the named curve.
echo "3. Generating server private key..."
openssl ecparam -genkey -name "$CURVE" -out "$CERT_DIR/server-key-sec1.pem"
openssl pkcs8 -topk8 -nocrypt -in "$CERT_DIR/server-key-sec1.pem" -out "$CERT_DIR/server-key.pem"
rm -f "$CERT_DIR/server-key-sec1.pem"

# Generate server certificate request
echo "4. Generating server certificate request..."
openssl req -new -sha256 \
    -key "$CERT_DIR/server-key.pem" \
    -out "$CERT_DIR/server-csr.pem" \
    -subj "/C=$COUNTRY/ST=$STATE/L=$LOCALITY/O=$ORGANIZATION/OU=$ORG_UNIT/CN=$COMMON_NAME"

# Create extensions file for server certificate
cat >"$CERT_DIR/server-ext.cnf" <<EOF
subjectAltName = DNS:localhost,DNS:*.localhost,IP:127.0.0.1,IP:0.0.0.0
EOF

# Generate server certificate signed by CA
echo "5. Generating server certificate..."
openssl x509 -req -sha256 \
    -in "$CERT_DIR/server-csr.pem" \
    -CA "$CERT_DIR/ca-cert.pem" \
    -CAkey "$CERT_DIR/ca-key.pem" \
    -CAcreateserial \
    -out "$CERT_DIR/server-cert.pem" \
    -days $VALIDITY_DAYS \
    -extfile "$CERT_DIR/server-ext.cnf"

# Clean up temporary files
rm -f "$CERT_DIR/server-csr.pem" "$CERT_DIR/server-ext.cnf"

echo ""
echo "Certificate generation complete!"
echo ""
echo "Generated files:"
echo "  CA certificate:     $CERT_DIR/ca-cert.pem (for clients to trust)"
echo "  Server certificate: $CERT_DIR/server-cert.pem"
echo "  Server private key: $CERT_DIR/server-key.pem"
echo ""
echo "To start the server with TLS (create server.properties):"
echo "  scalar.db.analytics.server.catalog.port=11051"
echo "  scalar.db.analytics.server.tls.enabled=true"
echo "  scalar.db.analytics.server.tls.cert_chain_path=$CERT_DIR_ABS/server-cert.pem"
echo "  scalar.db.analytics.server.tls.private_key_path=$CERT_DIR_ABS/server-key.pem"
echo ""
echo "Then start with: scalardb-analytics-server start --config /path/to/server.properties"
echo ""
echo "To configure the client (client.properties):"
echo "  scalar.db.analytics.client.server.host=localhost"
echo "  scalar.db.analytics.client.server.catalog.port=11051"
echo "  scalar.db.analytics.client.server.tls.enabled=true"
echo "  # Optional: Use custom CA certificate (if not specified, uses system default trust store)"
echo "  scalar.db.analytics.client.server.tls.ca_root_cert_path=$CERT_DIR_ABS/ca-cert.pem"
echo "  # Optional: Override authority for TLS verification"
echo "  # scalar.db.analytics.client.server.tls.override_authority=example.com"
echo ""
echo "To configure Spark:"
echo "  spark.sql.catalog.scalardb_analytics.server.tls.enabled=true"
echo "  # Optional: Use custom CA certificate (if not specified, uses system default trust store)"
echo "  spark.sql.catalog.scalardb_analytics.server.tls.ca_root_cert_path=$CERT_DIR_ABS/ca-cert.pem"
echo "  # Optional: Override authority for TLS verification"
echo "  # spark.sql.catalog.scalardb_analytics.server.tls.override_authority=example.com"
