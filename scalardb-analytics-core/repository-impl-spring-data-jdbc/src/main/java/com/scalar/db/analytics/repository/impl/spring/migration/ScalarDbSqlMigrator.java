/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.migration;

import com.scalar.db.analytics.repository.impl.spring.autoconfigure.ScalarDbSqlProperties;
import com.scalar.db.schemaloader.SchemaLoader;
import com.scalar.db.schemaloader.SchemaLoaderException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Handles ScalarDB schema initialization and migration.
 *
 * <p>This migrator is used for both initial loading and schema migration. When executed via {@link
 * ScalarDbSqlMigrationRunner}, it runs automatically at application startup to ensure the ScalarDB
 * schema is properly provisioned.
 *
 * <h3>Migration Risks</h3>
 *
 * <p>The {@link #migrate()} method invokes {@link SchemaLoader#alterTables(Properties, String,
 * java.util.Map)}, which may trigger table rebuilds for incompatible schema changes. This can
 * result in downtime or data unavailability during the migration process.
 *
 * <h3>Future Improvements</h3>
 *
 * <p>Currently, users cannot control the timing of schema migrations since they occur automatically
 * at startup. To provide better control over potentially disruptive operations:
 *
 * <ul>
 *   <li>A separate tool or subcommand should be provided to allow users to trigger migrations
 *       explicitly
 *   <li>Schema changes should be designed to avoid incompatible changes where possible
 *   <li>Flexible schema designs should be preferred to minimize the need for table rebuilds
 * </ul>
 */
public class ScalarDbSqlMigrator {

  private static final Logger logger = LoggerFactory.getLogger(ScalarDbSqlMigrator.class);
  private static final String SCHEMA_RESOURCE = "classpath:scalardb/schema.json";

  private final ScalarDbSqlProperties properties;
  private final PathMatchingResourcePatternResolver resourceResolver;
  private final String schemaLocation;

  public ScalarDbSqlMigrator(
      ScalarDbSqlProperties properties, PathMatchingResourcePatternResolver resourceResolver) {
    this(properties, resourceResolver, SCHEMA_RESOURCE);
  }

  ScalarDbSqlMigrator(
      ScalarDbSqlProperties properties,
      PathMatchingResourcePatternResolver resourceResolver,
      String schemaLocation) {
    this.properties = properties;
    this.resourceResolver = resourceResolver;
    this.schemaLocation = schemaLocation;
  }

  /**
   * Applies ScalarDB schema initialization and migration.
   *
   * <p>This method performs both initial schema provisioning and schema alterations to ensure the
   * ScalarDB schema matches the definition in {@code classpath:scalardb/schema.json}.
   *
   * <p><b>Warning:</b> The {@link SchemaLoader#alterTables(Properties, String, java.util.Map)} call
   * may trigger table rebuilds for incompatible schema changes. In production environments, this
   * could result in:
   *
   * <ul>
   *   <li>Downtime during table reconstruction
   *   <li>Temporary data unavailability
   *   <li>Increased resource consumption
   * </ul>
   *
   * <p>Since this migration runs automatically at application startup (via {@link
   * ScalarDbSqlMigrationRunner}), users currently have no control over when these potentially
   * disruptive operations occur. Future enhancements should provide explicit user control over
   * migration timing.
   *
   * @throws IllegalStateException if the schema resource cannot be found or the migration fails
   */
  public void migrate() {
    try {
      Resource schemaResource = resourceResolver.getResource(schemaLocation);
      if (!schemaResource.exists()) {
        throw new IllegalStateException("Schema resource not found: " + schemaLocation);
      }

      String schemaJson = readSchema(schemaResource);
      Properties scalarDbConfig = properties.toProperties();

      // Always invoke load before alterTables. Determining whether ScalarDB SQL requires only load,
      // only alterTables, or both is brittle in partially provisioned environments, so we accept
      // the warning log emitted when tables already exist in exchange for consistent migrations.
      SchemaLoader.load(scalarDbConfig, schemaJson, Collections.emptyMap(), true, false);
      logger.info("Provisioned ScalarDB schema from {}", schemaResource.getDescription());

      SchemaLoader.alterTables(scalarDbConfig, schemaJson, Collections.emptyMap());
      logger.info("Updated ScalarDB schema from {}", schemaResource.getDescription());
    } catch (IOException | SchemaLoaderException e) {
      throw new IllegalStateException("Failed to apply ScalarDB schema", e);
    }
  }

  public void unload() {
    try {
      Resource schemaResource = resourceResolver.getResource(schemaLocation);
      if (!schemaResource.exists()) {
        throw new IllegalStateException("Schema resource not found: " + schemaLocation);
      }

      String schemaJson = readSchema(schemaResource);
      Properties scalarDbConfig = properties.toProperties();

      SchemaLoader.unload(scalarDbConfig, schemaJson, false, false);
      logger.info("Unloaded ScalarDB schema from {}", schemaResource.getDescription());
    } catch (IOException | SchemaLoaderException e) {
      throw new IllegalStateException("Failed to unload ScalarDB schema", e);
    }
  }

  private String readSchema(Resource resource) throws IOException {
    try (InputStream in = resource.getInputStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
