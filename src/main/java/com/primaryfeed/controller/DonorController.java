package com.primaryfeed.controller;

import com.primaryfeed.entity.Donor;
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
@RequestMapping("/api/donors")
@RequiredArgsConstructor
@Tag(name = "Donors", description = "Manage food donors (individuals or organizations that give food). Donors do not have system login access - their records are managed by staff.")
@SecurityRequirement(name = "bearerAuth")
public class DonorController {

    private final DonationService donationService;

    @Operation(summary = "Get all donors", description = "Retrieve all registered donors (individuals and organizations)")
    @GetMapping
    public List<Donor> getAll() {
        return donationService.findAllDonors();
    }

    @Operation(summary = "Get donor by ID", description = "Retrieve a specific donor's profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Donor found"),
        @ApiResponse(responseCode = "404", description = "Donor not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Donor> getById(
        @Parameter(description = "Donor ID", example = "1") @PathVariable Integer id) {
        return donationService.findDonorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Create donor",
        description = "Register a new donor. Use donor_type: 0 for Individual, 1 for Organization"
    )
    @ApiResponse(responseCode = "201", description = "Donor created")
    @PostMapping
    public Donor create(@RequestBody Donor donor) {
        return donationService.saveDonor(donor);
    }

    @Operation(summary = "Update donor", description = "Update donor information (contact details, type, etc.)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Donor updated"),
        @ApiResponse(responseCode = "404", description = "Donor not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Donor> update(
        @Parameter(description = "Donor ID", example = "1") @PathVariable Integer id,
        @RequestBody Donor donor) {
        if (donationService.findDonorById(id).isEmpty()) return ResponseEntity.notFound().build();
        donor.setDonorId(id);
        return ResponseEntity.ok(donationService.saveDonor(donor));
    }

    @Operation(summary = "Delete donor", description = "Remove a donor from the system")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Donor deleted"),
        @ApiResponse(responseCode = "404", description = "Donor not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "Donor ID", example = "1") @PathVariable Integer id) {
        if (donationService.findDonorById(id).isEmpty()) return ResponseEntity.notFound().build();
        donationService.deleteDonor(id);
        return ResponseEntity.noContent().build();
    }
}
