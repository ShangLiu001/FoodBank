package com.primaryfeed.controller;

import com.primaryfeed.entity.Donor;
import com.primaryfeed.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donors")
@RequiredArgsConstructor
public class DonorController {

    private final DonationService donationService;

    @GetMapping
    public List<Donor> getAll() {
        return donationService.findAllDonors();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Donor> getById(@PathVariable Integer id) {
        return donationService.findDonorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Donor create(@RequestBody Donor donor) {
        return donationService.saveDonor(donor);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Donor> update(@PathVariable Integer id, @RequestBody Donor donor) {
        if (donationService.findDonorById(id).isEmpty()) return ResponseEntity.notFound().build();
        donor.setDonorId(id);
        return ResponseEntity.ok(donationService.saveDonor(donor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (donationService.findDonorById(id).isEmpty()) return ResponseEntity.notFound().build();
        donationService.deleteDonor(id);
        return ResponseEntity.noContent().build();
    }
}
