package com.primaryfeed.controller;

import com.primaryfeed.entity.DonationItem;
import com.primaryfeed.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donation-items")
@RequiredArgsConstructor
public class DonationItemController {

    private final DonationService donationService;

    @GetMapping("/donation/{donationId}")
    public List<DonationItem> getByDonation(@PathVariable Integer donationId) {
        return donationService.findItemsByDonationId(donationId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DonationItem> getById(@PathVariable Integer id) {
        return donationService.findItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public DonationItem create(@RequestBody DonationItem item) {
        return donationService.saveItem(item);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DonationItem> update(@PathVariable Integer id, @RequestBody DonationItem item) {
        if (donationService.findItemById(id).isEmpty()) return ResponseEntity.notFound().build();
        item.setDonationItemId(id);
        return ResponseEntity.ok(donationService.saveItem(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (donationService.findItemById(id).isEmpty()) return ResponseEntity.notFound().build();
        donationService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
