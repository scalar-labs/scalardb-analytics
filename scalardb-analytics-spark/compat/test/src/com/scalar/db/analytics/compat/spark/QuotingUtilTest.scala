/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.compat.spark

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class QuotingUtilTest extends AnyFunSuite with Matchers {

  test("quoteIfNeeded should not quote valid identifiers") {
    QuotingUtil.quoteIfNeeded("validIdentifier") shouldBe "validIdentifier"
    QuotingUtil.quoteIfNeeded("table_name") shouldBe "table_name"
    QuotingUtil.quoteIfNeeded("Table123") shouldBe "Table123"
    QuotingUtil.quoteIfNeeded("_underscore") shouldBe "_underscore"
  }

  test("quoteIfNeeded should quote all-digit identifiers") {
    QuotingUtil.quoteIfNeeded("123") shouldBe "`123`"
    QuotingUtil.quoteIfNeeded("0") shouldBe "`0`"
    QuotingUtil.quoteIfNeeded("999") shouldBe "`999`"
  }

  test("quoteIfNeeded should quote identifiers with special characters") {
    QuotingUtil.quoteIfNeeded("table-name") shouldBe "`table-name`"
    QuotingUtil.quoteIfNeeded("table.name") shouldBe "`table.name`"
    QuotingUtil.quoteIfNeeded("table name") shouldBe "`table name`"
    QuotingUtil.quoteIfNeeded("table@name") shouldBe "`table@name`"
  }

  test("quoteIfNeeded should escape existing backticks") {
    QuotingUtil.quoteIfNeeded("table`name") shouldBe "`table``name`"
    QuotingUtil.quoteIfNeeded("`table`") shouldBe "```table```"
  }

  test("quoteIfNeeded should not quote identifiers starting with numbers but containing letters") {
    // This is the key behavior difference we discovered
    QuotingUtil.quoteIfNeeded("123table") shouldBe "123table"
    QuotingUtil.quoteIfNeeded("1abc") shouldBe "1abc"
    QuotingUtil.quoteIfNeeded("9test_table") shouldBe "9test_table"
  }

  test("quoted should join namespace parts with dots") {
    QuotingUtil.quoted(Array("database", "table")) shouldBe "database.table"
    QuotingUtil.quoted(Array("catalog", "schema", "table")) shouldBe "catalog.schema.table"
  }

  test("quoted should quote individual parts as needed") {
    QuotingUtil.quoted(Array("database", "table-name")) shouldBe "database.`table-name`"
    QuotingUtil.quoted(Array("123", "table")) shouldBe "`123`.table"
    QuotingUtil.quoted(Array("db", "123table", "my-table")) shouldBe "db.123table.`my-table`"
  }

  test("quoted should handle empty namespace") {
    QuotingUtil.quoted(Array.empty[String]) shouldBe ""
  }

  test("quoted should handle single element namespace") {
    QuotingUtil.quoted(Array("table")) shouldBe "table"
    QuotingUtil.quoted(Array("123")) shouldBe "`123`"
    QuotingUtil.quoted(Array("table-name")) shouldBe "`table-name`"
  }
}
