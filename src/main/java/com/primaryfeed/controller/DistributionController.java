package com.primaryfeed.controller;

import com.primaryfeed.entity.Distribution;
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
@RequestMapping("/api/distributions")
@RequiredArgsConstructor
@Tag(name = "Distributions", description = "Manage food distributions to beneficiaries. Distributions record when food items are given out to recipients.")
@SecurityRequirement(name = "bearerAuth")
public class DistributionController {

    private final DistributionService distributionService;

    @Operation(summary = "Get all distributions", description = "Retrieve all distribution records across all branches")
    @GetMapping
    public List<Distribution> getAll() {
        return distributionService.findAll();
    }

    @Operation(summary = "Get distribution by ID", description = "Retrieve a specific distribution record by its distribution ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Distribution found"),
        @ApiResponse(responseCode = "404", description = "Distribution not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Distribution> getById(
        @Parameter(description = "Distribution ID", example = "1") @PathVariable Integer id) {
        return distributionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get distributions by branch", description = "Retrieve all distributions from a specific food bank branch")
    @GetMapping("/branch/{branchId}")
    public List<Distribution> getByBranch(
        @Parameter(description = "Branch ID", example = "1") @PathVariable Integer branchId) {
        return distributionService.findByBranchId(branchId);
    }

    @Operation(
        summary = "Get distributions by beneficiary",
        description = "Retrieve all distribution records for a specific beneficiary. Shows the complete food assistance history for that person or organization."
    )
    @GetMapping("/beneficiary/{beneficiaryId}")
    public List<Distribution> getByBeneficiary(
        @Parameter(description = "Beneficiary ID", example = "1") @PathVariable Integer beneficiaryId) {
        return distributionService.findByBeneficiaryId(beneficiaryId);
    }

    @Operation(summary = "Create distribution", description = "Record a new food distribution to a beneficiary")
    @ApiResponse(responseCode = "201", description = "Distribution created")
    @PostMapping
    public Distribution create(@RequestBody Distribution distribution) {
        return distributionService.save(distribution);
    }

    @Operation(summary = "Update distribution", description = "Update an existing distribution record")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Distribution updated"),
        @ApiResponse(responseCode = "404", description = "Distribution not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Distribution> update(
        @Parameter(description = "Distribution ID", example = "1") @PathVariable Integer id,
        @RequestBody Distribution distribution) {
        if (distributionService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        distribution.setDistributionId(id);
        return ResponseEntity.ok(distributionService.save(distribution));
    }

    @Operation(summary = "Delete distribution", description = "Delete a distribution record")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Distribution deleted"),
        @ApiResponse(responseCode = "404", description = "Distribution not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "Distribution ID", example = "1") @PathVariable Integer id) {
        if (distributionService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        distributionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
