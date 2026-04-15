package com.primaryfeed.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * All 17 insight queries from the project specification.
 * Uses native SQL via EntityManager — do not add business logic here.
 */
@Service
public class ReportService {

    @PersistenceContext
    private EntityManager em;

    // ── Helpers ────────────────────────────────────────────────────────────────

    private List<Map<String, Object>> toMaps(List<Tuple> rows) {
        return rows.stream().map(t -> {
            Map<String, Object> row = new LinkedHashMap<>();
            for (TupleElement<?> el : t.getElements()) {
                row.put(el.getAlias(), t.get(el.getAlias()));
            }
            return row;
        }).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> run(String sql, Map<String, Object> params) {
        var q = em.createNativeQuery(sql, Tuple.class);
        params.forEach(q::setParameter);
        return toMaps((List<Tuple>) q.getResultList());
    }

    // ── Query 1: All food items currently available at a specific branch ───────

    public List<Map<String, Object>> q1FoodItemsAtBranch(Integer branchId) {
        String sql = """
                SELECT fi.sku, fi.food_name, fi.storage_condition,
                       fc.category_name, i.quantity, i.unit, i.expiry_date, i.inventory_id
                FROM inventories i
                JOIN food_items fi ON i.food_sku = fi.sku
                JOIN food_categories fc ON fi.category_id = fc.category_id
                WHERE i.branch_id = :branchId AND i.quantity > 0
                ORDER BY fi.food_name
                """;
        return run(sql, Map.of("branchId", branchId));
    }

    // ── Query 2: Food items expiring within the next 3 days ───────────────────

    public List<Map<String, Object>> q2ExpiringIn3Days() {
        String sql = """
                SELECT fi.sku, fi.food_name, fc.category_name,
                       i.quantity, i.unit, i.expiry_date,
                       fbb.branch_name, i.inventory_id
                FROM inventories i
                JOIN food_items fi ON i.food_sku = fi.sku
                JOIN food_categories fc ON fi.category_id = fc.category_id
                JOIN food_bank_branches fbb ON i.branch_id = fbb.branch_id
                WHERE i.quantity > 0
                  AND i.expiry_date >= :start
                  AND i.expiry_date <= :cutoff
                ORDER BY i.expiry_date
                """;
        return run(sql, Map.of(
            "start",  LocalDate.now().atStartOfDay(),
            "cutoff", LocalDate.now().plusDays(3).atTime(LocalTime.MAX)
        ));
    }

    // ── Query 3: Total food quantity available across all branches ─────────────

    public List<Map<String, Object>> q3TotalQuantityAllBranches() {
        String sql = """
                SELECT fi.sku, fi.food_name, fc.category_name,
                       SUM(i.quantity) AS total_quantity, i.unit
                FROM inventories i
                JOIN food_items fi ON i.food_sku = fi.sku
                JOIN food_categories fc ON fi.category_id = fc.category_id
                WHERE i.quantity > 0
                GROUP BY fi.sku, fi.food_name, fc.category_name, i.unit
                ORDER BY total_quantity DESC
                """;
        return run(sql, Map.of());
    }

    // ── Query 4: Food items grouped by category ────────────────────────────────

    public List<Map<String, Object>> q4FoodItemsByCategory() {
        String sql = """
                SELECT fc.category_name, fc.category_description,
                       fi.sku, fi.food_name, fi.storage_condition
                FROM food_items fi
                JOIN food_categories fc ON fi.category_id = fc.category_id
                ORDER BY fc.category_name, fi.food_name
                """;
        return run(sql, Map.of());
    }

    // ── Query 5: Branch that distributed the most food last month (CTE) ────────

    public List<Map<String, Object>> q5TopDistributingBranchLastMonth() {
        String sql = """
                WITH monthly_totals AS (
                    SELECT d.branch_id, fbb.branch_name,
                           SUM(di.quantity) AS total_distributed
                    FROM distributions d
                    JOIN distribution_items di ON d.distribution_id = di.distribution_id
                    JOIN food_bank_branches fbb ON d.branch_id = fbb.branch_id
                    WHERE d.distribution_date >= DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 MONTH), '%Y-%m-01')
                      AND d.distribution_date < DATE_FORMAT(NOW(), '%Y-%m-01')
                    GROUP BY d.branch_id, fbb.branch_name
                )
                SELECT branch_id, branch_name, total_distributed
                FROM monthly_totals
                ORDER BY total_distributed DESC
                LIMIT 1
                """;
        return run(sql, Map.of());
    }

    // ── Query 6: All volunteers assigned to a specific branch ─────────────────

    public List<Map<String, Object>> q6VolunteersAtBranch(Integer branchId) {
        String sql = """
                SELECT v.volunteer_id, u.user_id, u.first_name, u.last_name,
                       u.email, u.phone, v.availability, v.background_check
                FROM volunteers v
                JOIN users u ON v.user_id = u.user_id
                WHERE u.branch_id = :branchId AND u.status = 1
                ORDER BY u.last_name, u.first_name
                """;
        return run(sql, Map.of("branchId", branchId));
    }

    // ── Query 7: Volunteer hours per volunteer in the last 30 days (view) ──────

    public List<Map<String, Object>> q7VolunteerHoursLast30Days() {
        return run("SELECT * FROM vw_volunteer_hours_log_last_30_days", Map.of());
    }

    // ── Query 8: Volunteers at a branch during a time window on a date ─────────

    public List<Map<String, Object>> q8VolunteersAtBranchWindow(
            Integer branchId, LocalDate shiftDate, LocalTime startTime, LocalTime endTime) {
        String sql = """
                SELECT v.volunteer_id, u.first_name, u.last_name, u.email,
                       vs.shift_date, vs.shift_time_start, vs.shift_time_end
                FROM volunteer_shifts vs
                JOIN volunteers v ON vs.volunteer_id = v.volunteer_id
                JOIN users u ON v.user_id = u.user_id
                WHERE vs.branch_id = :branchId
                  AND vs.shift_date = :shiftDate
                  AND vs.shift_time_start < :endTime
                  AND vs.shift_time_end > :startTime
                ORDER BY vs.shift_time_start
                """;
        return run(sql, Map.of(
                "branchId", branchId,
                "shiftDate", shiftDate,
                "startTime", startTime,
                "endTime", endTime));
    }

    // ── Query 9: Distribution history for a specific beneficiary ──────────────

    public List<Map<String, Object>> q9DistributionHistoryForBeneficiary(Integer beneficiaryId) {
        String sql = """
                SELECT d.distribution_id, d.distribution_date,
                       fbb.branch_name, fi.food_name, fc.category_name,
                       di.quantity, i.unit
                FROM distributions d
                JOIN distribution_items di ON d.distribution_id = di.distribution_id
                JOIN inventories i ON di.inventory_id = i.inventory_id
                JOIN food_items fi ON i.food_sku = fi.sku
                JOIN food_categories fc ON fi.category_id = fc.category_id
                JOIN food_bank_branches fbb ON d.branch_id = fbb.branch_id
                WHERE d.beneficiary_id = :beneficiaryId
                ORDER BY d.distribution_date DESC
                """;
        return run(sql, Map.of("beneficiaryId", beneficiaryId));
    }

    // ── Query 10: Number of beneficiaries served per branch this week ──────────

    public List<Map<String, Object>> q10BeneficiariesServedThisWeek() {
        String sql = """
                SELECT fbb.branch_id, fbb.branch_name,
                       COUNT(DISTINCT d.beneficiary_id) AS beneficiaries_served
                FROM distributions d
                JOIN food_bank_branches fbb ON d.branch_id = fbb.branch_id
                WHERE d.distribution_date >= DATE_SUB(CURDATE(), INTERVAL DAYOFWEEK(CURDATE()) - 1 DAY)
                GROUP BY fbb.branch_id, fbb.branch_name
                ORDER BY beneficiaries_served DESC
                """;
        return run(sql, Map.of());
    }

    // ── Query 11: All donations received from a specific donor ────────────────

    public List<Map<String, Object>> q11DonationsFromDonor(Integer donorId) {
        String sql = """
                SELECT don.donation_id, don.donation_date,
                       fbb.branch_name, fi.sku, fi.food_name, fc.category_name,
                       di.quantity, di.unit, di.expiry_date
                FROM donations don
                JOIN donation_items di ON don.donation_id = di.donation_id
                JOIN food_items fi ON di.food_sku = fi.sku
                JOIN food_categories fc ON fi.category_id = fc.category_id
                JOIN food_bank_branches fbb ON don.branch_id = fbb.branch_id
                WHERE don.donor_id = :donorId
                ORDER BY don.donation_date DESC
                """;
        return run(sql, Map.of("donorId", donorId));
    }

    // ── Query 12: Total food received vs. distributed per branch (net surplus) ─

    public List<Map<String, Object>> q12NetSurplusPerBranch() {
        String sql = """
                WITH received AS (
                    SELECT don.branch_id, SUM(di.quantity) AS total_received
                    FROM donations don
                    JOIN donation_items di ON don.donation_id = di.donation_id
                    GROUP BY don.branch_id
                ),
                distributed AS (
                    SELECT dist.branch_id, SUM(di.quantity) AS total_distributed
                    FROM distributions dist
                    JOIN distribution_items di ON dist.distribution_id = di.distribution_id
                    GROUP BY dist.branch_id
                )
                SELECT fbb.branch_id, fbb.branch_name,
                       COALESCE(r.total_received, 0) AS total_received,
                       COALESCE(d.total_distributed, 0) AS total_distributed,
                       COALESCE(r.total_received, 0) - COALESCE(d.total_distributed, 0) AS net_surplus
                FROM food_bank_branches fbb
                LEFT JOIN received r ON fbb.branch_id = r.branch_id
                LEFT JOIN distributed d ON fbb.branch_id = d.branch_id
                ORDER BY net_surplus DESC
                """;
        return run(sql, Map.of());
    }

    // ── Query 13: Food items below a user-supplied quantity threshold ──────────

    public List<Map<String, Object>> q13ItemsBelowThreshold(Integer threshold) {
        String sql = """
                SELECT fi.sku, fi.food_name, fc.category_name,
                       fbb.branch_name, i.quantity, i.unit
                FROM inventories i
                JOIN food_items fi ON i.food_sku = fi.sku
                JOIN food_categories fc ON fi.category_id = fc.category_id
                JOIN food_bank_branches fbb ON i.branch_id = fbb.branch_id
                WHERE i.quantity < :threshold
                ORDER BY i.quantity ASC, fi.food_name
                """;
        return run(sql, Map.of("threshold", threshold));
    }

    // ── Query 14: All users and their assigned roles ───────────────────────────

    public List<Map<String, Object>> q14AllUsersAndRoles() {
        String sql = """
                SELECT u.user_id, u.first_name, u.last_name, u.email, u.phone,
                       u.role,
                       CASE u.role WHEN 0 THEN 'Staff' WHEN 1 THEN 'Volunteer' ELSE 'Unknown' END AS role_name,
                       u.status, fbb.branch_name
                FROM users u
                LEFT JOIN food_bank_branches fbb ON u.branch_id = fbb.branch_id
                ORDER BY u.role, u.last_name, u.first_name
                """;
        return run(sql, Map.of());
    }

    // ── Query 15: Food categories most frequently donated or distributed ────────

    public List<Map<String, Object>> q15MostFrequentCategories() {
        String sql = """
                SELECT fc.category_name,
                       COALESCE(donated.qty, 0) AS total_donated,
                       COALESCE(distributed.qty, 0) AS total_distributed,
                       COALESCE(donated.qty, 0) + COALESCE(distributed.qty, 0) AS total_activity
                FROM food_categories fc
                LEFT JOIN (
                    SELECT fi.category_id, SUM(di.quantity) AS qty
                    FROM donation_items di
                    JOIN food_items fi ON di.food_sku = fi.sku
                    GROUP BY fi.category_id
                ) donated ON fc.category_id = donated.category_id
                LEFT JOIN (
                    SELECT fi.category_id, SUM(di.quantity) AS qty
                    FROM distribution_items di
                    JOIN inventories i ON di.inventory_id = i.inventory_id
                    JOIN food_items fi ON i.food_sku = fi.sku
                    GROUP BY fi.category_id
                ) distributed ON fc.category_id = distributed.category_id
                ORDER BY total_activity DESC
                """;
        return run(sql, Map.of());
    }

    // ── Query 16: Daily distribution totals for the last 14 days ──────────────

    public List<Map<String, Object>> q16DailyDistributionLast14Days() {
        String sql = """
                SELECT CAST(d.distribution_date AS DATE) AS distribution_day,
                       SUM(di.quantity) AS total_quantity,
                       COUNT(DISTINCT d.distribution_id) AS num_distributions
                FROM distributions d
                JOIN distribution_items di ON d.distribution_id = di.distribution_id
                WHERE d.distribution_date >= DATE_SUB(CURDATE(), INTERVAL 14 DAY)
                GROUP BY CAST(d.distribution_date AS DATE)
                ORDER BY distribution_day
                """;
        return run(sql, Map.of());
    }

    // ── Query 17: Branches with the highest volunteer-to-distribution ratio ────

    public List<Map<String, Object>> q17VolunteerToDistributionRatio() {
        String sql = """
                SELECT fbb.branch_id, fbb.branch_name,
                       COUNT(DISTINCT u.user_id) AS volunteer_count,
                       COUNT(DISTINCT d.distribution_id) AS distribution_count,
                       ROUND(
                           COUNT(DISTINCT u.user_id) / NULLIF(COUNT(DISTINCT d.distribution_id), 0),
                           4
                       ) AS volunteer_to_distribution_ratio
                FROM food_bank_branches fbb
                LEFT JOIN users u ON fbb.branch_id = u.branch_id AND u.role = 1 AND u.status = 1
                LEFT JOIN distributions d ON fbb.branch_id = d.branch_id
                GROUP BY fbb.branch_id, fbb.branch_name
                ORDER BY volunteer_to_distribution_ratio DESC
                """;
        return run(sql, Map.of());
    }
}
