package com.primaryfeed.controller;

import com.primaryfeed.entity.FoodCategory;
import com.primaryfeed.entity.FoodItem;
import com.primaryfeed.entity.Inventory;
import com.primaryfeed.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory & Food Items", description = "Manage inventory stock levels, food item catalog, and food categories. Inventory tracks actual stock at each branch with expiry dates.")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    // ── Inventory records ──────────────────────────────────────────────────────

    @Operation(
        summary = "Get all inventory records",
        description = "Retrieve all inventory stock records across all branches. Each record represents a specific food item (SKU) at a branch with an expiry date."
    )
    @GetMapping
    public List<Inventory> getAll() {
        return inventoryService.findAll();
    }

    @Operation(summary = "Get inventory by ID", description = "Retrieve a specific inventory record")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory record found"),
        @ApiResponse(responseCode = "404", description = "Inventory record not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Inventory> getById(
        @Parameter(description = "Inventory ID", example = "1") @PathVariable Integer id) {
        return inventoryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get inventory by branch",
        description = "Retrieve all inventory records for a specific branch. Shows current stock levels for that location."
    )
    @GetMapping("/branch/{branchId}")
    public List<Inventory> getByBranch(
        @Parameter(description = "Branch ID", example = "1") @PathVariable Integer branchId) {
        return inventoryService.findByBranchId(branchId);
    }

    @Operation(
        summary = "Get expiring inventory",
        description = "Retrieve inventory items expiring within the next 3 days. Use this to prioritize distribution of soon-to-expire items."
    )
    @GetMapping("/expiring")
    public List<Inventory> getExpiring() {
        return inventoryService.findExpiringSoon(LocalDateTime.now().plusDays(3));
    }

    @Operation(
        summary = "Get out-of-stock items",
        description = "Retrieve inventory records with quantity = 0. Useful for identifying items that need restocking."
    )
    @GetMapping("/out-of-stock")
    public List<Inventory> getOutOfStock() {
        return inventoryService.findOutOfStock();
    }

    @Operation(summary = "Create inventory record", description = "Add a new inventory record (stock at a branch)")
    @ApiResponse(responseCode = "201", description = "Inventory record created")
    @PostMapping
    public Inventory create(@RequestBody Inventory inventory) {
        return inventoryService.save(inventory);
    }

    @Operation(summary = "Update inventory record", description = "Update inventory quantity, expiry date, or other fields")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory updated"),
        @ApiResponse(responseCode = "404", description = "Inventory not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Inventory> update(
        @Parameter(description = "Inventory ID", example = "1") @PathVariable Integer id,
        @RequestBody Inventory inventory) {
        if (inventoryService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        inventory.setInventoryId(id);
        return ResponseEntity.ok(inventoryService.save(inventory));
    }

    @Operation(summary = "Delete inventory record", description = "Delete an inventory record")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Inventory deleted"),
        @ApiResponse(responseCode = "404", description = "Inventory not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "Inventory ID", example = "1") @PathVariable Integer id) {
        if (inventoryService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        inventoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Food items ─────────────────────────────────────────────────────────────

    @Operation(
        summary = "Get all food items",
        description = "Retrieve the complete food item catalog. These are the master records (SKU, name, category, storage conditions) - not actual stock levels."
    )
    @GetMapping("/food-items")
    public List<FoodItem> getAllFoodItems() {
        return inventoryService.findAllFoodItems();
    }

    @Operation(
        summary = "Get food item by SKU",
        description = "Retrieve a specific food item's details by SKU. Note: SKU is a string (e.g., 'SKU-001'), not an integer."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Food item found"),
        @ApiResponse(responseCode = "404", description = "Food item not found")
    })
    @GetMapping("/food-items/{sku}")
    public ResponseEntity<FoodItem> getFoodItemBySku(
        @Parameter(description = "Food item SKU", example = "SKU-001") @PathVariable String sku) {
        return inventoryService.findFoodItemBySku(sku)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create food item", description = "Add a new food item to the catalog")
    @ApiResponse(responseCode = "201", description = "Food item created")
    @PostMapping("/food-items")
    public FoodItem createFoodItem(@RequestBody FoodItem item) {
        return inventoryService.saveFoodItem(item);
    }

    @Operation(summary = "Update food item", description = "Update food item details (name, category, storage conditions, etc.)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Food item updated"),
        @ApiResponse(responseCode = "404", description = "Food item not found")
    })
    @PutMapping("/food-items/{sku}")
    public ResponseEntity<FoodItem> updateFoodItem(
        @Parameter(description = "Food item SKU", example = "SKU-001") @PathVariable String sku,
        @RequestBody FoodItem item) {
        if (inventoryService.findFoodItemBySku(sku).isEmpty()) return ResponseEntity.notFound().build();
        item.setSku(sku);
        return ResponseEntity.ok(inventoryService.saveFoodItem(item));
    }

    @Operation(summary = "Delete food item", description = "Remove a food item from the catalog")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Food item deleted"),
        @ApiResponse(responseCode = "404", description = "Food item not found")
    })
    @DeleteMapping("/food-items/{sku}")
    public ResponseEntity<Void> deleteFoodItem(
        @Parameter(description = "Food item SKU", example = "SKU-001") @PathVariable String sku) {
        if (inventoryService.findFoodItemBySku(sku).isEmpty()) return ResponseEntity.notFound().build();
        inventoryService.deleteFoodItem(sku);
        return ResponseEntity.noContent().build();
    }

    // ── Food categories (read-only) ────────────────────────────────────────────

    @Operation(
        summary = "Get all food categories",
        description = "Retrieve all food categories. Categories are reference data that drive expiry logic - DO NOT modify category names as they're used in database views."
    )
    @GetMapping("/categories")
    public List<FoodCategory> getAllCategories() {
        return inventoryService.findAllCategories();
    }

    @Operation(summary = "Get category by ID", description = "Retrieve a specific food category")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category found"),
        @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @GetMapping("/categories/{id}")
    public ResponseEntity<FoodCategory> getCategoryById(
        @Parameter(description = "Category ID", example = "1") @PathVariable Integer id) {
        return inventoryService.findCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
