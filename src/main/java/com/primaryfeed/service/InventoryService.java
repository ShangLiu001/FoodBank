package com.primaryfeed.service;

import com.primaryfeed.entity.FoodCategory;
import com.primaryfeed.entity.FoodItem;
import com.primaryfeed.entity.Inventory;
import com.primaryfeed.repository.FoodCategoryRepository;
import com.primaryfeed.repository.FoodItemRepository;
import com.primaryfeed.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final FoodItemRepository foodItemRepository;
    private final FoodCategoryRepository foodCategoryRepository;

    // Inventory records
    public List<Inventory> findAll() { return inventoryRepository.findAll(); }
    public Optional<Inventory> findById(Integer id) { return inventoryRepository.findById(id); }
    public List<Inventory> findByBranchId(Integer branchId) { return inventoryRepository.findByBranch_BranchId(branchId); }
    public List<Inventory> findByBranchIds(List<Integer> branchIds) { return inventoryRepository.findByBranch_BranchIdIn(branchIds); }
    public List<Inventory> findByFoodItemSku(String sku) { return inventoryRepository.findByFoodItem_Sku(sku); }
    public List<Inventory> findExpiringSoon(LocalDateTime cutoff) { return inventoryRepository.findExpiringSoon(cutoff); }
    public List<Inventory> findExpiringSoonByBranchIds(LocalDateTime cutoff, List<Integer> branchIds) { return inventoryRepository.findExpiringSoonByBranchIds(cutoff, branchIds); }
    public List<Inventory> findOutOfStock() { return inventoryRepository.findOutOfStock(); }
    public List<Inventory> findOutOfStockByBranchIds(List<Integer> branchIds) { return inventoryRepository.findOutOfStockByBranchIds(branchIds); }
    public Inventory save(Inventory inventory) { return inventoryRepository.save(inventory); }
    public void delete(Integer id) { inventoryRepository.deleteById(id); }

    // Food items
    public List<FoodItem> findAllFoodItems() { return foodItemRepository.findAll(); }
    public Optional<FoodItem> findFoodItemBySku(String sku) { return foodItemRepository.findById(sku); }
    public List<FoodItem> findFoodItemsByCategoryId(Integer categoryId) { return foodItemRepository.findByCategory_CategoryId(categoryId); }
    public List<FoodItem> searchFoodItemsByName(String name) { return foodItemRepository.findByFoodNameContainingIgnoreCase(name); }
    public FoodItem saveFoodItem(FoodItem item) { return foodItemRepository.save(item); }
    public void deleteFoodItem(String sku) { foodItemRepository.deleteById(sku); }

    // Food categories (read-only — see CLAUDE.md; category names drive tiered expiry logic)
    public List<FoodCategory> findAllCategories() { return foodCategoryRepository.findAll(); }
    public Optional<FoodCategory> findCategoryById(Integer id) { return foodCategoryRepository.findById(id); }
}
