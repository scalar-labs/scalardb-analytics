/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converter.reading;

import com.scalar.db.analytics.repository.impl.spring.converter.StringList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Converts H2 database's Object[] array representation to a {@link StringList} when reading from
 * the database. H2 2.x supports typed arrays like VARCHAR ARRAY, but returns them as Object[]
 * through JDBC. This converter handles the conversion from Object[] to StringList.
 */
@ReadingConverter
public class H2ArrayToStringListConverter implements Converter<Object[], StringList> {
  @Override
  public StringList convert(Object[] source) {
    // Convert each object to string
    List<String> strings =
        Arrays.stream(source)
            .map(obj -> obj != null ? obj.toString() : null)
            .collect(Collectors.toList());
    return new StringList(strings);
  }
}
