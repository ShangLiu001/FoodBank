package com.primaryfeed.controller;

import com.primaryfeed.entity.Address;
import com.primaryfeed.entity.FoodBankBranch;
import com.primaryfeed.entity.Staff;
import com.primaryfeed.entity.User;
import com.primaryfeed.entity.Volunteer;
import com.primaryfeed.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Users (Staff Only)", description = "Manage system users (Staff and Volunteers). Creates both user account and role-specific subtype records. STAFF access only.")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "Get all users", description = "Retrieve all system users (both Staff and Volunteers). Shows user accounts with their role, status, and branch assignments.")
    @GetMapping
    public List<User> getAll() {
        return userService.findAll();
    }

    @Operation(summary = "Get user by ID", description = "Retrieve a specific user account by user_id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<User> getById(
        @Parameter(description = "User ID", example = "1") @PathVariable Integer id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get users by role",
        description = "Retrieve all users with a specific role. Role values: 0=Staff, 1=Volunteer"
    )
    @GetMapping("/role/{role}")
    public List<User> getByRole(
        @Parameter(description = "User role (0=Staff, 1=Volunteer)", example = "0") @PathVariable Byte role) {
        return userService.findByRole(role);
    }

    @Operation(
        summary = "Get users by branch",
        description = "Retrieve all users assigned to a specific branch. Returns both Staff and Volunteers at that location."
    )
    @GetMapping("/branch/{branchId}")
    public List<User> getByBranch(
        @Parameter(description = "Branch ID", example = "1") @PathVariable Integer branchId) {
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
    @Operation(
        summary = "Create user (Staff or Volunteer)",
        description = """
            Creates both a user account AND the role-specific subtype record in one call.

            **Role values:**
            - role: 0 = Staff (requires jobTitle, optional hireDate)
            - role: 1 = Volunteer (requires availability, optional backgroundCheck)

            **Status values:**
            - status: 0 = Inactive (cannot log in)
            - status: 1 = Active

            Password is automatically bcrypt hashed before storage.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(responseCode = "400", description = "Email already exists or validation error")
    })
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

    @Operation(
        summary = "Update user",
        description = """
            Updates basic user fields (name, email, phone, status, branch, address).

            **IMPORTANT:** Role cannot be changed after user creation.

            Password is only re-hashed if a non-blank "password" key is supplied in the request body.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @Parameter(description = "User ID", example = "1") @PathVariable Integer id,
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

    @Operation(
        summary = "Delete user",
        description = "Remove a user account. This will also delete the associated Staff or Volunteer subtype record due to CASCADE constraints."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "User ID", example = "1") @PathVariable Integer id) {
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
