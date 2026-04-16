package com.primaryfeed.controller;

import com.primaryfeed.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * All 17 insight query endpoints.
 * Restricted to STAFF at both the filter chain (SecurityConfig) and method level (@PreAuthorize).
 * No logic lives here — every method delegates directly to ReportService.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STAFF')")
@Tag(name = "Reports (Staff Only)", description = "17 insight queries for data analysis and reporting. Requires STAFF role.")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Q1: Food items at a specific branch", description = "Returns all food items with quantity > 0 at the specified branch")
    @GetMapping("/1")
    public List<Map<String, Object>> q1(
        @Parameter(description = "Branch ID", required = true, example = "1")
        @RequestParam Integer branchId) {
        return reportService.q1FoodItemsAtBranch(branchId);
    }

    @Operation(summary = "Q2: Food items expiring in 3 days", description = "Returns all food items expiring within the next 3 days across all branches")
    @GetMapping("/2")
    public List<Map<String, Object>> q2() {
        return reportService.q2ExpiringIn3Days();
    }

    @Operation(summary = "Q3: Total food quantity across all branches", description = "Returns the total quantity of each food item (SKU) aggregated across all branches. Useful for system-wide inventory overview.")
    @GetMapping("/3")
    public List<Map<String, Object>> q3() {
        return reportService.q3TotalQuantityAllBranches();
    }

    @Operation(summary = "Q4: Food items grouped by category", description = "Returns all food items organized by their category. Shows the relationship between food_items and food_categories tables.")
    @GetMapping("/4")
    public List<Map<String, Object>> q4() {
        return reportService.q4FoodItemsByCategory();
    }

    @Operation(summary = "Q5: Branch with most distributions last month", description = "Returns the branch that distributed the highest total quantity of food last month. Uses CTE for complex aggregation.")
    @GetMapping("/5")
    public List<Map<String, Object>> q5() {
        return reportService.q5TopDistributingBranchLastMonth();
    }

    @Operation(summary = "Q6: All volunteers at a specific branch", description = "Returns all volunteer profiles assigned to the specified branch. Shows volunteer availability and background check status.")
    @GetMapping("/6")
    public List<Map<String, Object>> q6(
        @Parameter(description = "Branch ID", required = true, example = "1")
        @RequestParam Integer branchId) {
        return reportService.q6VolunteersAtBranch(branchId);
    }

    @Operation(summary = "Q7: Volunteer hours (last 30 days)", description = "Returns volunteer shift hours using vw_volunteer_hours_log_last_30_days view")
    @GetMapping("/7")
    public List<Map<String, Object>> q7() {
        return reportService.q7VolunteerHoursLast30Days();
    }

    @Operation(
        summary = "Q8: Volunteers at branch during time window",
        description = "Returns all volunteers scheduled at a specific branch during a given time window on a specific date. Useful for shift staffing verification and coverage analysis. Time format: HH:mm:ss (e.g., 08:00:00)"
    )
    @GetMapping("/8")
    public List<Map<String, Object>> q8(
            @Parameter(description = "Branch ID", required = true, example = "1")
            @RequestParam Integer branchId,
            @Parameter(description = "Shift date (YYYY-MM-DD)", required = true, example = "2026-04-05")
            @RequestParam LocalDate date,
            @Parameter(description = "Window start time (HH:mm:ss)", required = true, example = "08:00:00", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "time"))
            @RequestParam LocalTime startTime,
            @Parameter(description = "Window end time (HH:mm:ss)", required = true, example = "13:00:00", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "time"))
            @RequestParam LocalTime endTime) {
        return reportService.q8VolunteersAtBranchWindow(branchId, date, startTime, endTime);
    }

    @Operation(summary = "Q9: Distribution history for a beneficiary", description = "Returns all distribution records for a specific beneficiary. Shows complete food assistance history including dates, quantities, and items received.")
    @GetMapping("/9")
    public List<Map<String, Object>> q9(
        @Parameter(description = "Beneficiary ID", required = true, example = "1")
        @RequestParam Integer beneficiaryId) {
        return reportService.q9DistributionHistoryForBeneficiary(beneficiaryId);
    }

    @Operation(summary = "Q10: Beneficiaries served per branch this week", description = "Returns the count of unique beneficiaries served at each branch during the current week. Useful for weekly impact reporting.")
    @GetMapping("/10")
    public List<Map<String, Object>> q10() {
        return reportService.q10BeneficiariesServedThisWeek();
    }

    @Operation(summary = "Q11: All donations from a specific donor", description = "Returns all donation records from a specific donor. Shows donation history including dates, quantities, and items donated.")
    @GetMapping("/11")
    public List<Map<String, Object>> q11(
        @Parameter(description = "Donor ID", required = true, example = "1")
        @RequestParam Integer donorId) {
        return reportService.q11DonationsFromDonor(donorId);
    }

    @Operation(summary = "Q12: Food received vs. distributed per branch", description = "Returns total food received (donations) vs. distributed (to beneficiaries) per branch. Shows net surplus/deficit for inventory management.")
    @GetMapping("/12")
    public List<Map<String, Object>> q12() {
        return reportService.q12NetSurplusPerBranch();
    }

    @Operation(
        summary = "Q13: Food items below quantity threshold",
        description = "Returns all food items with total quantity below a user-specified threshold across all branches. Useful for identifying low-stock items that need restocking."
    )
    @GetMapping("/13")
    public List<Map<String, Object>> q13(
        @Parameter(description = "Minimum quantity threshold", required = true, example = "10")
        @RequestParam Integer threshold) {
        return reportService.q13ItemsBelowThreshold(threshold);
    }

    @Operation(summary = "Q14: All users and their roles", description = "Returns all system users with their assigned roles (0=Staff, 1=Volunteer), status, and branch assignments. Useful for user management and access control auditing.")
    @GetMapping("/14")
    public List<Map<String, Object>> q14() {
        return reportService.q14AllUsersAndRoles();
    }

    @Operation(summary = "Q15: Most frequently donated/distributed categories", description = "Returns food categories ranked by frequency of donation or distribution transactions. Shows which types of food are most actively moving through the system.")
    @GetMapping("/15")
    public List<Map<String, Object>> q15() {
        return reportService.q15MostFrequentCategories();
    }

    @Operation(summary = "Q16: Daily distribution totals (last 14 days)", description = "Returns total quantity distributed per day for the last 14 days. Useful for identifying distribution trends and demand patterns. Casts distribution_date to DATE for daily grouping.")
    @GetMapping("/16")
    public List<Map<String, Object>> q16() {
        return reportService.q16DailyDistributionLast14Days();
    }

    @Operation(summary = "Q17: Branches with highest volunteer-to-distribution ratio", description = "Returns branches ranked by their volunteer count relative to distribution volume. Identifies which branches have the most volunteer support per distribution activity.")
    @GetMapping("/17")
    public List<Map<String, Object>> q17() {
        return reportService.q17VolunteerToDistributionRatio();
    }
}
