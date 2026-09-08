/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.authz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccessControlEntryTest {

  @Test
  void createShouldPopulateAllFields() {
    UUID granteeId = UUID.randomUUID();
    UUID resourceId = UUID.randomUUID();

    AccessControlEntry entry =
        AccessControlEntry.create(GranteeType.USER, granteeId, resourceId, Permission.TABLE_READ);

    assertThat(entry.granteeType()).isEqualTo(GranteeType.USER);
    assertThat(entry.granteeId()).isEqualTo(granteeId);
    assertThat(entry.resourceId()).isEqualTo(resourceId);
    assertThat(entry.permission()).isEqualTo(Permission.TABLE_READ);
  }
}
