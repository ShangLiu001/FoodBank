package com.primaryfeed.user;

import com.primaryfeed.BaseIntegrationTest;
import com.primaryfeed.entity.*;
import com.primaryfeed.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class UserCreationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private VolunteerRepository volunteerRepository;

    @Autowired
    private FoodBankBranchRepository branchRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Rollback
    void testCreateStaffUser_CreatesBothRows() {
        // Arrange
        FoodBankBranch branch = branchRepository.findById(1).orElseThrow();

        // Create user with role=0 (Staff)
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Staff");
        user.setEmail("newstaff@test.com");
        user.setPhone("617-555-9999");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole((byte) 0);  // Staff role
        user.setStatus((byte) 1);  // Active
        user.setBranch(branch);

        // Act - save user first
        user = userRepository.save(user);
        entityManager.flush();

        // Create staff record
        Staff staff = new Staff();
        staff.setUser(user);
        staff.setJobTitle("Test Manager");
        staff.setHireDate(LocalDateTime.now());

        staff = staffRepository.save(staff);
        entityManager.flush();
        entityManager.clear();

        // Assert - verify both rows exist
        User savedUser = userRepository.findById(user.getUserId()).orElse(null);
        assertNotNull(savedUser, "User should be saved");
        assertEquals((byte) 0, savedUser.getRole(), "User role should be Staff (0)");

        Staff savedStaff = staffRepository.findById(staff.getStaffId()).orElse(null);
        assertNotNull(savedStaff, "Staff record should be saved");
        assertEquals(user.getUserId(), savedStaff.getUser().getUserId(),
                "Staff record should reference the correct user");
        assertEquals("Test Manager", savedStaff.getJobTitle(),
                "Staff job title should be saved");
    }

    @Test
    @Rollback
    void testCreateVolunteerUser_CreatesBothRows() {
        // Arrange
        FoodBankBranch branch = branchRepository.findById(1).orElseThrow();

        // Create user with role=1 (Volunteer)
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Volunteer");
        user.setEmail("newvolunteer@test.com");
        user.setPhone("617-555-8888");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole((byte) 1);  // Volunteer role
        user.setStatus((byte) 1);  // Active
        user.setBranch(branch);

        // Act - save user first
        user = userRepository.save(user);
        entityManager.flush();

        // Create volunteer record
        Volunteer volunteer = new Volunteer();
        volunteer.setUser(user);
        volunteer.setAvailability("Weekends");
        volunteer.setBackgroundCheck((byte) 1);  // Cleared

        volunteer = volunteerRepository.save(volunteer);
        entityManager.flush();
        entityManager.clear();

        // Assert - verify both rows exist
        User savedUser = userRepository.findById(user.getUserId()).orElse(null);
        assertNotNull(savedUser, "User should be saved");
        assertEquals((byte) 1, savedUser.getRole(), "User role should be Volunteer (1)");

        Volunteer savedVolunteer = volunteerRepository.findById(volunteer.getVolunteerId()).orElse(null);
        assertNotNull(savedVolunteer, "Volunteer record should be saved");
        assertEquals(user.getUserId(), savedVolunteer.getUser().getUserId(),
                "Volunteer record should reference the correct user");
        assertEquals("Weekends", savedVolunteer.getAvailability(),
                "Volunteer availability should be saved");
        assertEquals((byte) 1, savedVolunteer.getBackgroundCheck(),
                "Volunteer background check status should be saved");
    }

    @Test
    @Rollback
    void testCreateStaffWithWrongRole_ThrowsException() {
        // Arrange - create user with role=1 (Volunteer)
        FoodBankBranch branch = branchRepository.findById(1).orElseThrow();

        User user = new User();
        user.setFirstName("Wrong");
        user.setLastName("Role");
        user.setEmail("wrongrole@test.com");
        user.setPhone("617-555-7777");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole((byte) 1);  // Volunteer role (WRONG for staff)
        user.setStatus((byte) 1);
        user.setBranch(branch);

        user = userRepository.save(user);
        entityManager.flush();

        // Act & Assert - attempt to create staff record for volunteer user
        Staff staff = new Staff();
        staff.setUser(user);
        staff.setJobTitle("Test Manager");
        staff.setHireDate(LocalDateTime.now());

        User finalUser = user;
        assertThrows(Exception.class, () -> {
            staffRepository.save(staff);
            entityManager.flush();
        }, "Should throw exception when creating staff record for user with role=1");
    }

    @Test
    @Rollback
    void testCreateVolunteerWithWrongRole_ThrowsException() {
        // Arrange - create user with role=0 (Staff)
        FoodBankBranch branch = branchRepository.findById(1).orElseThrow();

        User user = new User();
        user.setFirstName("Wrong");
        user.setLastName("Role2");
        user.setEmail("wrongrole2@test.com");
        user.setPhone("617-555-6666");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole((byte) 0);  // Staff role (WRONG for volunteer)
        user.setStatus((byte) 1);
        user.setBranch(branch);

        user = userRepository.save(user);
        entityManager.flush();

        // Act & Assert - attempt to create volunteer record for staff user
        Volunteer volunteer = new Volunteer();
        volunteer.setUser(user);
        volunteer.setAvailability("Weekdays");
        volunteer.setBackgroundCheck((byte) 0);

        User finalUser = user;
        assertThrows(Exception.class, () -> {
            volunteerRepository.save(volunteer);
            entityManager.flush();
        }, "Should throw exception when creating volunteer record for user with role=0");
    }
}
