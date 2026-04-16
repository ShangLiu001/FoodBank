package com.primaryfeed.controller;

import com.primaryfeed.entity.Donation;
import com.primaryfeed.service.DonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
@Tag(name = "Donations", description = "Track food donations received from donors. Donations are header records - use /api/donation-items for line items.")
@SecurityRequirement(name = "bearerAuth")
public class DonationController {

    private final DonationService donationService;

    @Operation(summary = "Get all donations", description = "Retrieve all donation header records")
    @GetMapping
    public List<Donation> getAll() {
        return donationService.findAll();
    }

    @Operation(summary = "Get donation by ID", description = "Retrieve a specific donation record")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Donation found"),
        @ApiResponse(responseCode = "404", description = "Donation not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Donation> getById(
        @Parameter(description = "Donation ID", example = "1") @PathVariable Integer id) {
        return donationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get donations by branch", description = "Retrieve all donations received at a specific branch")
    @GetMapping("/branch/{branchId}")
    public List<Donation> getByBranch(
        @Parameter(description = "Branch ID", example = "1") @PathVariable Integer branchId) {
        return donationService.findByBranchId(branchId);
    }

    @Operation(
        summary = "Get donations by donor",
        description = "Retrieve all donation records from a specific donor. Shows complete donation history for that individual or organization."
    )
    @GetMapping("/donor/{donorId}")
    public List<Donation> getByDonor(
        @Parameter(description = "Donor ID", example = "1") @PathVariable Integer donorId) {
        return donationService.findByDonorId(donorId);
    }

    @Operation(summary = "Create donation", description = "Record a new donation header. Use /api/donation-items to add specific food items to this donation.")
    @ApiResponse(responseCode = "201", description = "Donation created")
    @PostMapping
    public Donation create(@RequestBody Donation donation) {
        return donationService.save(donation);
    }

    @Operation(summary = "Update donation", description = "Update an existing donation header record")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Donation updated"),
        @ApiResponse(responseCode = "404", description = "Donation not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Donation> update(
        @Parameter(description = "Donation ID", example = "1") @PathVariable Integer id,
        @RequestBody Donation donation) {
        if (donationService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        donation.setDonationId(id);
        return ResponseEntity.ok(donationService.save(donation));
    }

    @Operation(summary = "Delete donation", description = "Delete a donation record")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Donation deleted"),
        @ApiResponse(responseCode = "404", description = "Donation not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "Donation ID", example = "1") @PathVariable Integer id) {
        if (donationService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        donationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
