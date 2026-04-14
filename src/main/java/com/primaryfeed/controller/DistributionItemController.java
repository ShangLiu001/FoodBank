package com.primaryfeed.controller;

import com.primaryfeed.entity.DistributionItem;
import com.primaryfeed.service.DistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/distribution-items")
@RequiredArgsConstructor
public class DistributionItemController {

    private final DistributionService distributionService;

    @GetMapping("/distribution/{distributionId}")
    public List<DistributionItem> getByDistribution(@PathVariable Integer distributionId) {
        return distributionService.findItemsByDistributionId(distributionId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DistributionItem> getById(@PathVariable Integer id) {
        return distributionService.findItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public DistributionItem create(@RequestBody DistributionItem item) {
        return distributionService.saveItem(item);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DistributionItem> update(@PathVariable Integer id, @RequestBody DistributionItem item) {
        if (distributionService.findItemById(id).isEmpty()) return ResponseEntity.notFound().build();
        item.setDistributionItemId(id);
        return ResponseEntity.ok(distributionService.saveItem(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (distributionService.findItemById(id).isEmpty()) return ResponseEntity.notFound().build();
        distributionService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
