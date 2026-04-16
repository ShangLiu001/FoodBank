package com.primaryfeed.volunteer;

import com.primaryfeed.BaseIntegrationTest;
import com.primaryfeed.entity.FoodBankBranch;
import com.primaryfeed.entity.Volunteer;
import com.primaryfeed.entity.VolunteerShift;
import com.primaryfeed.repository.FoodBankBranchRepository;
import com.primaryfeed.repository.VolunteerRepository;
import com.primaryfeed.repository.VolunteerShiftRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

public class ShiftOverlapTest extends BaseIntegrationTest {

    @Autowired
    private VolunteerShiftRepository shiftRepository;

    @Autowired
    private VolunteerRepository volunteerRepository;

    @Autowired
    private FoodBankBranchRepository branchRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Rollback
    void testOverlappingShifts_ThrowsException() {
        // Arrange - create first shift
        Volunteer volunteer = volunteerRepository.findById(1000).orElseThrow();
        FoodBankBranch branch = branchRepository.findById(1).orElseThrow();
        LocalDate today = LocalDate.now().plusDays(1);

        VolunteerShift shift1 = new VolunteerShift();
        shift1.setVolunteer(volunteer);
        shift1.setBranch(branch);
        shift1.setShiftDate(today);
        shift1.setShiftTimeStart(LocalTime.of(9, 0));
        shift1.setShiftTimeEnd(LocalTime.of(12, 0));
        shiftRepository.save(shift1);
        entityManager.flush();

        // Act & Assert - attempt to create overlapping shift
        VolunteerShift shift2 = new VolunteerShift();
        shift2.setVolunteer(volunteer);
        shift2.setBranch(branch);
        shift2.setShiftDate(today);
        shift2.setShiftTimeStart(LocalTime.of(10, 0));  // Overlaps with shift1
        shift2.setShiftTimeEnd(LocalTime.of(14, 0));

        assertThrows(Exception.class, () -> {
            shiftRepository.save(shift2);
            entityManager.flush();
        }, "Should throw exception when shifts overlap");
    }

    @Test
    @Rollback
    void testBackToBackShifts_Succeeds() {
        // Arrange
        Volunteer volunteer = volunteerRepository.findById(1000).orElseThrow();
        FoodBankBranch branch = branchRepository.findById(1).orElseThrow();
        LocalDate today = LocalDate.now().plusDays(2);

        // First shift
        VolunteerShift shift1 = new VolunteerShift();
        shift1.setVolunteer(volunteer);
        shift1.setBranch(branch);
        shift1.setShiftDate(today);
        shift1.setShiftTimeStart(LocalTime.of(9, 0));
        shift1.setShiftTimeEnd(LocalTime.of(12, 0));
        shiftRepository.save(shift1);
        entityManager.flush();

        // Act - create back-to-back shift (end time of shift1 = start time of shift2)
        VolunteerShift shift2 = new VolunteerShift();
        shift2.setVolunteer(volunteer);
        shift2.setBranch(branch);
        shift2.setShiftDate(today);
        shift2.setShiftTimeStart(LocalTime.of(12, 0));  // Exactly when shift1 ends
        shift2.setShiftTimeEnd(LocalTime.of(15, 0));

        // Assert - should succeed without exception
        assertDoesNotThrow(() -> {
            shiftRepository.save(shift2);
            entityManager.flush();
        }, "Back-to-back shifts should be allowed");

        // Verify both shifts were saved
        long shiftCount = shiftRepository.findAll().stream()
                .filter(s -> s.getVolunteer().getVolunteerId().equals(1000) &&
                             s.getShiftDate().equals(today))
                .count();
        assertEquals(2, shiftCount, "Both back-to-back shifts should be saved");
    }

    @Test
    @Rollback
    void testNonOverlappingShifts_Succeeds() {
        // Arrange
        Volunteer volunteer = volunteerRepository.findById(1000).orElseThrow();
        FoodBankBranch branch1 = branchRepository.findById(1).orElseThrow();
        FoodBankBranch branch2 = branchRepository.findById(2).orElseThrow();
        LocalDate today = LocalDate.now().plusDays(3);

        // First shift at branch 1
        VolunteerShift shift1 = new VolunteerShift();
        shift1.setVolunteer(volunteer);
        shift1.setBranch(branch1);
        shift1.setShiftDate(today);
        shift1.setShiftTimeStart(LocalTime.of(9, 0));
        shift1.setShiftTimeEnd(LocalTime.of(12, 0));
        shiftRepository.save(shift1);
        entityManager.flush();

        // Act - create shift at different time (even at different branch, should still check overlap)
        VolunteerShift shift2 = new VolunteerShift();
        shift2.setVolunteer(volunteer);
        shift2.setBranch(branch2);  // Different branch
        shift2.setShiftDate(today);
        shift2.setShiftTimeStart(LocalTime.of(14, 0));  // Different time, no overlap
        shift2.setShiftTimeEnd(LocalTime.of(17, 0));

        // Assert - should succeed without exception
        assertDoesNotThrow(() -> {
            shiftRepository.save(shift2);
            entityManager.flush();
        }, "Non-overlapping shifts should be allowed even at different branches");

        // Verify both shifts were saved
        long shiftCount = shiftRepository.findAll().stream()
                .filter(s -> s.getVolunteer().getVolunteerId().equals(1000) &&
                             s.getShiftDate().equals(today))
                .count();
        assertEquals(2, shiftCount, "Both non-overlapping shifts should be saved");
    }
}
