package com.primaryfeed.integration;

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

/**
 * End-to-end smoke test covering the complete donation-to-distribution workflow.
 * This test validates that the entire system works together: creating donors and beneficiaries,
 * processing donations (which trigger inventory creation), and processing distributions
 * (which trigger inventory decrements).
 */
public class SmokeTest extends BaseIntegrationTest {

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DonationItemRepository donationItemRepository;

    @Autowired
    private DistributionRepository distributionRepository;

    @Autowired
    private DistributionItemRepository distributionItemRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private FoodBankBranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TriggerLogRepository triggerLogRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Rollback
    void testCompleteDonationToDistributionFlow() {
        // ═══════════════════════════════════════════════════════════════
        // SETUP: Get existing test data
        // ═══════════════════════════════════════════════════════════════
        FoodBankBranch branch = branchRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Test branch not found"));
        User user = userRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Test user not found"));
        FoodItem foodItem = foodItemRepository.findById("SKU-001")
                .orElseThrow(() -> new RuntimeException("Test food item not found"));
        Donor donor = donorRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Test donor not found"));
        Beneficiary beneficiary = beneficiaryRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Test beneficiary not found"));

        // ═══════════════════════════════════════════════════════════════
        // PHASE 1: Create Donation
        // ═══════════════════════════════════════════════════════════════
        Donation donation = new Donation();
        donation.setBranch(branch);
        donation.setDonor(donor);
        donation.setUser(user);
        donation.setDonationDate(LocalDateTime.now());
        donation = donationRepository.save(donation);

        // Create donation item
        DonationItem donationItem = new DonationItem();
        donationItem.setDonation(donation);
        donationItem.setFoodItem(foodItem);
        donationItem.setQuantity(200);
        donationItem.setUnit("cans");
        donationItem.setExpiryDate(LocalDateTime.now().plusDays(90));
        donationItemRepository.save(donationItem);
        entityManager.flush();

        // ═══════════════════════════════════════════════════════════════
        // VERIFY: Donation created inventory via trigger
        // ═══════════════════════════════════════════════════════════════
        List<Inventory> inventoryAfterDonation = inventoryRepository.findByFoodItem_Sku("SKU-001");
        Inventory createdInventory = inventoryAfterDonation.stream()
                .filter(i -> i.getBranch().getBranchId().equals(1) &&
                             i.getUnit().equals("cans") &&
                             i.getQuantity() >= 200)
                .findFirst()
                .orElse(null);

        assertNotNull(createdInventory,
                "Donation trigger should have created inventory record");
        assertTrue(createdInventory.getQuantity() >= 200,
                "Inventory quantity should be at least 200 after donation");

        int quantityAfterDonation = createdInventory.getQuantity();

        // Verify trigger log
        List<TriggerLog> donationLogs = triggerLogRepository.findAll();
        boolean foundDonationLog = donationLogs.stream()
                .anyMatch(log -> log.getTriggerName().equals("trg_after_donation_items_insert") &&
                                 log.getMessage().contains("SKU-001"));
        assertTrue(foundDonationLog,
                "Should find trigger log for donation item insert");

        // ═══════════════════════════════════════════════════════════════
        // PHASE 2: Create Distribution
        // ═══════════════════════════════════════════════════════════════
        Distribution distribution = new Distribution();
        distribution.setBranch(branch);
        distribution.setBeneficiary(beneficiary);
        distribution.setUser(user);
        distribution.setDistributionDate(LocalDateTime.now());
        distribution = distributionRepository.save(distribution);

        // Create distribution item
        DistributionItem distributionItem = new DistributionItem();
        distributionItem.setDistribution(distribution);
        distributionItem.setInventory(createdInventory);
        distributionItem.setQuantity(50);
        distributionItemRepository.save(distributionItem);
        entityManager.flush();
        entityManager.clear();

        // ═══════════════════════════════════════════════════════════════
        // VERIFY: Distribution decremented inventory via trigger
        // ═══════════════════════════════════════════════════════════════
        Inventory inventoryAfterDistribution = inventoryRepository.findById(createdInventory.getInventoryId())
                .orElseThrow(() -> new RuntimeException("Inventory record disappeared"));

        assertEquals(quantityAfterDonation - 50, inventoryAfterDistribution.getQuantity(),
                "Distribution trigger should have decremented inventory by 50");

        // Verify trigger log
        List<TriggerLog> distributionLogs = triggerLogRepository.findAll();
        boolean foundDistributionLog = distributionLogs.stream()
                .anyMatch(log -> log.getTriggerName().equals("trg_after_distribution_items_insert") &&
                                 log.getMessage().contains(String.valueOf(createdInventory.getInventoryId())));
        assertTrue(foundDistributionLog,
                "Should find trigger log for distribution item insert");

        // ═══════════════════════════════════════════════════════════════
        // FINAL VERIFICATION: Complete workflow integrity
        // ═══════════════════════════════════════════════════════════════
        // Verify donation record exists
        Donation savedDonation = donationRepository.findById(donation.getDonationId())
                .orElse(null);
        assertNotNull(savedDonation, "Donation should be persisted");

        // Verify distribution record exists
        Distribution savedDistribution = distributionRepository.findById(distribution.getDistributionId())
                .orElse(null);
        assertNotNull(savedDistribution, "Distribution should be persisted");

        // Verify inventory is in valid state
        assertTrue(inventoryAfterDistribution.getQuantity() > 0,
                "Inventory should still have positive quantity");
        assertTrue(inventoryAfterDistribution.getQuantity() == 150 ||
                   inventoryAfterDistribution.getQuantity() >= 150,
                "Inventory should have expected quantity after donation and distribution");

        // ═══════════════════════════════════════════════════════════════
        // SUCCESS: Complete end-to-end workflow validated
        // ═══════════════════════════════════════════════════════════════
        System.out.println("✓ Smoke test passed: Complete donation-to-distribution workflow validated");
        System.out.println("  - Donor provided " + donationItem.getQuantity() + " " + donationItem.getUnit());
        System.out.println("  - Inventory created with quantity: " + quantityAfterDonation);
        System.out.println("  - Beneficiary received " + distributionItem.getQuantity() + " units");
        System.out.println("  - Final inventory quantity: " + inventoryAfterDistribution.getQuantity());
    }
}
