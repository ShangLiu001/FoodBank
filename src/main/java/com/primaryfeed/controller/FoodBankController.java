package com.primaryfeed.controller;
import com.primaryfeed.entity.FoodBank;
import com.primaryfeed.repository.FoodBankRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/food-banks")
@RequiredArgsConstructor
@Tag(name = "Food Banks", description = "Manage food bank organizations. Each food bank can have multiple branches.")
@SecurityRequirement(name = "bearerAuth")
public class FoodBankController {
    private final FoodBankRepository repo;

    @Operation(summary = "Get all food banks", description = "Retrieve all food bank organizations")
    @GetMapping
    public List<FoodBank> getAll() {
        return repo.findAll();
    }

    @Operation(summary = "Get food bank by ID", description = "Retrieve a specific food bank organization")
    @GetMapping("/{id}")
    public FoodBank getById(
        @Parameter(description = "Food bank ID", example = "1") @PathVariable Integer id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Not found: " + id));
    }

    @Operation(summary = "Create food bank", description = "Register a new food bank organization")
    @PostMapping
    public FoodBank create(@RequestBody FoodBank fb) {
        return repo.save(fb);
    }
}