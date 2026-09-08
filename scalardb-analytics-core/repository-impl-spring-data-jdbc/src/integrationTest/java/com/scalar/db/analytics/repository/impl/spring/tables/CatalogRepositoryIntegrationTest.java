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
import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogRepositoryIntegrationTest extends AbstractScalarDbIntegrationTest {

  @Test
  void createAndFindByName() {
    Catalog catalog = Catalog.create("catalog-create");

    catalogRepository.create(ctx, catalog);

    Optional<Catalog> found = catalogRepository.findByName(ctx, "catalog-create");
    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(catalog.getId());
  }

  @Test
  void createDuplicateShouldThrowException() {
    catalogRepository.create(ctx, Catalog.create("catalog-dup"));

    assertThatThrownBy(() -> catalogRepository.create(ctx, Catalog.create("catalog-dup")))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            ex ->
                assertThat(((AnalyticsException) ex).getErrorCode())
                    .isEqualTo(AnalyticsErrorCode.CATALOG_ALREADY_EXISTS));
  }

  @Test
  void listShouldReturnAllCatalogs() {
    catalogRepository.create(ctx, Catalog.create("catalog-1"));
    catalogRepository.create(ctx, Catalog.create("catalog-2"));
    catalogRepository.create(ctx, Catalog.create("catalog-3"));

    List<Catalog> catalogs = catalogRepository.list(ctx);

    assertThat(catalogs).hasSize(3);
    assertThat(catalogs)
        .extracting(Catalog::getName)
        .containsExactlyInAnyOrder("catalog-1", "catalog-2", "catalog-3");
  }

  @Test
  void listShouldReturnEmptyWhenNoCatalogs() {
    List<Catalog> catalogs = catalogRepository.list(ctx);

    assertThat(catalogs).isEmpty();
  }

  @Test
  void findByNameShouldReturnEmptyWhenMissing() {
    assertThat(catalogRepository.findByName(ctx, "missing")).isEmpty();
  }

  @Test
  void findByIdShouldReturnCatalog() {
    UUID catalogId = createCatalog("catalog-find-id");

    Optional<Catalog> found = catalogRepository.findById(ctx, catalogId);

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(catalogId);
  }

  @Test
  void findByIdShouldReturnEmptyWhenMissing() {
    assertThat(catalogRepository.findById(ctx, UUID.randomUUID())).isEmpty();
  }

  @Test
  void deleteByNameShouldRemoveCatalog() {
    Catalog catalog = Catalog.create("catalog-delete");
    catalogRepository.create(ctx, catalog);

    catalogRepository.deleteByName(ctx, "catalog-delete");

    assertThat(catalogRepository.findByName(ctx, "catalog-delete")).isEmpty();
  }

  @Test
  void deleteByNameShouldNotThrowWhenMissing() {
    assertThatCode(() -> catalogRepository.deleteByName(ctx, "does-not-exist"))
        .doesNotThrowAnyException();
  }

  @Test
  void deleteByIdShouldRemoveCatalog() {
    UUID catalogId = createCatalog("catalog-delete-id");

    catalogRepository.deleteById(ctx, catalogId);

    assertThat(catalogRepository.findById(ctx, catalogId)).isEmpty();
  }

  @Test
  void deleteByIdShouldNotThrowWhenMissing() {
    assertThatCode(() -> catalogRepository.deleteById(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }
}
