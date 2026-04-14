package com.primaryfeed.controller;

import com.primaryfeed.entity.Donation;
import com.primaryfeed.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    @GetMapping
    public List<Donation> getAll() {
        return donationService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Donation> getById(@PathVariable Integer id) {
        return donationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/branch/{branchId}")
    public List<Donation> getByBranch(@PathVariable Integer branchId) {
        return donationService.findByBranchId(branchId);
    }

    @GetMapping("/donor/{donorId}")
    public List<Donation> getByDonor(@PathVariable Integer donorId) {
        return donationService.findByDonorId(donorId);
    }

    @PostMapping
    public Donation create(@RequestBody Donation donation) {
        return donationService.save(donation);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Donation> update(@PathVariable Integer id, @RequestBody Donation donation) {
        if (donationService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        donation.setDonationId(id);
        return ResponseEntity.ok(donationService.save(donation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (donationService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        donationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
