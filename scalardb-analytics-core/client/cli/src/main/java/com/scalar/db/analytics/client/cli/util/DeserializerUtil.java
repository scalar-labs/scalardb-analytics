/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.scalar.db.analytics.client.exception.ErrorDetail;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class DeserializerUtil {
  /**
   * Validate the given node against the given field descriptors.
   *
   * <p>This method will check if the given node is an object and has all the required fields as
   * specified by the given field descriptors. If any of the required fields are missing or have
   * incorrect types, this method will return an error detail.
   *
   * @param node The JSON node to be validated
   * @param descs The field descriptors
   * @param errorFunc The function to create an error detail if any of the required fields are
   *     missing or have incorrect types
   * @return An optional error detail if the given node is invalid, otherwise an empty optional
   */
  public static Optional<ErrorDetail> validateObjectFields(
      JsonNode node, List<FieldDesc> descs, Function<String, ErrorDetail> errorFunc) {
    Preconditions.checkArgument(node.isObject());

    return descs.stream()
        .map(desc -> validateField(node, desc, errorFunc))
        .filter(Optional::isPresent)
        .findFirst()
        .flatMap(Function.identity());
  }

  private static Optional<ErrorDetail> validateField(
      JsonNode node, FieldDesc desc, Function<String, ErrorDetail> errorFunc) {
    if (!desc.required()) {
      return Optional.empty();
    }

    if (!node.has(desc.name())) {
      return Optional.of(errorFunc.apply(desc.name() + " is missing"));
    }

    switch (desc.type()) {
      case STRING:
        if (!node.get(desc.name()).isTextual()) {
          return Optional.of(errorFunc.apply(desc.name() + " must be a string"));
        }
        break;
      case OBJECT:
        if (!node.get(desc.name()).isObject()) {
          return Optional.of(errorFunc.apply(desc.name() + " must be an object"));
        }
        break;
    }

    return Optional.empty();
  }

  public enum FieldType {
    STRING,
    OBJECT,
  }

  /** Field descriptor, to be used to validate a JSON object. */
  public static class FieldDesc {
    private final String name;
    private final FieldType type;
    private final boolean required;

    public FieldDesc(String name, FieldType type, boolean required) {
      this.name = name;
      this.type = type;
      this.required = required;
    }

    public String name() {
      return name;
    }

    public FieldType type() {
      return type;
    }

    public boolean required() {
      return required;
    }
  }
}
