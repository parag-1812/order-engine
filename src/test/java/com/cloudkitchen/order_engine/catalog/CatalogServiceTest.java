package com.cloudkitchen.order_engine.catalog;

import com.cloudkitchen.order_engine.repository.CatalogIngredientRepository;
import com.cloudkitchen.order_engine.repository.KitchenStockRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the coverage brain: how well a kitchen covers a dish given its
 * stock, honouring ingredient roles (core / substitutable / optional).
 */
class CatalogServiceTest {

    private final CatalogIngredientRepository catalogRepo = mock(CatalogIngredientRepository.class);
    private final KitchenStockRepository stockRepo = mock(KitchenStockRepository.class);
    private final CatalogService service = new CatalogService(catalogRepo, stockRepo);

    // Catalog ids used in tests
    private static final long PANEER = 2, TOFU = 7, SPINACH = 20, CORIANDER = 29, CASHEW = 42;

    private void kitchenStocks(long kitchenId, long... ids) {
        List<KitchenStockEntity> rows = java.util.Arrays.stream(ids)
                .mapToObj(id -> new KitchenStockEntity(kitchenId, id, true))
                .toList();
        when(stockRepo.findByKitchenIdAndInStockTrue(kitchenId)).thenReturn(rows);
    }

    private CatalogService.ResolvedSpec core(String name, long id) {
        return new CatalogService.ResolvedSpec(name, "CORE", id, null, null);
    }

    @Test
    void fullCoverageScores100() {
        kitchenStocks(1L, SPINACH, PANEER);
        CatalogService.Coverage cov = service.coverageForSpecs(1L,
                List.of(core("Spinach", SPINACH), core("Paneer", PANEER)));
        assertThat(cov.score()).isEqualTo(100);
        assertThat(cov.missing()).isEmpty();
        assertThat(cov.substitutions()).isEmpty();
    }

    @Test
    void optionalItemsDoNotCountAgainstCoverage() {
        kitchenStocks(1L, SPINACH); // no coriander
        CatalogService.Coverage cov = service.coverageForSpecs(1L, List.of(
                core("Spinach", SPINACH),
                new CatalogService.ResolvedSpec("Coriander", "OPTIONAL", CORIANDER, null, null)));
        assertThat(cov.score()).isEqualTo(100);
        assertThat(cov.missing()).isEmpty();
    }

    @Test
    void substitutableItemCountsWhenKitchenHasTheSubstitute() {
        kitchenStocks(1L, TOFU); // has Tofu, not Paneer
        CatalogService.Coverage cov = service.coverageForSpecs(1L, List.of(
                new CatalogService.ResolvedSpec("Paneer", "SUBSTITUTABLE", PANEER, "Tofu", TOFU)));
        assertThat(cov.score()).isEqualTo(100);
        assertThat(cov.missing()).isEmpty();
        assertThat(cov.substitutions()).containsExactly("Tofu instead of Paneer");
    }

    @Test
    void missingCoreItemIsReportedToSource() {
        kitchenStocks(1L, SPINACH); // no cashew
        CatalogService.Coverage cov = service.coverageForSpecs(1L, List.of(
                core("Spinach", SPINACH), core("Cashew", CASHEW)));
        assertThat(cov.score()).isEqualTo(50);
        assertThat(cov.missing()).containsExactly("Cashew");
    }

    @Test
    void noRequiredItemsIsTreatedAsFullyCovered() {
        kitchenStocks(1L);
        CatalogService.Coverage cov = service.coverageForSpecs(1L, List.of());
        assertThat(cov.score()).isEqualTo(100);
    }
}
