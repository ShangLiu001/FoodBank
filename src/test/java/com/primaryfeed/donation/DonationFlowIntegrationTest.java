package com.primaryfeed.donation;

import com.primaryfeed.BaseIntegrationTest;
import com.primaryfeed.entity.*;
import com.primaryfeed.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DonationFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DonationItemRepository donationItemRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private FoodBankBranchRepository branchRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TriggerLogRepository triggerLogRepository;

    @Test
    @Rollback
    void testCreateDonationWithItem_CreatesInventory() {
        // Arrange
        FoodBankBranch branch = branchRepository.findById(1).orElseThrow();
        Donor donor = donorRepository.findById(1).orElseThrow();
        User user = userRepository.findById(1).orElseThrow();
        FoodItem foodItem = foodItemRepository.findById("TEST-001").orElseThrow();

        Donation donation = new Donation();
        donation.setBranch(branch);
        donation.setDonor(donor);
        donation.setUser(user);
        donation.setDonationDate(LocalDateTime.now());
        donation = donationRepository.save(donation);

        DonationItem item = new DonationItem();
        item.setDonation(donation);
        item.setFoodItem(foodItem);
        item.setQuantity(100);
        item.setUnit("cans");
        item.setExpiryDate(LocalDateTime.now().plusDays(30));

        // Act
        donationItemRepository.save(item);
        entityManager.flush();
        entityManager.clear();

        // Assert - verify trigger side effect
        List<Inventory> inventory = inventoryRepository.findByFoodItem_Sku("TEST-001");
        assertFalse(inventory.isEmpty(), "Inventory should have been created by trigger");

        Inventory createdInventory = inventory.stream()
                .filter(i -> i.getBranch().getBranchId().equals(1))
                .findFirst()
                .orElse(null);

        assertNotNull(createdInventory, "Inventory for branch 1 should exist");
        assertEquals(100, createdInventory.getQuantity(), "Quantity should match donation");
        assertEquals("cans", createdInventory.getUnit(), "Unit should match donation");
    }

    @Test
    @Rollback
    void testSecondDonationSameBatch_IncrementsInventory() {
        // Arrange - create first donation
        FoodBankBranch branch = branchRepository.findById(1).orElseThrow();
        Donor donor = donorRepository.findById(1).orElseThrow();
        User user = userRepository.findById(1).orElseThrow();
        FoodItem foodItem = foodItemRepository.findById("TEST-001").orElseThrow();
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(60);

        Donation donation1 = new Donation();
        donation1.setBranch(branch);
        donation1.setDonor(donor);
        donation1.setUser(user);
        donation1.setDonationDate(LocalDateTime.now());
        donation1 = donationRepository.save(donation1);

        DonationItem item1 = new DonationItem();
        item1.setDonation(donation1);
        item1.setFoodItem(foodItem);
        item1.setQuantity(50);
        item1.setUnit("boxes");
        item1.setExpiryDate(expiryDate);
        donationItemRepository.save(item1);
        entityManager.flush();

        // Get initial quantity
        List<Inventory> inventoryBefore = inventoryRepository.findByFoodItem_Sku("TEST-001");
        int initialQuantity = inventoryBefore.stream()
                .filter(i -> i.getBranch().getBranchId().equals(1) &&
                             i.getUnit().equals("boxes"))
                .findFirst()
                .map(Inventory::getQuantity)
                .orElse(0);

        // Act - create second donation with same batch parameters
        Donation donation2 = new Donation();
        donation2.setBranch(branch);
        donation2.setDonor(donor);
        donation2.setUser(user);
        donation2.setDonationDate(LocalDateTime.now());
        donation2 = donationRepository.save(donation2);

        DonationItem item2 = new DonationItem();
        item2.setDonation(donation2);
        item2.setFoodItem(foodItem);
        item2.setQuantity(30);
        item2.setUnit("boxes");
        item2.setExpiryDate(expiryDate);  // Same expiry date
        donationItemRepository.save(item2);
        entityManager.flush();
        entityManager.clear();

        // Assert - verify trigger incremented existing batch
        List<Inventory> inventoryAfter = inventoryRepository.findByFoodItem_Sku("TEST-001");
        int finalQuantity = inventoryAfter.stream()
                .filter(i -> i.getBranch().getBranchId().equals(1) &&
                             i.getUnit().equals("boxes"))
                .findFirst()
                .map(Inventory::getQuantity)
                .orElse(0);

        assertEquals(initialQuantity + 30, finalQuantity,
                "Quantity should be incremented by second donation");
    }

    @Test
    @Rollback
    void testDonationDifferentExpiry_CreatesSeparateBatch() {
        // Arrange
        FoodBankBranch branch = branchRepository.findById(1).orElseThrow();
        Donor donor = donorRepository.findById(1).orElseThrow();
        User user = userRepository.findById(1).orElseThrow();
        FoodItem foodItem = foodItemRepository.findById("TEST-002").orElseThrow();

        // First donation with expiry date 1
        Donation donation1 = new Donation();
        donation1.setBranch(branch);
        donation1.setDonor(donor);
        donation1.setUser(user);
        donation1.setDonationDate(LocalDateTime.now());
        donation1 = donationRepository.save(donation1);

        DonationItem item1 = new DonationItem();
        item1.setDonation(donation1);
        item1.setFoodItem(foodItem);
        item1.setQuantity(20);
        item1.setUnit("kg");
        item1.setExpiryDate(LocalDateTime.now().plusDays(5));
        donationItemRepository.save(item1);
        entityManager.flush();

        // Second donation with expiry date 2
        Donation donation2 = new Donation();
        donation2.setBranch(branch);
        donation2.setDonor(donor);
        donation2.setUser(user);
        donation2.setDonationDate(LocalDateTime.now());
        donation2 = donationRepository.save(donation2);

        DonationItem item2 = new DonationItem();
        item2.setDonation(donation2);
        item2.setFoodItem(foodItem);
        item2.setQuantity(25);
        item2.setUnit("kg");
        item2.setExpiryDate(LocalDateTime.now().plusDays(10));  // Different expiry
        donationItemRepository.save(item2);
        entityManager.flush();
        entityManager.clear();

        // Assert - verify two separate batches created
        List<Inventory> inventory = inventoryRepository.findByFoodItem_Sku("TEST-002");
        long batchCount = inventory.stream()
                .filter(i -> i.getBranch().getBranchId().equals(1) &&
                             i.getUnit().equals("kg"))
                .count();

        assertEquals(2, batchCount, "Should create two separate inventory batches for different expiry dates");
    }

    @Test
    @Rollback
    void testTriggerLogsRecorded() {
        // Arrange
        FoodBankBranch branch = branchRepository.findById(1).orElseThrow();
        Donor donor = donorRepository.findById(1).orElseThrow();
        User user = userRepository.findById(1).orElseThrow();
        FoodItem foodItem = foodItemRepository.findById("TEST-001").orElseThrow();

        long initialLogCount = triggerLogRepository.count();

        Donation donation = new Donation();
        donation.setBranch(branch);
        donation.setDonor(donor);
        donation.setUser(user);
        donation.setDonationDate(LocalDateTime.now());
        donation = donationRepository.save(donation);

        DonationItem item = new DonationItem();
        item.setDonation(donation);
        item.setFoodItem(foodItem);
        item.setQuantity(15);
        item.setUnit("gallons");
        item.setExpiryDate(LocalDateTime.now().plusDays(7));

        // Act
        donationItemRepository.save(item);
        entityManager.flush();
        entityManager.clear();

        // Assert - verify trigger log was created
        long finalLogCount = triggerLogRepository.count();
        assertTrue(finalLogCount > initialLogCount,
                "Trigger should have logged the inventory creation");

        List<TriggerLog> logs = triggerLogRepository.findAll();
        boolean foundRelevantLog = logs.stream()
                .anyMatch(log -> log.getTriggerName().equals("trg_after_donation_items_insert") &&
                                 log.getMessage().contains("TEST-001"));

        assertTrue(foundRelevantLog, "Should find trigger log for the donation item insert");
    }
}
