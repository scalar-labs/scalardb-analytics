/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.tables;

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
  void setupDataSource() {
    catalogId = createCatalog("namespace-catalog");
    dataSourceId = createPostgresDataSource(catalogId, "namespace-ds");
  }

  @Test
  void createAndFindById() {
    UUID namespaceId = UUID.randomUUID();
    Namespace namespace = new Namespace(namespaceId, dataSourceId, List.of("db", "schema"));

    namespaceRepository.create(ctx, namespace);

    Optional<Namespace> found = namespaceRepository.findById(ctx, namespaceId);
    assertThat(found).isPresent();
    assertThat(found.get().getNames()).containsExactly("db", "schema");
  }

  @Test
  void createWithEmptyNamesShouldSucceed() {
    UUID namespaceId = UUID.randomUUID();
    Namespace namespace = new Namespace(namespaceId, dataSourceId, List.of());

    namespaceRepository.create(ctx, namespace);

    Optional<Namespace> found = namespaceRepository.findById(ctx, namespaceId);
    assertThat(found).isPresent();
    assertThat(found.get().getNames()).isEmpty();
  }

  @Test
  void createDuplicateShouldThrowException() {
    List<String> names = List.of("dup", "schema");
    namespaceRepository.create(ctx, new Namespace(UUID.randomUUID(), dataSourceId, names));

    assertThatThrownBy(
            () ->
                namespaceRepository.create(
                    ctx, new Namespace(UUID.randomUUID(), dataSourceId, names)))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            ex ->
                assertThat(((AnalyticsException) ex).getErrorCode())
                    .isEqualTo(AnalyticsErrorCode.NAMESPACE_ALREADY_EXISTS));
  }

  @Test
  void findByDataSourceIdAndNamesShouldReturnNamespace() {
    List<String> names = List.of("sales", "reports");
    UUID nsId = UUID.randomUUID();
    namespaceRepository.create(ctx, new Namespace(nsId, dataSourceId, names));

    Optional<Namespace> found =
        namespaceRepository.findByDataSourceIdAndNames(ctx, dataSourceId, names);

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(nsId);
  }

  @Test
  void findByDataSourceIdAndNamesShouldReturnEmptyWhenMissing() {
    assertThat(
            namespaceRepository.findByDataSourceIdAndNames(ctx, dataSourceId, List.of("missing")))
        .isEmpty();
  }

  @Test
  void findByIdShouldReturnEmptyWhenMissing() {
    assertThat(namespaceRepository.findById(ctx, UUID.randomUUID())).isEmpty();
  }

  @Test
  void queryServiceShouldListNamespacesByCatalog() {
    namespaceRepository.create(ctx, new Namespace(UUID.randomUUID(), dataSourceId, List.of("ns1")));
    namespaceRepository.create(
        ctx, new Namespace(UUID.randomUUID(), dataSourceId, List.of("ns2", "sub")));

    List<DataSourceNamespace> namespaces =
        dataSourceNamespaceQueryService.listByCatalogName(ctx, "namespace-catalog");

    assertThat(namespaces).hasSize(2);
    assertThat(namespaces)
        .flatExtracting(ns -> ns.getNamespace().getNames())
        .contains("ns1", "ns2", "sub");
  }

  @Test
  void deleteByIdShouldRemoveNamespace() {
    UUID namespaceId = UUID.randomUUID();
    namespaceRepository.create(ctx, new Namespace(namespaceId, dataSourceId, List.of("cleanup")));

    namespaceRepository.deleteById(ctx, namespaceId);

    assertThat(namespaceRepository.findById(ctx, namespaceId)).isEmpty();
  }

  @Test
  void deleteByIdShouldNotThrowWhenMissing() {
    assertThatCode(() -> namespaceRepository.deleteById(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }
}
