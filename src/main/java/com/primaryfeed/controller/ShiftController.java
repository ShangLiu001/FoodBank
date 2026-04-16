package com.primaryfeed.controller;

import com.primaryfeed.entity.VolunteerShift;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
@Tag(name = "Volunteer Shifts", description = "Schedule and manage volunteer shifts. Database triggers prevent overlapping shifts for the same volunteer across different branches on the same date. Back-to-back shifts (end time = start time) are allowed.")
@SecurityRequirement(name = "bearerAuth")
public class ShiftController {

    private final VolunteerService volunteerService;

    @Operation(summary = "Get all shifts", description = "Retrieve all volunteer shifts across all branches and dates")
    @GetMapping
    public List<VolunteerShift> getAll() {
        return volunteerService.findAllShifts();
    }

    @Operation(summary = "Get shift by ID", description = "Retrieve a specific volunteer shift")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Shift found"),
        @ApiResponse(responseCode = "404", description = "Shift not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<VolunteerShift> getById(
        @Parameter(description = "Shift ID", example = "1") @PathVariable Integer id) {
        return volunteerService.findShiftById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get shifts by volunteer",
        description = "Retrieve all shifts for a specific volunteer. Shows their complete shift history and upcoming schedule."
    )
    @GetMapping("/volunteer/{volunteerId}")
    public List<VolunteerShift> getByVolunteer(
        @Parameter(description = "Volunteer ID", example = "1") @PathVariable Integer volunteerId) {
        return volunteerService.findShiftsByVolunteerId(volunteerId);
    }

    @Operation(
        summary = "Get shifts by branch",
        description = "Retrieve all shifts at a specific branch. Useful for seeing branch staffing levels."
    )
    @GetMapping("/branch/{branchId}")
    public List<VolunteerShift> getByBranch(
        @Parameter(description = "Branch ID", example = "1") @PathVariable Integer branchId) {
        return volunteerService.findShiftsByBranchId(branchId);
    }

    @Operation(
        summary = "Get shifts by date",
        description = "Retrieve all shifts on a specific date across all branches. Useful for daily staffing overview."
    )
    @GetMapping("/date/{date}")
    public List<VolunteerShift> getByDate(
        @Parameter(description = "Shift date", example = "2024-06-01") @PathVariable LocalDate date) {
        return volunteerService.findShiftsByDate(date);
    }

    @Operation(
        summary = "Create shift",
        description = "Schedule a new volunteer shift. WARNING: Database triggers will prevent creation if the volunteer has overlapping shifts on the same date (even at different branches). Back-to-back shifts where previous end time equals new start time are allowed."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Shift created"),
        @ApiResponse(responseCode = "400", description = "Shift overlap detected - volunteer already scheduled during this time")
    })
    @PostMapping
    public VolunteerShift create(@RequestBody VolunteerShift shift) {
        return volunteerService.saveShift(shift);
    }

    @Operation(
        summary = "Update shift",
        description = "Update a volunteer shift (time, date, or branch). Overlap validation still applies."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Shift updated"),
        @ApiResponse(responseCode = "404", description = "Shift not found"),
        @ApiResponse(responseCode = "400", description = "Updated shift would overlap with another shift")
    })
    @PutMapping("/{id}")
    public ResponseEntity<VolunteerShift> update(
        @Parameter(description = "Shift ID", example = "1") @PathVariable Integer id,
        @RequestBody VolunteerShift shift) {
        if (volunteerService.findShiftById(id).isEmpty()) return ResponseEntity.notFound().build();
        shift.setShiftId(id);
        return ResponseEntity.ok(volunteerService.saveShift(shift));
    }

    @Operation(summary = "Delete shift", description = "Cancel/remove a volunteer shift")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Shift deleted"),
        @ApiResponse(responseCode = "404", description = "Shift not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "Shift ID", example = "1") @PathVariable Integer id) {
        if (volunteerService.findShiftById(id).isEmpty()) return ResponseEntity.notFound().build();
        volunteerService.deleteShift(id);
        return ResponseEntity.noContent().build();
    }
}
