/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NamespaceRepositoryIntegrationTest extends AbstractScalarDbIntegrationTest {

  private UUID catalogId;
  private UUID dataSourceId;

  @BeforeEach
  void setUpDataSource() {
    catalogId = createCatalog("namespace-catalog");
    dataSourceId = createPostgresDataSource(catalogId, "namespace-ds");
  }

  @Test
  void createShouldPersistNamespacesWithSingleLevelNames() {
    Namespace namespace = new Namespace(UUID.randomUUID(), dataSourceId, List.of("analytics"));

    namespaceRepository.create(ctx, namespace);

    Optional<Namespace> stored = namespaceRepository.findById(ctx, namespace.getId());
    assertThat(stored).isPresent();
    assertThat(stored.get().getNames()).containsExactly("analytics");
  }

  @Test
  void createShouldPersistNamespacesWithMultipleLevels() {
    List<String> names = List.of("db", "schema", "reporting");
    Namespace namespace = new Namespace(UUID.randomUUID(), dataSourceId, names);

    namespaceRepository.create(ctx, namespace);

    Optional<Namespace> stored = namespaceRepository.findById(ctx, namespace.getId());
    assertThat(stored).isPresent();
    assertThat(stored.get().getNames()).containsExactlyElementsOf(names);
  }

  @Test
  void createShouldAllowEmptyNames() {
    Namespace namespace = new Namespace(UUID.randomUUID(), dataSourceId, List.of());

    namespaceRepository.create(ctx, namespace);

    Optional<Namespace> stored = namespaceRepository.findById(ctx, namespace.getId());
    assertThat(stored).isPresent();
    assertThat(stored.get().getNames()).isEmpty();
  }

  @Test
  void createDuplicateShouldThrowEntityAlreadyExists() {
    List<String> names = List.of("dup", "schema");
    namespaceRepository.create(ctx, new Namespace(UUID.randomUUID(), dataSourceId, names));

    assertThatThrownBy(
            () ->
                namespaceRepository.create(
                    ctx, new Namespace(UUID.randomUUID(), dataSourceId, names)))
        .isInstanceOf(AnalyticsException.class)
        .extracting(ex -> ((AnalyticsException) ex).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.NAMESPACE_ALREADY_EXISTS);
  }

  @Test
  void findByDataSourceIdAndNamesShouldReturnNamespace() {
    List<String> names = List.of("sales", "china");
    Namespace namespace = new Namespace(UUID.randomUUID(), dataSourceId, names);
    namespaceRepository.create(ctx, namespace);

    Optional<Namespace> stored =
        namespaceRepository.findByDataSourceIdAndNames(ctx, dataSourceId, names);
    assertThat(stored).isPresent();
    assertThat(stored.get().getId()).isEqualTo(namespace.getId());
  }

  @Test
  void findByDataSourceIdAndNamesShouldReturnEmptyWhenMissing() {
    assertThat(
            namespaceRepository.findByDataSourceIdAndNames(ctx, dataSourceId, List.of("missing")))
        .isEmpty();
  }

  @Test
  void findByIdShouldReturnEmptyWhenNamespaceMissing() {
    assertThat(namespaceRepository.findById(ctx, UUID.randomUUID())).isEmpty();
  }

  @Test
  void deleteByIdShouldRemoveNamespace() {
    Namespace namespace = new Namespace(UUID.randomUUID(), dataSourceId, List.of("cleanup"));
    namespaceRepository.create(ctx, namespace);

    namespaceRepository.deleteById(ctx, namespace.getId());

    assertThat(namespaceRepository.findById(ctx, namespace.getId())).isEmpty();
  }

  @Test
  void deleteByIdShouldNotThrowWhenNamespaceMissing() {
    assertThatCode(() -> namespaceRepository.deleteById(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void queryServiceShouldReflectPersistedNamespaces() {
    namespaceRepository.create(ctx, new Namespace(UUID.randomUUID(), dataSourceId, List.of("ns1")));
    namespaceRepository.create(
        ctx, new Namespace(UUID.randomUUID(), dataSourceId, List.of("ns2", "sub")));

    List<DataSourceNamespace> namespaces =
        dataSourceNamespaceQueryService.listByCatalogName(ctx, "namespace-catalog");

    assertThat(namespaces).hasSize(2);
  }
}
