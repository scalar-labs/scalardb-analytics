/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.scalardb.cluster.rpc.v1.DistributedTransactionAdminGrpc;
import com.scalar.db.analytics.grpc.generated.scalardb.cluster.rpc.v1.HasPrivilegeResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScalarDbPrivilegeClientImplTest {

  @Mock private DistributedTransactionAdminGrpc.DistributedTransactionAdminBlockingStub adminStub;

  private static final String AUTH_TOKEN = "test-token";

  private ScalarDbPrivilegeClientImpl client;

  @BeforeEach
  void setUp() {
    lenient().when(adminStub.withDeadlineAfter(anyLong(), any())).thenReturn(adminStub);

    client = new ScalarDbPrivilegeClientImpl(adminStub, 5000, Duration.ofSeconds(60), 1000);
  }

  @Test
  void hasSelectPrivilege_withTableAccess_shouldReturnTrue() {
    HasPrivilegeResponse response = HasPrivilegeResponse.newBuilder().setHasPrivilege(true).build();
    when(adminStub.hasPrivilege(any())).thenReturn(response);

    boolean result = client.hasSelectPrivilege(AUTH_TOKEN, "alice", "my_namespace", "my_table");

    assertThat(result).isTrue();
  }

  @Test
  void hasSelectPrivilege_withNamespaceAccess_shouldReturnTrue() {
    HasPrivilegeResponse response = HasPrivilegeResponse.newBuilder().setHasPrivilege(true).build();
    when(adminStub.hasPrivilege(any())).thenReturn(response);

    boolean result = client.hasSelectPrivilege(AUTH_TOKEN, "alice", "my_namespace", null);

    assertThat(result).isTrue();
  }

  @Test
  void hasSelectPrivilege_withNoPrivilege_shouldReturnFalse() {
    HasPrivilegeResponse response =
        HasPrivilegeResponse.newBuilder().setHasPrivilege(false).build();
    when(adminStub.hasPrivilege(any())).thenReturn(response);

    boolean result = client.hasSelectPrivilege(AUTH_TOKEN, "alice", "my_namespace", "my_table");

    assertThat(result).isFalse();
  }

  @Test
  void hasSelectPrivilege_shouldCacheResults() {
    HasPrivilegeResponse response = HasPrivilegeResponse.newBuilder().setHasPrivilege(true).build();
    when(adminStub.hasPrivilege(any())).thenReturn(response);

    client.hasSelectPrivilege(AUTH_TOKEN, "alice", "ns", "tbl");
    client.hasSelectPrivilege(AUTH_TOKEN, "alice", "ns", "tbl");

    verify(adminStub, times(1)).hasPrivilege(any());
  }

  @Test
  void hasSelectPrivilege_withDifferentKeys_shouldNotShareCache() {
    HasPrivilegeResponse response = HasPrivilegeResponse.newBuilder().setHasPrivilege(true).build();
    when(adminStub.hasPrivilege(any())).thenReturn(response);

    client.hasSelectPrivilege(AUTH_TOKEN, "alice", "ns1", "tbl");
    client.hasSelectPrivilege(AUTH_TOKEN, "alice", "ns2", "tbl");

    verify(adminStub, times(2)).hasPrivilege(any());
  }

  @Test
  void hasSelectPrivilege_withHasPrivilegeFailure_shouldThrow() {
    when(adminStub.hasPrivilege(any()))
        .thenThrow(new StatusRuntimeException(Status.DEADLINE_EXCEEDED));

    assertThatThrownBy(() -> client.hasSelectPrivilege(AUTH_TOKEN, "alice", "ns", "tbl"))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            e -> {
              AnalyticsException ae = (AnalyticsException) e;
              assertThat(ae.getErrorCode())
                  .isEqualTo(AnalyticsErrorCode.SCALARDB_CLUSTER_UNAVAILABLE);
            })
        .hasMessageContaining("unavailable");
  }
}
