package com.primaryfeed.controller;

import com.primaryfeed.entity.DistributionItem;
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
@RequestMapping("/api/distribution-items")
@RequiredArgsConstructor
@Tag(name = "Distribution Items", description = "Manage line items for distributions. Each distribution (header) contains multiple items linking to inventory_id (not food_sku directly).")
@SecurityRequirement(name = "bearerAuth")
public class DistributionItemController {

    private final DistributionService distributionService;

    @Operation(
        summary = "Get items for a distribution",
        description = "Retrieve all line items for a specific distribution. Shows what food was given out, quantities, and which inventory records were decremented."
    )
    @GetMapping("/distribution/{distributionId}")
    public List<DistributionItem> getByDistribution(
        @Parameter(description = "Distribution ID", example = "1") @PathVariable Integer distributionId) {
        return distributionService.findItemsByDistributionId(distributionId);
    }

    @Operation(summary = "Get distribution item by ID", description = "Retrieve a specific distribution line item")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Distribution item found"),
        @ApiResponse(responseCode = "404", description = "Distribution item not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DistributionItem> getById(
        @Parameter(description = "Distribution item ID", example = "1") @PathVariable Integer id) {
        return distributionService.findItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Create distribution item",
        description = "Add a new line item to a distribution. IMPORTANT: Use inventory_id (not food_sku) to specify which inventory record to distribute from. The quantity will be decremented from that inventory record."
    )
    @ApiResponse(responseCode = "201", description = "Distribution item created")
    @PostMapping
    public DistributionItem create(@RequestBody DistributionItem item) {
        return distributionService.saveItem(item);
    }

    @Operation(summary = "Update distribution item", description = "Update a distribution line item (quantity, inventory_id, etc.)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Distribution item updated"),
        @ApiResponse(responseCode = "404", description = "Distribution item not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<DistributionItem> update(
        @Parameter(description = "Distribution item ID", example = "1") @PathVariable Integer id,
        @RequestBody DistributionItem item) {
        if (distributionService.findItemById(id).isEmpty()) return ResponseEntity.notFound().build();
        item.setDistributionItemId(id);
        return ResponseEntity.ok(distributionService.saveItem(item));
    }

    @Operation(summary = "Delete distribution item", description = "Remove a line item from a distribution")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Distribution item deleted"),
        @ApiResponse(responseCode = "404", description = "Distribution item not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "Distribution item ID", example = "1") @PathVariable Integer id) {
        if (distributionService.findItemById(id).isEmpty()) return ResponseEntity.notFound().build();
        distributionService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
