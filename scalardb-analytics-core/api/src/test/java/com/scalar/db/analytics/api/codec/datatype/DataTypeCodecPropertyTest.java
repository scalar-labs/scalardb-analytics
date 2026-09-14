/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.datatype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.codec.CodecObjectMapperFactory;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.DataTypeKind;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/** Property-based tests for {@link DataTypeCodec}. */
class DataTypeCodecPropertyTest {

  private final DataTypeCodec codec;
  private final ObjectMapper objectMapper;

  {
    objectMapper = CodecObjectMapperFactory.create();
    codec = new DataTypeCodec(objectMapper);
  }

  @Property
  void byte_shouldRoundTripCorrectly() {
    assertSimpleTypeRoundTrip(DataType.Byte.INSTANCE, DataType.Byte.class, "BYTE");
  }

  @Property
  void smallInt_shouldRoundTripCorrectly() {
    assertSimpleTypeRoundTrip(DataType.SmallInt.INSTANCE, DataType.SmallInt.class, "SMALLINT");
  }

  @Property
  void int_shouldRoundTripCorrectly() {
    assertSimpleTypeRoundTrip(DataType.Int.INSTANCE, DataType.Int.class, "INT");
  }

  @Property
  void bigInt_shouldRoundTripCorrectly() {
    assertSimpleTypeRoundTrip(DataType.BigInt.INSTANCE, DataType.BigInt.class, "BIGINT");
  }

  @Property
  void float_shouldRoundTripCorrectly() {
    assertSimpleTypeRoundTrip(DataType.Float.INSTANCE, DataType.Float.class, "FLOAT");
  }

  @Property
  void double_shouldRoundTripCorrectly() {
    assertSimpleTypeRoundTrip(DataType.Double.INSTANCE, DataType.Double.class, "DOUBLE");
  }

  @Property
  void text_shouldRoundTripCorrectly() {
    assertSimpleTypeRoundTrip(DataType.Text.INSTANCE, DataType.Text.class, "TEXT");
  }

  @Property
  void blob_shouldRoundTripCorrectly() {
    assertSimpleTypeRoundTrip(DataType.Blob.INSTANCE, DataType.Blob.class, "BLOB");
  }

  @Property
  void boolean_shouldRoundTripCorrectly() {
    assertSimpleTypeRoundTrip(DataType.Boolean.INSTANCE, DataType.Boolean.class, "BOOLEAN");
  }

  @Property
  void date_shouldRoundTripCorrectly() {
    assertSimpleTypeRoundTrip(DataType.Date.INSTANCE, DataType.Date.class, "DATE");
  }

  @Property
  void time_shouldRoundTripCorrectly() {
    assertSimpleTypeRoundTrip(DataType.Time.INSTANCE, DataType.Time.class, "TIME");
  }

  @Property
  void timestamp_shouldRoundTripCorrectly() {
    assertSimpleTypeRoundTrip(DataType.Timestamp.INSTANCE, DataType.Timestamp.class, "TIMESTAMP");
  }

  @Property
  void timestampTZ_shouldRoundTripCorrectly() {
    assertSimpleTypeRoundTrip(
        DataType.TimestampTZ.INSTANCE, DataType.TimestampTZ.class, "TIMESTAMPTZ");
  }

  @Property
  void duration_shouldRoundTripCorrectly() {
    assertSimpleTypeRoundTrip(DataType.Duration.INSTANCE, DataType.Duration.class, "DURATION");
  }

  @Property
  void interval_shouldRoundTripCorrectly() {
    assertSimpleTypeRoundTrip(DataType.Interval.INSTANCE, DataType.Interval.class, "INTERVAL");
  }

  @Property
  void decimal_shouldRoundTripCorrectly(
      @ForAll("precision") int precision, @ForAll("scale") int scale) {
    // Given
    DataType.Decimal original =
        DataType.Decimal.builder().precision(precision).scale(scale).build();

    // When - serialize and deserialize
    String json = codec.serialize(original);
    DataType roundTripped = codec.deserialize(json);

    // Then
    assertThat(roundTripped).isInstanceOf(DataType.Decimal.class);
    DataType.Decimal result = (DataType.Decimal) roundTripped;
    assertThat(result.getPrecision()).isEqualTo(precision);
    assertThat(result.getScale()).isEqualTo(scale);
    assertThat(result).isEqualTo(original);
    try {
      JsonNode node = objectMapper.readTree(json);
      assertThat(node.get("kind").asText()).isEqualTo("DECIMAL");
      assertThat(node.get("precision").asInt()).isEqualTo(precision);
      assertThat(node.get("scale").asInt()).isEqualTo(scale);
    } catch (Exception e) {
      throw new AssertionError("Failed to parse serialized JSON", e);
    }
  }

