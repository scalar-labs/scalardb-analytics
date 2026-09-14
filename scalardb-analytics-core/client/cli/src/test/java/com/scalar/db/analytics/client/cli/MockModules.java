/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.client.module.Modules;
import com.scalar.db.analytics.sdk.ScalarDbAnalyticsClient;
import com.scalar.db.analytics.sdk.auth.InternalUserDirectoryClient;
import com.scalar.db.analytics.sdk.auth.UserClient;
import com.scalar.db.analytics.sdk.authz.PermissionClient;
import com.scalar.db.analytics.sdk.authz.RoleClient;
import com.scalar.db.analytics.sdk.catalog.CatalogClient;
import com.scalar.db.analytics.sdk.datasource.DataSourceClient;
import com.scalar.db.analytics.sdk.namespace.NamespaceClient;
import com.scalar.db.analytics.sdk.table.TableClient;

public class MockModules {
  public static Modules create() {
    ScalarDbAnalyticsClient client = mock(ScalarDbAnalyticsClient.class);

    // Mock individual clients
    CatalogClient catalogClient = mock(CatalogClient.class);
    DataSourceClient dataSourceClient = mock(DataSourceClient.class);
    NamespaceClient namespaceClient = mock(NamespaceClient.class);
    TableClient tableClient = mock(TableClient.class);
    InternalUserDirectoryClient internalUserDirectoryClient =
        mock(InternalUserDirectoryClient.class);
    UserClient userClient = mock(UserClient.class);
    RoleClient roleClient = mock(RoleClient.class);
    PermissionClient permissionClient = mock(PermissionClient.class);

    // Configure the main client to return the mocked clients
    when(client.catalog()).thenReturn(catalogClient);
    when(client.dataSource()).thenReturn(dataSourceClient);
    when(client.namespace()).thenReturn(namespaceClient);
    when(client.table()).thenReturn(tableClient);
    when(client.internalUserDirectory()).thenReturn(internalUserDirectoryClient);
    when(client.user()).thenReturn(userClient);
    when(client.role()).thenReturn(roleClient);
    when(client.permission()).thenReturn(permissionClient);

    return new Modules(client);
  }
}
