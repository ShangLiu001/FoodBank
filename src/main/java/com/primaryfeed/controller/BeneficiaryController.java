package com.primaryfeed.controller;

import com.primaryfeed.entity.Beneficiary;
import com.primaryfeed.service.DistributionService;
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
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
@Tag(name = "Beneficiaries", description = "Manage food assistance recipients. Beneficiaries are individuals or organizations that receive food from the food bank. They do not have system login access.")
@SecurityRequirement(name = "bearerAuth")
public class BeneficiaryController {

    private final DistributionService distributionService;

    @Operation(summary = "Get all beneficiaries", description = "Retrieve all registered beneficiaries")
    @GetMapping
    public List<Beneficiary> getAll() {
        return distributionService.findAllBeneficiaries();
    }

    @Operation(summary = "Get beneficiary by ID", description = "Retrieve a specific beneficiary's profile by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Beneficiary found"),
        @ApiResponse(responseCode = "404", description = "Beneficiary not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Beneficiary> getById(
        @Parameter(description = "Beneficiary ID", example = "1") @PathVariable Integer id) {
        return distributionService.findBeneficiaryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get eligible beneficiaries",
        description = "Retrieve only beneficiaries with eligibility_status = 1 (Eligible). Use this to find who can currently receive food assistance."
    )
    @GetMapping("/eligible")
    public List<Beneficiary> getEligible() {
        return distributionService.findBeneficiariesByEligibility((byte) 1);
    }

    @Operation(summary = "Create beneficiary", description = "Register a new beneficiary (recipient of food assistance)")
    @ApiResponse(responseCode = "201", description = "Beneficiary created")
    @PostMapping
    public Beneficiary create(@RequestBody Beneficiary beneficiary) {
        return distributionService.saveBeneficiary(beneficiary);
    }

    @Operation(summary = "Update beneficiary", description = "Update beneficiary information (e.g., eligibility status, contact info, household size)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Beneficiary updated"),
        @ApiResponse(responseCode = "404", description = "Beneficiary not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Beneficiary> update(
        @Parameter(description = "Beneficiary ID", example = "1") @PathVariable Integer id,
        @RequestBody Beneficiary beneficiary) {
        if (distributionService.findBeneficiaryById(id).isEmpty()) return ResponseEntity.notFound().build();
        beneficiary.setBeneficiaryId(id);
        return ResponseEntity.ok(distributionService.saveBeneficiary(beneficiary));
    }

    @Operation(summary = "Delete beneficiary", description = "Remove a beneficiary from the system")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Beneficiary deleted"),
        @ApiResponse(responseCode = "404", description = "Beneficiary not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "Beneficiary ID", example = "1") @PathVariable Integer id) {
        if (distributionService.findBeneficiaryById(id).isEmpty()) return ResponseEntity.notFound().build();
        distributionService.deleteBeneficiary(id);
        return ResponseEntity.noContent().build();
    }
}
