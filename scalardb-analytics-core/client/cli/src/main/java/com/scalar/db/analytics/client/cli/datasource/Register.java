/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.codec.CodecObjectMapperFactory;
import com.scalar.db.analytics.api.codec.provider.DataSourceProviderCodec;
import com.scalar.db.analytics.api.codec.provider.ProviderCodecRegistry;
import com.scalar.db.analytics.api.codec.schema.DataSourceSchemaCodec;
import com.scalar.db.analytics.api.model.RegisterDataSourceResult;
import com.scalar.db.analytics.api.model.SkippedColumn;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.client.cli.CliRoot;
import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Failure;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.cli.util.FilePrefixSubstitutor;
import com.scalar.db.analytics.client.exception.ClientException;
import com.scalar.db.analytics.client.exception.ErrorDetail.DataSourceDefinitionError;
import com.scalar.db.analytics.client.module.Modules;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(name = "register", description = "Register a new data source")
public class Register implements Callable<Integer> {

  private static final int MAX_PAYLOAD_BYTES = 1_048_576; // 1 MiB
  private static final int READ_BUFFER_SIZE = 8192;

  static class ProviderPayloadOption {
    @Option(
        names = {"--provider-json"},
        description = "Inline JSON payload that describes the data source provider")
    @SuppressWarnings("NotNullFieldNotInitialized")
    private String json;

    @Option(
        names = {"--provider-file"},
        description = "Path to a JSON file that describes the data source provider")
    @SuppressWarnings("NotNullFieldNotInitialized")
    private Path file;

    @Option(
        names = {"--provider-stdin"},
        description = "Read the provider JSON payload from standard input")
    private boolean stdin;
  }

  static class SchemaPayloadOption {
    @Option(
        names = {"--schema-json"},
        description = "Inline JSON payload that describes the data source schema")
    @SuppressWarnings("NotNullFieldNotInitialized")
    private String json;

    @Option(
        names = {"--schema-file"},
        description = "Path to a JSON file that describes the data source schema")
    @SuppressWarnings("NotNullFieldNotInitialized")
    private Path file;
  }

  @Option(
      names = {"--catalog"},
      description = "Catalog name that owns the data source. " + CliRoot.CATALOG_OPTION_DEFAULT)
  @Nullable
  private String catalog;

