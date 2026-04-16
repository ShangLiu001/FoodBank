package com.primaryfeed.distribution;

import com.primaryfeed.BaseIntegrationTest;
import com.primaryfeed.entity.*;
import com.primaryfeed.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DistributionFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DistributionRepository distributionRepository;

    @Autowired
    private DistributionItemRepository distributionItemRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private FoodBankBranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DonationItemRepository donationItemRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private TriggerLogRepository triggerLogRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Rollback
    void testCreateDistribution_DecrementsInventory() {
        // Arrange - create inventory via donation first
        FoodBankBranch branch = branchRepository.findById(1).orElseThrow();
        Donor donor = donorRepository.findById(1).orElseThrow();
        User user = userRepository.findById(1).orElseThrow();
        FoodItem foodItem = foodItemRepository.findById("TEST-001").orElseThrow();
        Beneficiary beneficiary = beneficiaryRepository.findById(1).orElseThrow();

        // Create donation to establish inventory
        Donation donation = new Donation();
        donation.setBranch(branch);
        donation.setDonor(donor);
        donation.setUser(user);
        donation.setDonationDate(LocalDateTime.now());
        donation = donationRepository.save(donation);

        DonationItem donationItem = new DonationItem();
        donationItem.setDonation(donation);
        donationItem.setFoodItem(foodItem);
        donationItem.setQuantity(100);
        donationItem.setUnit("cans");
        donationItem.setExpiryDate(LocalDateTime.now().plusDays(30));
        donationItemRepository.save(donationItem);
        entityManager.flush();

        // Get inventory record
        List<Inventory> inventoryList = inventoryRepository.findByFoodItem_Sku("TEST-001");
        Inventory inventory = inventoryList.stream()
                .filter(i -> i.getBranch().getBranchId().equals(1))
                .findFirst()
                .orElseThrow();

        int initialQuantity = inventory.getQuantity();

        // Act - create distribution
        Distribution distribution = new Distribution();
        distribution.setBranch(branch);
        distribution.setBeneficiary(beneficiary);
        distribution.setUser(user);
        distribution.setDistributionDate(LocalDateTime.now());
        distribution = distributionRepository.save(distribution);

        DistributionItem distributionItem = new DistributionItem();
        distributionItem.setDistribution(distribution);
        distributionItem.setInventory(inventory);
        distributionItem.setQuantity(30);
        distributionItemRepository.save(distributionItem);
        entityManager.flush();
        entityManager.clear();

        // Assert - verify trigger decremented inventory
        Inventory updatedInventory = inventoryRepository.findById(inventory.getInventoryId()).orElseThrow();
        assertEquals(initialQuantity - 30, updatedInventory.getQuantity(),
                "Inventory should be decremented by distribution quantity");
    }

    @Test
    @Rollback
    void testDistributeInsufficientStock_ThrowsException() {
        // Arrange - create inventory with limited stock
        FoodBankBranch branch = branchRepository.findById(1).orElseThrow();
        Donor donor = donorRepository.findById(1).orElseThrow();
        User user = userRepository.findById(1).orElseThrow();
        FoodItem foodItem = foodItemRepository.findById("TEST-002").orElseThrow();
        Beneficiary beneficiary = beneficiaryRepository.findById(1).orElseThrow();

        // Create donation with only 10 units
        Donation donation = new Donation();
        donation.setBranch(branch);
        donation.setDonor(donor);
        donation.setUser(user);
        donation.setDonationDate(LocalDateTime.now());
        donation = donationRepository.save(donation);

        DonationItem donationItem = new DonationItem();
        donationItem.setDonation(donation);
        donationItem.setFoodItem(foodItem);
        donationItem.setQuantity(10);
        donationItem.setUnit("kg");
        donationItem.setExpiryDate(LocalDateTime.now().plusDays(5));
        donationItemRepository.save(donationItem);
        entityManager.flush();

        // Get inventory record
        List<Inventory> inventoryList = inventoryRepository.findByFoodItem_Sku("TEST-002");
        Inventory inventory = inventoryList.stream()
                .filter(i -> i.getBranch().getBranchId().equals(1))
                .findFirst()
                .orElseThrow();

        // Act & Assert - attempt to distribute more than available
        Distribution distribution = new Distribution();
        distribution.setBranch(branch);
        distribution.setBeneficiary(beneficiary);
        distribution.setUser(user);
        distribution.setDistributionDate(LocalDateTime.now());
        distribution = distributionRepository.save(distribution);

        DistributionItem distributionItem = new DistributionItem();
        distributionItem.setDistribution(distribution);
        distributionItem.setInventory(inventory);
        distributionItem.setQuantity(50);  // More than the 10 available

        Distribution finalDistribution = distribution;
        assertThrows(Exception.class, () -> {
            distributionItemRepository.save(distributionItem);
            entityManager.flush();
        }, "Should throw exception when trying to distribute more than available stock");
    }

    @Test
    @Rollback
    void testDistributionLogsRecorded() {
        // Arrange - create inventory via donation first
        FoodBankBranch branch = branchRepository.findById(1).orElseThrow();
        Donor donor = donorRepository.findById(1).orElseThrow();
        User user = userRepository.findById(1).orElseThrow();
        FoodItem foodItem = foodItemRepository.findById("TEST-003").orElseThrow();
        Beneficiary beneficiary = beneficiaryRepository.findById(2).orElseThrow();

        // Create donation to establish inventory
        Donation donation = new Donation();
        donation.setBranch(branch);
        donation.setDonor(donor);
        donation.setUser(user);
        donation.setDonationDate(LocalDateTime.now());
        donation = donationRepository.save(donation);

        DonationItem donationItem = new DonationItem();
        donationItem.setDonation(donation);
        donationItem.setFoodItem(foodItem);
        donationItem.setQuantity(50);
        donationItem.setUnit("lbs");
        donationItem.setExpiryDate(LocalDateTime.now().plusDays(14));
        donationItemRepository.save(donationItem);
        entityManager.flush();

        // Get inventory record
        List<Inventory> inventoryList = inventoryRepository.findByFoodItem_Sku("TEST-003");
        Inventory inventory = inventoryList.stream()
                .filter(i -> i.getBranch().getBranchId().equals(1))
                .findFirst()
                .orElseThrow();

        long initialLogCount = triggerLogRepository.count();

        // Act - create distribution
        Distribution distribution = new Distribution();
        distribution.setBranch(branch);
        distribution.setBeneficiary(beneficiary);
        distribution.setUser(user);
        distribution.setDistributionDate(LocalDateTime.now());
        distribution = distributionRepository.save(distribution);

        DistributionItem distributionItem = new DistributionItem();
        distributionItem.setDistribution(distribution);
        distributionItem.setInventory(inventory);
        distributionItem.setQuantity(20);
        distributionItemRepository.save(distributionItem);
        entityManager.flush();
        entityManager.clear();

        // Assert - verify trigger log was created
        long finalLogCount = triggerLogRepository.count();
        assertTrue(finalLogCount > initialLogCount,
                "Trigger should have logged the distribution");

        List<TriggerLog> logs = triggerLogRepository.findAll();
        boolean foundRelevantLog = logs.stream()
                .anyMatch(log -> log.getTriggerName().equals("trg_after_distribution_items_insert") &&
                                 log.getMessage().contains(String.valueOf(inventory.getInventoryId())));

        assertTrue(foundRelevantLog, "Should find trigger log for the distribution item insert");
    }
}
