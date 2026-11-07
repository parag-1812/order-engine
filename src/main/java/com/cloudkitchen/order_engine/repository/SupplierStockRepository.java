package com.cloudkitchen.order_engine.repository;

import com.cloudkitchen.order_engine.supplier.SupplierStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierStockRepository extends JpaRepository<SupplierStockEntity, Long> {

    List<SupplierStockEntity> findBySupplierId(Long supplierId);

    List<SupplierStockEntity> findBySupplierIdAndInStockTrue(Long supplierId);

    @Modifying
    @Query(value = "INSERT INTO supplier_stock (supplier_id, catalog_ingredient_id, in_stock) "
            + "SELECT :supplierId, id, true FROM catalog_ingredients", nativeQuery = true)
    void stockEntireCatalog(@Param("supplierId") Long supplierId);
}
