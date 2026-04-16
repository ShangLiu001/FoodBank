package com.primaryfeed.controller;

import com.primaryfeed.entity.Volunteer;
import com.primaryfeed.service.VolunteerService;
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
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
@Tag(name = "Volunteers", description = "Manage volunteer profiles (role-specific data). Create volunteer user accounts via /api/users with role=1. This endpoint manages the volunteer subtype record (availability, background check status).")
@SecurityRequirement(name = "bearerAuth")
public class VolunteerController {

    private final VolunteerService volunteerService;

    @Operation(summary = "Get all volunteers", description = "Retrieve all volunteer profile records")
    @GetMapping
    public List<Volunteer> getAll() {
        return volunteerService.findAll();
    }

    @Operation(summary = "Get volunteer by ID", description = "Retrieve a specific volunteer's profile by volunteer_id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Volunteer found"),
        @ApiResponse(responseCode = "404", description = "Volunteer not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Volunteer> getById(
        @Parameter(description = "Volunteer ID", example = "1") @PathVariable Integer id) {
        return volunteerService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get volunteers by branch",
        description = "Retrieve all volunteers assigned to a specific branch"
    )
    @GetMapping("/branch/{branchId}")
    public List<Volunteer> getByBranch(
        @Parameter(description = "Branch ID", example = "1") @PathVariable Integer branchId) {
        return volunteerService.findByBranchId(branchId);
    }

    @Operation(
        summary = "Create volunteer profile",
        description = "Create volunteer subtype record. NOTE: Typically created automatically via POST /api/users with role=1. Use this endpoint directly only if updating volunteer-specific fields."
    )
    @ApiResponse(responseCode = "201", description = "Volunteer profile created")
    @PostMapping
    public Volunteer create(@RequestBody Volunteer volunteer) {
        return volunteerService.save(volunteer);
    }

    @Operation(summary = "Update volunteer profile", description = "Update volunteer-specific fields (availability, background check status)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Volunteer updated"),
        @ApiResponse(responseCode = "404", description = "Volunteer not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Volunteer> update(
        @Parameter(description = "Volunteer ID", example = "1") @PathVariable Integer id,
        @RequestBody Volunteer volunteer) {
        if (volunteerService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        volunteer.setVolunteerId(id);
        return ResponseEntity.ok(volunteerService.save(volunteer));
    }

    @Operation(summary = "Delete volunteer profile", description = "Remove volunteer subtype record")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Volunteer deleted"),
        @ApiResponse(responseCode = "404", description = "Volunteer not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "Volunteer ID", example = "1") @PathVariable Integer id) {
        if (volunteerService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        volunteerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
