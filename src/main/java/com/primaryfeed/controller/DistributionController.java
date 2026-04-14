package com.primaryfeed.controller;

import com.primaryfeed.entity.Distribution;
import com.primaryfeed.service.DistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/distributions")
@RequiredArgsConstructor
public class DistributionController {

    private final DistributionService distributionService;

    @GetMapping
    public List<Distribution> getAll() {
        return distributionService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Distribution> getById(@PathVariable Integer id) {
        return distributionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/branch/{branchId}")
    public List<Distribution> getByBranch(@PathVariable Integer branchId) {
        return distributionService.findByBranchId(branchId);
    }

    @GetMapping("/beneficiary/{beneficiaryId}")
    public List<Distribution> getByBeneficiary(@PathVariable Integer beneficiaryId) {
        return distributionService.findByBeneficiaryId(beneficiaryId);
    }

    @PostMapping
    public Distribution create(@RequestBody Distribution distribution) {
        return distributionService.save(distribution);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Distribution> update(@PathVariable Integer id, @RequestBody Distribution distribution) {
        if (distributionService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        distribution.setDistributionId(id);
        return ResponseEntity.ok(distributionService.save(distribution));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (distributionService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        distributionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
