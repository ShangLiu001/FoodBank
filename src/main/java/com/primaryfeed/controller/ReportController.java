package com.primaryfeed.controller;

import com.primaryfeed.service.ReportService;
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
public class ReportController {

    private final ReportService reportService;

    /** Q1 — Food items currently available at a specific branch */
    @GetMapping("/1")
    public List<Map<String, Object>> q1(@RequestParam Integer branchId) {
        return reportService.q1FoodItemsAtBranch(branchId);
    }

    /** Q2 — Food items expiring within the next 3 days */
    @GetMapping("/2")
    public List<Map<String, Object>> q2() {
        return reportService.q2ExpiringIn3Days();
    }

    /** Q3 — Total food quantity available across all branches */
    @GetMapping("/3")
    public List<Map<String, Object>> q3() {
        return reportService.q3TotalQuantityAllBranches();
    }

    /** Q4 — Food items grouped by category */
    @GetMapping("/4")
    public List<Map<String, Object>> q4() {
        return reportService.q4FoodItemsByCategory();
    }

    /** Q5 — Branch that distributed the most food last month */
    @GetMapping("/5")
    public List<Map<String, Object>> q5() {
        return reportService.q5TopDistributingBranchLastMonth();
    }

    /** Q6 — All volunteers assigned to a specific branch */
    @GetMapping("/6")
    public List<Map<String, Object>> q6(@RequestParam Integer branchId) {
        return reportService.q6VolunteersAtBranch(branchId);
    }

    /** Q7 — Volunteer hours per volunteer in the last 30 days */
    @GetMapping("/7")
    public List<Map<String, Object>> q7() {
        return reportService.q7VolunteerHoursLast30Days();
    }

    /**
     * Q8 — Volunteers at a specific branch during a given time window on a given date.
     * Example: GET /api/reports/8?branchId=1&date=2024-06-01&startTime=09:00&endTime=17:00
     */
    @GetMapping("/8")
    public List<Map<String, Object>> q8(
            @RequestParam Integer branchId,
            @RequestParam LocalDate date,
            @RequestParam LocalTime startTime,
            @RequestParam LocalTime endTime) {
        return reportService.q8VolunteersAtBranchWindow(branchId, date, startTime, endTime);
    }

    /** Q9 — Distribution history for a specific beneficiary */
    @GetMapping("/9")
    public List<Map<String, Object>> q9(@RequestParam Integer beneficiaryId) {
        return reportService.q9DistributionHistoryForBeneficiary(beneficiaryId);
    }

    /** Q10 — Number of beneficiaries served per branch this week */
    @GetMapping("/10")
    public List<Map<String, Object>> q10() {
        return reportService.q10BeneficiariesServedThisWeek();
    }

    /** Q11 — All donations received from a specific donor */
    @GetMapping("/11")
    public List<Map<String, Object>> q11(@RequestParam Integer donorId) {
        return reportService.q11DonationsFromDonor(donorId);
    }

    /** Q12 — Total food received vs. distributed per branch (net surplus) */
    @GetMapping("/12")
    public List<Map<String, Object>> q12() {
        return reportService.q12NetSurplusPerBranch();
    }

    /**
     * Q13 — Food items below a user-supplied quantity threshold.
     * Example: GET /api/reports/13?threshold=10
     */
    @GetMapping("/13")
    public List<Map<String, Object>> q13(@RequestParam Integer threshold) {
        return reportService.q13ItemsBelowThreshold(threshold);
    }

    /** Q14 — All users and their assigned roles */
    @GetMapping("/14")
    public List<Map<String, Object>> q14() {
        return reportService.q14AllUsersAndRoles();
    }

    /** Q15 — Food categories most frequently donated or distributed */
    @GetMapping("/15")
    public List<Map<String, Object>> q15() {
        return reportService.q15MostFrequentCategories();
    }

    /** Q16 — Daily distribution totals for the last 14 days */
    @GetMapping("/16")
    public List<Map<String, Object>> q16() {
        return reportService.q16DailyDistributionLast14Days();
    }

    /** Q17 — Branches with the highest volunteer-to-distribution ratio */
    @GetMapping("/17")
    public List<Map<String, Object>> q17() {
        return reportService.q17VolunteerToDistributionRatio();
    }
}
