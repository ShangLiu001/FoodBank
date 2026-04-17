package com.primaryfeed.controller;

import com.primaryfeed.auth.JwtUtil;
import com.primaryfeed.dto.BranchLookup;
import com.primaryfeed.entity.User;
import com.primaryfeed.repository.FoodBankBranchRepository;
import com.primaryfeed.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
@Tag(name = "Branches", description = "Food bank branch locations. Scoped to the current user's food bank.")
@SecurityRequirement(name = "bearerAuth")
public class BranchController {

    private final FoodBankBranchRepository branchRepository;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Operation(
            summary = "Get branches for current user's food bank",
            description = "Returns branchId and branchName for all branches belonging to the same food bank as the authenticated user. Designed for populating selector dropdowns."
    )
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestHeader(value = "Authorization", required = false)
            @Parameter(hidden = true) String header
    ) {
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }

        String email = jwtUtil.extractEmail(header.substring(7));
        User user = userService.findByEmail(email).orElse(null);
        if (user == null || user.getBranch() == null || user.getBranch().getFoodBank() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot determine food bank for current user"));
        }

        Integer foodBankId = user.getBranch().getFoodBank().getFoodBankId();
        List<BranchLookup> branches = branchRepository.findByFoodBank_FoodBankId(foodBankId).stream()
                .map(b -> new BranchLookup(b.getBranchId(), b.getBranchName()))
                .toList();

        return ResponseEntity.ok(branches);
    }
}