/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.{EitherValues, Inside, Inspectors, OptionValues}
import org.scalatest.diagrams.Diagrams

/** Base trait for all integration tests.
  *
  * Provides standardized test style and assertion utilities:
  *   - AnyFunSpec for describe/it style testing
  *   - Diagrams for clear assertion messages (use `assert` instead of matchers)
  *   - Inside for pattern matching assertions
  *   - OptionValues for convenient Option assertion (`value` method)
  *   - EitherValues for convenient Either assertion (`left.value`, `right.value`)
  *   - Inspectors for collection assertions
  *
  * Test lifecycle traits (BeforeAndAfterAll, BeforeAndAfterEach) should be mixed in by individual
  * test classes only when needed.
  */
trait BaseSpec
    extends AnyFunSpec
    with Diagrams
    with Inside
    with OptionValues
    with EitherValues
    with Inspectors
