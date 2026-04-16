package com.primaryfeed.controller;

import com.primaryfeed.entity.DonationItem;
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
@RequestMapping("/api/donation-items")
@RequiredArgsConstructor
@Tag(name = "Donation Items", description = "Manage line items for donations. Each donation (header) contains multiple donation items specifying SKU, quantity, unit, and expiry date.")
@SecurityRequirement(name = "bearerAuth")
public class DonationItemController {

    private final DonationService donationService;

    @Operation(
        summary = "Get items for a donation",
        description = "Retrieve all line items (food products) for a specific donation. Shows what food items were received, quantities, units, and expiry dates."
    )
    @GetMapping("/donation/{donationId}")
    public List<DonationItem> getByDonation(
        @Parameter(description = "Donation ID", example = "1") @PathVariable Integer donationId) {
        return donationService.findItemsByDonationId(donationId);
    }

    @Operation(summary = "Get donation item by ID", description = "Retrieve a specific donation line item")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Donation item found"),
        @ApiResponse(responseCode = "404", description = "Donation item not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DonationItem> getById(
        @Parameter(description = "Donation item ID", example = "1") @PathVariable Integer id) {
        return donationService.findItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Create donation item",
        description = "Add a new line item to a donation. Include food_sku, quantity, unit, and expiry_date. Note: The same SKU can appear multiple times in one donation with different expiry dates."
    )
    @ApiResponse(responseCode = "201", description = "Donation item created")
    @PostMapping
    public DonationItem create(@RequestBody DonationItem item) {
        return donationService.saveItem(item);
    }

    @Operation(summary = "Update donation item", description = "Update a donation line item (quantity, unit, expiry date, etc.)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Donation item updated"),
        @ApiResponse(responseCode = "404", description = "Donation item not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<DonationItem> update(
        @Parameter(description = "Donation item ID", example = "1") @PathVariable Integer id,
        @RequestBody DonationItem item) {
        if (donationService.findItemById(id).isEmpty()) return ResponseEntity.notFound().build();
        item.setDonationItemId(id);
        return ResponseEntity.ok(donationService.saveItem(item));
    }

    @Operation(summary = "Delete donation item", description = "Remove a line item from a donation")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Donation item deleted"),
        @ApiResponse(responseCode = "404", description = "Donation item not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "Donation item ID", example = "1") @PathVariable Integer id) {
        if (donationService.findItemById(id).isEmpty()) return ResponseEntity.notFound().build();
        donationService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
