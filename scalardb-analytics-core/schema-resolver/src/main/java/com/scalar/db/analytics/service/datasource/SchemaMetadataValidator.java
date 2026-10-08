/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource;

import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.TableDetail;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The character-level contract for names that come from a data source's schema: namespace, table,
 * and column names, and the source type names of columns.
 *
 * <p>These names reach logs, API responses, and terminals. A name that contains a line break, a
 * terminal escape sequence, or a bidirectional override could forge a log line or alter what a
 * terminal shows, so such a name is rejected when the schema is ingested rather than escaped at
 * every place it is written. A name is never rewritten, because a rewritten name would no longer
 * identify the object in the data source.
 *
 * <p>The prohibited characters are defined by {@link #isProhibited(int)}. Everything else,
 * including printable non-Latin characters, is accepted.
 */
public final class SchemaMetadataValidator {

  /** The kind of name being validated, as reported in {@link InvalidSchemaMetadataException}. */
  public enum Field {
    NAMESPACE("namespace"),
    TABLE("table"),
    COLUMN("column"),
    COLUMN_TYPE("column_type");

    private final String label;

    Field(String label) {
      this.label = label;
    }

    /** Returns the name of this field as it appears in error metadata. */
    public String label() {
      return label;
    }
  }

  private SchemaMetadataValidator() {}

  /**
   * Returns whether a code point is prohibited in a name: a Unicode {@code Cc} control character
   * (C0 controls including TAB, LF, CR, and ESC; DEL; C1 controls) or a bidirectional embedding,
   * override, or isolate control.
   *
   * @param codePoint the code point to check
   * @return {@code true} if the code point is prohibited
   */
  public static boolean isProhibited(int codePoint) {
    return codePoint <= 0x1F // C0 controls
        || (codePoint >= 0x7F && codePoint <= 0x9F) // DEL and C1 controls
        || (codePoint >= 0x202A && codePoint <= 0x202E) // LRE, RLE, PDF, LRO, RLO
        || (codePoint >= 0x2066 && codePoint <= 0x2069); // LRI, RLI, FSI, PDI
  }

  /**
   * Validates every namespace, table, and column name in a resolved schema.
   *
   * @param schema the resolved schema
   * @throws InvalidSchemaMetadataException if a name contains a prohibited character
   */
  public static void validate(ResolvedSchema schema) throws InvalidSchemaMetadataException {
    for (ResolvedSchemaEntry entry : schema.getEntries()) {
      List<String> namespaceNames = entry.getNamespace().getNames();
      validateNamespace(namespaceNames);
      for (TableDetail table : entry.getTables()) {
        String tableName = table.getInfo().getName();
        validateName(Field.TABLE, namespaceNames, tableName);
        List<String> tablePath = append(namespaceNames, tableName);
        for (Column column : table.getColumns()) {
          validateName(Field.COLUMN, tablePath, column.getName());
        }
      }
    }
  }

  /**
   * Validates the names of a namespace.
   *
   * @param namespaceNames the names of the namespace, from the outermost level
   * @throws InvalidSchemaMetadataException if a name contains a prohibited character
   */
  public static void validateNamespace(List<String> namespaceNames)
      throws InvalidSchemaMetadataException {
    for (int i = 0; i < namespaceNames.size(); i++) {
      validateName(Field.NAMESPACE, namespaceNames.subList(0, i), namespaceNames.get(i));
    }
  }

  /**
   * Validates one name.
   *
   * @param field the kind of name
   * @param parentPath the names of the objects that contain the named one, from the outermost; for
   *     {@link Field#COLUMN_TYPE}, the path of the column itself
   * @param name the name to validate
   * @throws InvalidSchemaMetadataException if the name contains a prohibited character
   */
  public static void validateName(Field field, List<String> parentPath, String name)
      throws InvalidSchemaMetadataException {
    if (name.codePoints().anyMatch(SchemaMetadataValidator::isProhibited)) {
      List<String> path = field == Field.COLUMN_TYPE ? parentPath : append(parentPath, name);
      throw new InvalidSchemaMetadataException(field, formatPath(path), escape(name));
    }
  }

  /**
   * Returns a representation of a string that contains no prohibited characters: a backslash
   * becomes {@code \\}, TAB, LF, and CR become {@code \t}, {@code \n}, and {@code \r}, and any
   * other prohibited character becomes {@code \}{@code uXXXX}.
   *
   * @param value the string to escape
   * @return the escaped string
   */
  public static String escape(String value) {
    StringBuilder sb = new StringBuilder(value.length());
    value
        .codePoints()
        .forEach(
            cp -> {
              if (cp == '\\') {
                sb.append("\\\\");
              } else if (cp == '\t') {
                sb.append("\\t");
              } else if (cp == '\n') {
                sb.append("\\n");
              } else if (cp == '\r') {
                sb.append("\\r");
              } else if (isProhibited(cp)) {
                sb.append(String.format("\\u%04x", cp));
              } else {
                sb.appendCodePoint(cp);
              }
            });
    return sb.toString();
  }

  // Each name is escaped and double-quoted, so a dot or a quote in a name keeps the path
  // unambiguous: "public"."my.table"."col".
  private static String formatPath(List<String> path) {
    return path.stream()
        .map(name -> '"' + escape(name).replace("\"", "\\\"") + '"')
        .collect(Collectors.joining("."));
  }

  private static List<String> append(List<String> path, String name) {
    List<String> result = new ArrayList<>(path.size() + 1);
    result.addAll(path);
    result.add(name);
    return result;
  }
}
