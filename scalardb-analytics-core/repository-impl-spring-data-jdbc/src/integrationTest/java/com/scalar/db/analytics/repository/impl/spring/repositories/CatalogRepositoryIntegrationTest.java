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
import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogRepositoryIntegrationTest extends AbstractScalarDbIntegrationTest {

  @Test
  void createShouldPersistCatalog() {
    Catalog catalog = Catalog.create("catalog-under-test");

    catalogRepository.create(ctx, catalog);

    Optional<Catalog> found = catalogRepository.findById(ctx, catalog.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("catalog-under-test");
  }

  @Test
  void createWithDuplicateNameShouldThrowEntityAlreadyExists() {
    catalogRepository.create(ctx, Catalog.create("duplicate"));

    assertThatThrownBy(() -> catalogRepository.create(ctx, Catalog.create("duplicate")))
        .isInstanceOf(AnalyticsException.class)
        .extracting(ex -> ((AnalyticsException) ex).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.CATALOG_ALREADY_EXISTS);
  }

  @Test
  void findByNameShouldReturnCatalogWhenExists() {
    Catalog catalog = Catalog.create("lookup-catalog");
    catalogRepository.create(ctx, catalog);

    Optional<Catalog> found = catalogRepository.findByName(ctx, "lookup-catalog");
    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(catalog.getId());
  }

  @Test
  void findByNameShouldReturnEmptyWhenMissing() {
    assertThat(catalogRepository.findByName(ctx, "missing")).isEmpty();
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
  void listShouldBeEmptyWhenNoCatalogsExist() {
    assertThat(catalogRepository.list(ctx)).isEmpty();
  }

  @Test
  void deleteByNameShouldRemoveCatalog() {
    Catalog catalog = Catalog.create("delete-by-name");
    catalogRepository.create(ctx, catalog);

    catalogRepository.deleteByName(ctx, "delete-by-name");

    assertThat(catalogRepository.findByName(ctx, "delete-by-name")).isEmpty();
  }

  @Test
  void deleteByNameShouldNotThrowWhenCatalogMissing() {
    assertThatCode(() -> catalogRepository.deleteByName(ctx, "missing-catalog"))
        .doesNotThrowAnyException();
  }

  @Test
  void findByIdShouldReturnCatalog() {
    UUID catalogId = createCatalog("find-by-id");

    Optional<Catalog> found = catalogRepository.findById(ctx, catalogId);
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("find-by-id");
  }

  @Test
  void findByIdShouldReturnEmptyWhenMissing() {
    assertThat(catalogRepository.findById(ctx, UUID.randomUUID())).isEmpty();
  }

  @Test
  void deleteByIdShouldRemoveCatalog() {
    UUID catalogId = createCatalog("delete-by-id");

    catalogRepository.deleteById(ctx, catalogId);

    assertThat(catalogRepository.findById(ctx, catalogId)).isEmpty();
  }

  @Test
  void deleteByIdShouldNotThrowWhenCatalogMissing() {
    assertThatCode(() -> catalogRepository.deleteById(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }
}
