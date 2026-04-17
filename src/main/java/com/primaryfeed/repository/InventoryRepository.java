package com.primaryfeed.repository;
import com.primaryfeed.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {
    List<Inventory> findByBranch_BranchId(Integer branchId);
    List<Inventory> findByBranch_BranchIdIn(List<Integer> branchIds);
    List<Inventory> findByFoodItem_Sku(String sku);

    @Query("SELECT i FROM Inventory i WHERE i.quantity > 0 AND i.expiryDate <= :cutoff AND i.expiryDate >= CURRENT_TIMESTAMP")
    List<Inventory> findExpiringSoon(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT i FROM Inventory i WHERE i.quantity > 0 AND i.expiryDate <= :cutoff AND i.expiryDate >= CURRENT_TIMESTAMP AND i.branch.branchId IN :branchIds")
    List<Inventory> findExpiringSoonByBranchIds(@Param("cutoff") LocalDateTime cutoff, @Param("branchIds") List<Integer> branchIds);

    @Query("SELECT i FROM Inventory i WHERE i.quantity = 0")
    List<Inventory> findOutOfStock();

    @Query("SELECT i FROM Inventory i WHERE i.quantity = 0 AND i.branch.branchId IN :branchIds")
    List<Inventory> findOutOfStockByBranchIds(@Param("branchIds") List<Integer> branchIds);
}