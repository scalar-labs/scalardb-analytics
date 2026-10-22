/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.service.datasource.SchemaMetadataValidator.Field;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class SchemaMetadataValidatorTest {

  private static final UUID DATA_SOURCE_ID = UUID.randomUUID();
  // Built from code points: Error Prone rejects bidi controls written in source.
  private static final String RLO = new String(Character.toChars(0x202E));
  private static final String LRI = new String(Character.toChars(0x2066));

  static Stream<Arguments> prohibitedNames() {
    return Stream.of(
        // name with the character, escaped form
        Arguments.of("a\nb", "a\\nb"),
        Arguments.of("a\rb", "a\\rb"),
        Arguments.of("a\tb", "a\\tb"),
        Arguments.of("a\u001b[31mb", "a\\u001b[31mb"),
        Arguments.of("a\u0000b", "a\\u0000b"),
        Arguments.of("a\u007fb", "a\\u007fb"),
        Arguments.of("a\u009bb", "a\\u009bb"),
        Arguments.of("a" + RLO + "b", "a\\u202eb"),
        Arguments.of("a" + LRI + "b", "a\\u2066b"));
  }

  @ParameterizedTest
  @MethodSource("prohibitedNames")
  void validateName_withProhibitedCharacter_shouldThrowWithEscapedValue(
      String name, String escaped) {
    assertThatThrownBy(() -> SchemaMetadataValidator.validateName(Field.COLUMN, List.of(), name))
        .isInstanceOfSatisfying(
            InvalidSchemaMetadataException.class,
            e -> {
              assertThat(e.getField()).isEqualTo(Field.COLUMN);
              assertThat(e.getValue()).isEqualTo(escaped);
              assertThat(e.getPath()).isEqualTo('"' + escaped + '"');
              assertThat(e.getMessage()).doesNotContain(name).contains(escaped);
            });
  }

  @ParameterizedTest
  @CsvSource({"顧客", "注文_2024", "café", "Ελληνικά", "emoji_😀", "has space", "a.b", "\"quoted\""})
  void validateName_withPrintableUnicode_shouldPass(String name) {
    assertThatCode(() -> SchemaMetadataValidator.validateName(Field.TABLE, List.of("ns"), name))
        .doesNotThrowAnyException();
  }

  @Test
  void isProhibited_shouldMatchControlCategoryAndBidiControlsExactly() {
    for (int cp = 0; cp <= Character.MAX_CODE_POINT; cp++) {
      boolean expected =
          Character.getType(cp) == Character.CONTROL
              || (cp >= 0x202A && cp <= 0x202E)
              || (cp >= 0x2066 && cp <= 0x2069);
      assertThat(SchemaMetadataValidator.isProhibited(cp)).as("U+%04X", cp).isEqualTo(expected);
    }
  }

  @Test
  void escape_shouldEscapeBackslashSoThatEscapedFormIsUnambiguous() {
    assertThat(SchemaMetadataValidator.escape("a\\nb")).isEqualTo("a\\\\nb");
    assertThat(SchemaMetadataValidator.escape("a\nb")).isEqualTo("a\\nb");
  }

  @Test
  void validate_withValidSchema_shouldPass() {
    assertThatCode(() -> SchemaMetadataValidator.validate(schema("公開", "顧客", "名前")))
        .doesNotThrowAnyException();
  }

  @Test
  void validate_withInvalidNamespace_shouldReportNamespace() {
    assertThatThrownBy(() -> SchemaMetadataValidator.validate(schema("ns\u001b", "tbl", "col")))
        .isInstanceOfSatisfying(
            InvalidSchemaMetadataException.class,
            e -> {
              assertThat(e.getField()).isEqualTo(Field.NAMESPACE);
              assertThat(e.getPath()).isEqualTo("\"ns\\u001b\"");
            });
  }

  @Test
  void validate_withInvalidTable_shouldReportTableWithPath() {
    assertThatThrownBy(() -> SchemaMetadataValidator.validate(schema("ns", "t\nbl", "col")))
        .isInstanceOfSatisfying(
            InvalidSchemaMetadataException.class,
            e -> {
              assertThat(e.getField()).isEqualTo(Field.TABLE);
              assertThat(e.getPath()).isEqualTo("\"ns\".\"t\\nbl\"");
              assertThat(e.getValue()).isEqualTo("t\\nbl");
            });
  }

  @Test
  void validate_withInvalidColumn_shouldReportColumnWithPath() {
    assertThatThrownBy(
            () -> SchemaMetadataValidator.validate(schema("ns", "tbl", "c" + RLO + "ol")))
        .isInstanceOfSatisfying(
            InvalidSchemaMetadataException.class,
            e -> {
              assertThat(e.getField()).isEqualTo(Field.COLUMN);
              assertThat(e.getPath()).isEqualTo("\"ns\".\"tbl\".\"c\\u202eol\"");
            });
  }

  @Test
  void validateName_forColumnType_shouldReportColumnPath() {
    assertThatThrownBy(
            () ->
                SchemaMetadataValidator.validateName(
                    Field.COLUMN_TYPE, List.of("ns", "tbl", "col"), "my\rtype"))
        .isInstanceOfSatisfying(
            InvalidSchemaMetadataException.class,
            e -> {
              assertThat(e.getField()).isEqualTo(Field.COLUMN_TYPE);
              assertThat(e.getPath()).isEqualTo("\"ns\".\"tbl\".\"col\"");
              assertThat(e.getValue()).isEqualTo("my\\rtype");
            });
  }

  @Test
  void validate_withQuoteInParentName_shouldKeepPathUnambiguous() {
    assertThatThrownBy(() -> SchemaMetadataValidator.validate(schema("a\".\"b", "t\nbl", "col")))
        .isInstanceOfSatisfying(
            InvalidSchemaMetadataException.class,
            e -> assertThat(e.getPath()).isEqualTo("\"a\\\".\\\"b\".\"t\\nbl\""));
  }

  private static ResolvedSchema schema(String namespaceName, String tableName, String columnName) {
    Namespace namespace = Namespace.create(DATA_SOURCE_ID, List.of(namespaceName));
    TableInfo table = TableInfo.create(namespace.getId(), tableName);
    Column column = Column.create(table.getId(), columnName, DataType.Int.INSTANCE, 0, true);
    return new ResolvedSchema(
        List.of(
            new ResolvedSchemaEntry(namespace, List.of(new TableDetail(table, List.of(column))))));
  }
}
