package com.primaryfeed.controller;

import com.primaryfeed.entity.Beneficiary;
import com.primaryfeed.service.DistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final DistributionService distributionService;

    @GetMapping
    public List<Beneficiary> getAll() {
        return distributionService.findAllBeneficiaries();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Beneficiary> getById(@PathVariable Integer id) {
        return distributionService.findBeneficiaryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/eligible")
    public List<Beneficiary> getEligible() {
        return distributionService.findBeneficiariesByEligibility((byte) 1);
    }

    @PostMapping
    public Beneficiary create(@RequestBody Beneficiary beneficiary) {
        return distributionService.saveBeneficiary(beneficiary);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Beneficiary> update(@PathVariable Integer id, @RequestBody Beneficiary beneficiary) {
        if (distributionService.findBeneficiaryById(id).isEmpty()) return ResponseEntity.notFound().build();
        beneficiary.setBeneficiaryId(id);
        return ResponseEntity.ok(distributionService.saveBeneficiary(beneficiary));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (distributionService.findBeneficiaryById(id).isEmpty()) return ResponseEntity.notFound().build();
        distributionService.deleteBeneficiary(id);
        return ResponseEntity.noContent().build();
    }
}
