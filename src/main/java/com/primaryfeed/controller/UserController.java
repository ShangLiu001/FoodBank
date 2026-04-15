package com.primaryfeed.controller;

import com.primaryfeed.entity.Address;
import com.primaryfeed.entity.FoodBankBranch;
import com.primaryfeed.entity.Staff;
import com.primaryfeed.entity.User;
import com.primaryfeed.entity.Volunteer;
import com.primaryfeed.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Staff-only. SecurityConfig already gates /api/users/** to hasRole("STAFF").
 *
 * POST /api/users creates both the users row and the matching staff or volunteers subtype row.
 * PUT  /api/users/{id} updates user fields only; role cannot be changed after creation.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public List<User> getAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Integer id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/role/{role}")
    public List<User> getByRole(@PathVariable Byte role) {
        return userService.findByRole(role);
    }

    @GetMapping("/branch/{branchId}")
    public List<User> getByBranch(@PathVariable Integer branchId) {
        return userService.findByBranchId(branchId);
    }

    /**
     * Creates a user row + the matching subtype row (staff or volunteers).
     *
     * Request body (flat JSON):
     * {
     *   "firstName": "...",   "lastName": "...",
     *   "email": "...",       "phone": "...",
     *   "password": "plaintext",
     *   "role": 0,            "status": 1,
     *   "branchId": 1,
     *   "addressId": 1,             (optional)
     *   "drivingLicenseNum": "...", (optional)
     *   -- staff only (role = 0) --
     *   "jobTitle": "...",
     *   "hireDate": "2024-01-15T00:00:00",
     *   -- volunteer only (role = 1) --
     *   "availability": "...",
     *   "backgroundCheck": 0
     * }
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        if (userService.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }

        User user = buildUser(body, null);
        String password = (String) body.get("password");
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "password is required"));
        }
        user.setPasswordHash(passwordEncoder.encode(password));

        User saved = userService.save(user);

        if (saved.getRole() != null && saved.getRole() == 0) {
            Staff staff = new Staff();
            staff.setUser(saved);
            staff.setJobTitle((String) body.get("jobTitle"));
            String hireDateStr = (String) body.get("hireDate");
            staff.setHireDate(hireDateStr != null ? LocalDateTime.parse(hireDateStr) : LocalDateTime.now());
            userService.saveStaff(staff);
        } else {
            Volunteer volunteer = new Volunteer();
            volunteer.setUser(saved);
            volunteer.setAvailability((String) body.get("availability"));
            Number bgCheck = (Number) body.get("backgroundCheck");
            volunteer.setBackgroundCheck(bgCheck != null ? bgCheck.byteValue() : (byte) 0);
            userService.saveVolunteer(volunteer);
        }

        // Re-fetch so the response contains the fully loaded entity
        return ResponseEntity.status(201).body(
                userService.findById(saved.getUserId()).orElse(saved));
    }

    /**
     * Updates basic user fields. Role cannot be changed after creation.
     * Password is only re-hashed if a non-blank "password" key is supplied.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id,
                                    @RequestBody Map<String, Object> body) {
        return userService.findById(id).map(existing -> {
            buildUser(body, existing);

            String password = (String) body.get("password");
            if (password != null && !password.isBlank()) {
                existing.setPasswordHash(passwordEncoder.encode(password));
            }

            return ResponseEntity.ok(userService.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (userService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    /**
     * Applies fields from the request body map onto a User object.
     * Pass null for {@code target} to create a fresh User.
     * Role is only applied when creating (target == null); it cannot be changed on update.
     */
    private User buildUser(Map<String, Object> body, User target) {
        User user = target != null ? target : new User();

        if (body.containsKey("firstName"))  user.setFirstName((String) body.get("firstName"));
        if (body.containsKey("lastName"))   user.setLastName((String) body.get("lastName"));
        if (body.containsKey("email"))      user.setEmail((String) body.get("email"));
        if (body.containsKey("phone"))      user.setPhone((String) body.get("phone"));
        if (body.containsKey("drivingLicenseNum")) {
            user.setDrivingLicenseNum((String) body.get("drivingLicenseNum"));
        }

        if (body.containsKey("status")) {
            user.setStatus(((Number) body.get("status")).byteValue());
        }

        // Role is set only on create
        if (target == null && body.containsKey("role")) {
            user.setRole(((Number) body.get("role")).byteValue());
        }

        if (body.containsKey("branchId")) {
            FoodBankBranch branch = new FoodBankBranch();
            branch.setBranchId(((Number) body.get("branchId")).intValue());
            user.setBranch(branch);
        }

        if (body.containsKey("addressId")) {
            Number addressIdNum = (Number) body.get("addressId");
            if (addressIdNum != null) {
                Address address = new Address();
                address.setAddressId(addressIdNum.intValue());
                user.setAddress(address);
            } else {
                user.setAddress(null);
            }
        }

        return user;
    }
}