  @Option(
      names = {"--data-source"},
      description = "Name of the data source to register",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String dataSource;

  @ArgGroup(exclusive = true, multiplicity = "1")
  @SuppressWarnings("NotNullFieldNotInitialized")
  private ProviderPayloadOption providerPayload;

  @ArgGroup(exclusive = true)
  private SchemaPayloadOption schemaPayload;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private DataSourceCommand parent;

  @Spec
  @SuppressWarnings("NotNullFieldNotInitialized")
  private CommandSpec spec;

  private final ObjectMapper objectMapper;
  private final DataSourceProviderCodec providerCodec;
  private final DataSourceSchemaCodec schemaCodec;

  public Register() {
    this.objectMapper = CodecObjectMapperFactory.create();
    this.providerCodec =
        new DataSourceProviderCodec(
            ProviderCodecRegistry.create(this.objectMapper), this.objectMapper);
    this.schemaCodec = new DataSourceSchemaCodec(this.objectMapper);
  }

  @Override
  public Integer call() {
    String catalogName = parent.root().resolveCatalog(catalog, spec);
    Modules modules = parent.root().loadModules();

    JsonNode providerNode;
    try {
      String providerJson = readProviderPayload();
      providerNode = objectMapper.readTree(providerJson);
      // Substitute file references in the provider JSON at JsonNode level
      providerNode = FilePrefixSubstitutor.substitute(providerNode);
    } catch (ClientException e) {
      printer().printFailure(new Failure(e.detail().getMessage()));
      return ExitCode.USAGE;
    } catch (IOException e) {
      printer().printFailure(new Failure("Failed to process provider payload: " + e.getMessage()));
      return ExitCode.USAGE;
    }

    DataSourceProvider provider;
    @Nullable DataSourceSchema schema;
    try {
      provider = deserializeProviderFromNode(providerNode);
      schema = readSchemaPayload();
      validateSchemaCompatibility(provider, schema);
    } catch (ClientException e) {
      printer().printFailure(new Failure(e.detail().getMessage()));
      return ExitCode.USAGE;
    }

    RegisterDataSourceRequest request =
        new RegisterDataSourceRequest(catalogName, dataSource, provider, schema);

    try {
      RegisterDataSourceResult result = modules.client().dataSource().register(request);
      printer().printSuccess(new Success(result.getDataSource()));
      printSkippedColumns(result.getSkippedColumns());
      return ExitCode.OK;
    } catch (Exception e) {
      printer().printError(new CommandError("Failed to register a data source", e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }

  private void printSkippedColumns(List<SkippedColumn> skippedColumns) {
    if (skippedColumns.isEmpty()) {
      return;
    }
    StringBuilder message =
        new StringBuilder(
            skippedColumns.size() == 1
                ? "Warning: 1 column was skipped because its source type is not supported:"
                : String.format(
                    "Warning: %d columns were skipped because their source types are not"
                        + " supported:",
                    skippedColumns.size()));
    for (SkippedColumn column : skippedColumns) {
      List<String> nameParts = new ArrayList<>(column.getNamespaceNames());
      nameParts.add(column.getTableName());
      nameParts.add(column.getColumnName());
      message
          .append(System.lineSeparator())
          .append(
              String.format(
                  "  %s (%s): %s",
                  String.join(".", nameParts), column.getSourceTypeName(), column.getReason()));
    }
    printer().printWarning(message.toString());
  }

  private String readProviderPayload() {
    if (providerPayload.json != null) {
      return requireNonEmpty(providerPayload.json, "Provider payload cannot be empty");
    }

    if (providerPayload.file != null) {
      try (InputStream in = Files.newInputStream(providerPayload.file)) {
        return requireNonEmpty(readFully(in), "Provider payload cannot be empty");
      } catch (IOException e) {
        throw new ClientException(
            new DataSourceDefinitionError(
                String.format("Failed to read provider payload: %s", providerPayload.file), e));
      }
    }

    if (providerPayload.stdin) {
      try {
        return requireNonEmpty(readFully(System.in), "Provider payload cannot be empty");
      } catch (IOException e) {
        throw new ClientException(
            new DataSourceDefinitionError("Failed to read provider payload from stdin", e));
      }
    }

    throw new ClientException(new DataSourceDefinitionError("Provider payload is required"));
  }

  private @Nullable DataSourceSchema readSchemaPayload() {
    if (schemaPayload == null) {
      return null;
    }

    if (schemaPayload.json != null) {
      return deserializeSchema(
          requireNonEmpty(schemaPayload.json, "Schema payload cannot be empty"));
    }

    if (schemaPayload.file != null) {
      try (InputStream in = Files.newInputStream(schemaPayload.file)) {
        return deserializeSchema(requireNonEmpty(readFully(in), "Schema payload cannot be empty"));
      } catch (IOException e) {
        throw new ClientException(
            new DataSourceDefinitionError(
                String.format("Failed to read schema payload: %s", schemaPayload.file), e));
      }
    }

    throw new ClientException(new DataSourceDefinitionError("Schema payload option is invalid"));
  }

  private String readFully(InputStream inputStream) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[READ_BUFFER_SIZE];
    int read;
    while ((read = inputStream.read(buffer)) != -1) {
      output.write(buffer, 0, read);
      if (output.size() > MAX_PAYLOAD_BYTES) {
        throw new IOException(
            String.format("Payload exceeds maximum size of %d bytes", MAX_PAYLOAD_BYTES));
      }
    }
    return output.toString(StandardCharsets.UTF_8.name());
  }

  private String requireNonEmpty(String payload, String errorMessage) {
    if (payload == null || payload.trim().isEmpty()) {
      throw new ClientException(new DataSourceDefinitionError(errorMessage));
    }
    return payload;
  }

  private DataSourceSchema deserializeSchema(String schemaJson) {
    try {
      // The codec returns null only for a null input, and schemaJson is non-null here.
      return Objects.requireNonNull(schemaCodec.deserialize(schemaJson));
    } catch (IllegalArgumentException e) {
      throw new ClientException(
          new DataSourceDefinitionError(
              String.format("Invalid data source schema configuration: %s", e.getMessage()), e));
    }
  }

  private DataSourceProvider deserializeProviderFromNode(JsonNode providerNode) {
    try {
      return providerCodec.deserialize(providerNode);
    } catch (IllegalArgumentException e) {
      throw new ClientException(
          new DataSourceDefinitionError(
              String.format("Invalid data source provider configuration: %s", e.getMessage()), e));
    } catch (Exception e) {
      String message =
          "Failed to process provider payload"
              .concat(e.getMessage() != null ? ": " + e.getMessage() : "");
      throw new ClientException(new DataSourceDefinitionError(message, e));
    }
  }

  // Enforce alignment between provider capabilities and CLI schema flags so users cannot supply
  // manual schemas to auto-resolving providers or omit required schemas for manual-only ones.
  private void validateSchemaCompatibility(
      DataSourceProvider provider, @Nullable DataSourceSchema schema) {
    boolean supportsResolution = provider.supportsSchemaResolution();
    if (supportsResolution && schema != null) {
      throw new ClientException(
          new DataSourceDefinitionError(
              String.format(
                  "Provider type '%s' resolves schemas automatically; remove manual schema input.",
                  provider.getType())));
    }
    if (!supportsResolution && schema == null) {
      throw new ClientException(
          new DataSourceDefinitionError(
              String.format(
                  "Provider type '%s' requires a schema; supply --schema-json or --schema-file.",
                  provider.getType())));
    }
  }
}
