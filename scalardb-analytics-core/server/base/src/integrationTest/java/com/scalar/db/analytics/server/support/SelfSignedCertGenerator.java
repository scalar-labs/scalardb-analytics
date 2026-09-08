/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.support;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Generates a self-signed TLS certificate for integration tests using Bouncy Castle. Creates a
 * PEM-encoded certificate and private key written to temporary files.
 */
public final class SelfSignedCertGenerator {

  private final File certFile;
  private final File keyFile;

  private SelfSignedCertGenerator(File certFile, File keyFile) {
    this.certFile = certFile;
    this.keyFile = keyFile;
  }

  /** Generates a new self-signed certificate for "localhost". */
  public static SelfSignedCertGenerator create() {
    try {
      KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
      keyGen.initialize(2048, new SecureRandom());
      KeyPair keyPair = keyGen.generateKeyPair();

      X500Name subject = new X500Name("CN=localhost");
      Instant now = Instant.now();
      Date notBefore = Date.from(now);
      Date notAfter = Date.from(now.plus(365, ChronoUnit.DAYS));
      BigInteger serial = BigInteger.valueOf(now.toEpochMilli());

      ContentSigner signer =
          new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());

      X509v3CertificateBuilder certBuilder =
          new JcaX509v3CertificateBuilder(
              subject, serial, notBefore, notAfter, subject, keyPair.getPublic());

      X509CertificateHolder certHolder = certBuilder.build(signer);

      File certFile = File.createTempFile("test-cert-", ".pem");
      certFile.deleteOnExit();
      try (JcaPEMWriter writer =
          new JcaPEMWriter(Files.newBufferedWriter(certFile.toPath(), StandardCharsets.UTF_8))) {
        writer.writeObject(certHolder);
      }

      File keyFile = File.createTempFile("test-key-", ".pem");
      keyFile.deleteOnExit();
      try (JcaPEMWriter writer =
          new JcaPEMWriter(Files.newBufferedWriter(keyFile.toPath(), StandardCharsets.UTF_8))) {
        writer.writeObject(keyPair.getPrivate());
      }

      return new SelfSignedCertGenerator(certFile, keyFile);
    } catch (NoSuchAlgorithmException | OperatorCreationException | IOException e) {
      throw new RuntimeException("Failed to generate self-signed certificate", e);
    }
  }

  /** Returns the path to the PEM-encoded certificate chain file. */
  public String certChainPath() {
    return certFile.getAbsolutePath();
  }

  /** Returns the path to the PEM-encoded private key file. */
  public String privateKeyPath() {
    return keyFile.getAbsolutePath();
  }

  /** Returns the certificate file (used by gRPC client for trust). */
  public File certFile() {
    return certFile;
  }
}
