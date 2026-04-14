package com.primaryfeed.controller;

import com.primaryfeed.entity.FoodCategory;
import com.primaryfeed.entity.FoodItem;
import com.primaryfeed.entity.Inventory;
import com.primaryfeed.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // ── Inventory records ──────────────────────────────────────────────────────

    @GetMapping
    public List<Inventory> getAll() {
        return inventoryService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Inventory> getById(@PathVariable Integer id) {
        return inventoryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/branch/{branchId}")
    public List<Inventory> getByBranch(@PathVariable Integer branchId) {
        return inventoryService.findByBranchId(branchId);
    }

    @GetMapping("/expiring")
    public List<Inventory> getExpiring() {
        return inventoryService.findExpiringSoon(LocalDateTime.now().plusDays(3));
    }

    @GetMapping("/out-of-stock")
    public List<Inventory> getOutOfStock() {
        return inventoryService.findOutOfStock();
    }

    @PostMapping
    public Inventory create(@RequestBody Inventory inventory) {
        return inventoryService.save(inventory);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Inventory> update(@PathVariable Integer id, @RequestBody Inventory inventory) {
        if (inventoryService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        inventory.setInventoryId(id);
        return ResponseEntity.ok(inventoryService.save(inventory));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (inventoryService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        inventoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Food items ─────────────────────────────────────────────────────────────

    @GetMapping("/food-items")
    public List<FoodItem> getAllFoodItems() {
        return inventoryService.findAllFoodItems();
    }

    @GetMapping("/food-items/{sku}")
    public ResponseEntity<FoodItem> getFoodItemBySku(@PathVariable String sku) {
        return inventoryService.findFoodItemBySku(sku)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/food-items")
    public FoodItem createFoodItem(@RequestBody FoodItem item) {
        return inventoryService.saveFoodItem(item);
    }

    @PutMapping("/food-items/{sku}")
    public ResponseEntity<FoodItem> updateFoodItem(@PathVariable String sku, @RequestBody FoodItem item) {
        if (inventoryService.findFoodItemBySku(sku).isEmpty()) return ResponseEntity.notFound().build();
        item.setSku(sku);
        return ResponseEntity.ok(inventoryService.saveFoodItem(item));
    }

    @DeleteMapping("/food-items/{sku}")
    public ResponseEntity<Void> deleteFoodItem(@PathVariable String sku) {
        if (inventoryService.findFoodItemBySku(sku).isEmpty()) return ResponseEntity.notFound().build();
        inventoryService.deleteFoodItem(sku);
        return ResponseEntity.noContent().build();
    }

    // ── Food categories (read-only) ────────────────────────────────────────────

    @GetMapping("/categories")
    public List<FoodCategory> getAllCategories() {
        return inventoryService.findAllCategories();
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<FoodCategory> getCategoryById(@PathVariable Integer id) {
        return inventoryService.findCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