  // Passing null violates the @NullMarked contract, but the runtime guard exists for callers that
  // do not enforce it, so the test deliberately passes null.
  @SuppressWarnings("NullAway")
  @Example
  void deserialize_withNullJson_shouldThrowException() {
    assertThatThrownBy(() -> codec.deserialize(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("JSON cannot be null or empty");
  }

  @Example
  void deserialize_withEmptyJson_shouldThrowException() {
    assertThatThrownBy(() -> codec.deserialize(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("JSON cannot be null or empty");
  }

  @Example
  void deserialize_withInvalidJson_shouldThrowException() {
    String invalidJson = "not a valid json";
    assertThatThrownBy(() -> codec.deserialize(invalidJson))
        .isInstanceOf(DataTypeCodecException.class)
        .hasMessageContaining("Failed to deserialize JSON to DataType");
  }

  @Example
  void deserialize_withMissingKindField_shouldThrowException() {
    String jsonWithoutKind = "{\"precision\":10,\"scale\":2}";
    assertThatThrownBy(() -> codec.deserialize(jsonWithoutKind))
        .isInstanceOf(DataTypeCodecException.class)
        .hasMessageContaining("Missing 'kind' field");
  }

  @Example
  void deserialize_withUnknownDataType_shouldThrowException() {
    String jsonWithUnknownType = "{\"kind\":\"UNKNOWN_TYPE\"}";
    assertThatThrownBy(() -> codec.deserialize(jsonWithUnknownType))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No enum constant");
  }

  // Passing null violates the @NullMarked contract, but the runtime guard exists for callers that
  // do not enforce it, so the test deliberately passes null.
  @SuppressWarnings("NullAway")
  @Example
  void serialize_withNullDataType_shouldThrowException() {
    assertThatThrownBy(() -> codec.serialize(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("DataType cannot be null");
  }

  @Example
  void serialize_shouldProduceValidJsonStructure() {
    // Given
    DataType.Decimal dataType = DataType.Decimal.builder().precision(10).scale(2).build();

    // When
    String json = codec.serialize(dataType);

    // Then
    assertThat(json).isNotNull();
    assertThat(json).contains("\"kind\"");
    assertThat(json).contains("\"precision\"");
    assertThat(json).contains("\"scale\"");
  }

  // Helper methods for reducing test redundancy
  private void assertSimpleTypeRoundTrip(
      DataType original, Class<? extends DataType> expectedClass, String expectedKind) {
    // When - serialize and deserialize
    String json = codec.serialize(original);
    DataType roundTripped = codec.deserialize(json);

    // Then
    assertThat(roundTripped).isInstanceOf(expectedClass);
    assertThat(roundTripped).isEqualTo(original);
    try {
      JsonNode node = objectMapper.readTree(json);
      assertThat(node.get("kind").asText()).isEqualTo(expectedKind);
    } catch (Exception e) {
      throw new AssertionError("Failed to parse serialized JSON", e);
    }
  }

  // Providers for jqwik
  @Provide
  Arbitrary<Integer> precision() {
    return Arbitraries.integers().between(1, 38);
  }

  @Provide
  Arbitrary<Integer> scale() {
    return Arbitraries.integers().between(0, 38);
  }

  // ========== String Literal Format Tests ==========

  @Example
  void deserialize_withStringLiteral_text_shouldWork() {
    String json = "\"TEXT\"";
    DataType result = codec.deserialize(json);

    assertThat(result).isInstanceOf(DataType.Text.class);
    assertThat(result.getKind()).isEqualTo(DataTypeKind.TEXT);
  }

  @Example
  void deserialize_withStringLiteral_int_shouldWork() {
    String json = "\"INT\"";
    DataType result = codec.deserialize(json);

    assertThat(result).isInstanceOf(DataType.Int.class);
    assertThat(result.getKind()).isEqualTo(DataTypeKind.INT);
  }

  @Example
  void deserialize_withStringLiteral_bigint_shouldWork() {
    String json = "\"BIGINT\"";
    DataType result = codec.deserialize(json);

    assertThat(result).isInstanceOf(DataType.BigInt.class);
    assertThat(result.getKind()).isEqualTo(DataTypeKind.BIGINT);
  }

  @Example
  void deserialize_withStringLiteral_decimal_shouldUseDefaults() {
    String json = "\"DECIMAL\"";
    DataType result = codec.deserialize(json);

    assertThat(result).isInstanceOf(DataType.Decimal.class);
    DataType.Decimal decimal = (DataType.Decimal) result;
    assertThat(decimal.getPrecision()).isEqualTo(38);
    assertThat(decimal.getScale()).isEqualTo(0);
  }

  @Example
  void deserialize_withStringLiteral_boolean_shouldWork() {
    String json = "\"BOOLEAN\"";
    DataType result = codec.deserialize(json);

    assertThat(result).isInstanceOf(DataType.Boolean.class);
    assertThat(result.getKind()).isEqualTo(DataTypeKind.BOOLEAN);
  }

  @Example
  void deserialize_withStringLiteral_lowercase_shouldWork() {
    String json = "\"text\"";
    DataType result = codec.deserialize(json);

    assertThat(result).isInstanceOf(DataType.Text.class);
    assertThat(result.getKind()).isEqualTo(DataTypeKind.TEXT);
  }

  @Example
  void deserialize_withStringLiteral_mixedCase_shouldWork() {
    String json = "\"TeXt\"";
    DataType result = codec.deserialize(json);

    assertThat(result).isInstanceOf(DataType.Text.class);
  }

  @Example
  void deserialize_withStringLiteral_timestamp_shouldWork() {
    String json = "\"TIMESTAMP\"";
    DataType result = codec.deserialize(json);

    assertThat(result).isInstanceOf(DataType.Timestamp.class);
    assertThat(result.getKind()).isEqualTo(DataTypeKind.TIMESTAMP);
  }

  @Example
  void deserialize_withStringLiteral_timestamptz_shouldWork() {
    String json = "\"TIMESTAMPTZ\"";
    DataType result = codec.deserialize(json);

    assertThat(result).isInstanceOf(DataType.TimestampTZ.class);
    assertThat(result.getKind()).isEqualTo(DataTypeKind.TIMESTAMPTZ);
  }

  @Example
  void deserialize_withStringLiteral_invalid_shouldThrowException() {
    String json = "\"INVALID_TYPE\"";

    assertThatThrownBy(() -> codec.deserialize(json))
        .isInstanceOf(DataTypeCodecException.class)
        .hasMessageContaining("Unknown DataType: INVALID_TYPE");
  }

  // ========== Object Format Still Works ==========

  @Example
  void deserialize_withObjectFormat_text_shouldStillWork() {
    String json = "{\"kind\":\"TEXT\"}";
    DataType result = codec.deserialize(json);

    assertThat(result).isInstanceOf(DataType.Text.class);
    assertThat(result.getKind()).isEqualTo(DataTypeKind.TEXT);
  }

  @Example
  void deserialize_withObjectFormat_decimal_shouldStillWork() {
    String json = "{\"kind\":\"DECIMAL\",\"precision\":10,\"scale\":2}";
    DataType result = codec.deserialize(json);

    assertThat(result).isInstanceOf(DataType.Decimal.class);
    DataType.Decimal decimal = (DataType.Decimal) result;
    assertThat(decimal.getPrecision()).isEqualTo(10);
    assertThat(decimal.getScale()).isEqualTo(2);
  }

  // ========== Comprehensive Type Coverage ==========

  @Property
  void allSimpleTypes_withStringLiteral_shouldDeserialize() {
    for (DataTypeKind kind : DataTypeKind.values()) {
      String json = "\"" + kind.name() + "\"";
      DataType result = codec.deserialize(json);
      assertThat(result.getKind()).isEqualTo(kind);
    }
  }

  // ========== Error Cases ==========

  @Example
  void deserialize_withNumberLiteral_shouldThrowException() {
    String json = "123";

    assertThatThrownBy(() -> codec.deserialize(json))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid JSON structure");
  }

  @Example
  void deserialize_withBooleanLiteral_shouldThrowException() {
    String json = "true";

    assertThatThrownBy(() -> codec.deserialize(json))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid JSON structure");
  }

  @Example
  void deserialize_withArrayLiteral_shouldThrowException() {
    String json = "[\"TEXT\"]";

    assertThatThrownBy(() -> codec.deserialize(json))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid JSON structure");
  }
}
