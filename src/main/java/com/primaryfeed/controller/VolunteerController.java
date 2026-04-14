package com.primaryfeed.controller;

import com.primaryfeed.entity.Volunteer;
import com.primaryfeed.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    @GetMapping
    public List<Volunteer> getAll() {
        return volunteerService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Volunteer> getById(@PathVariable Integer id) {
        return volunteerService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/branch/{branchId}")
    public List<Volunteer> getByBranch(@PathVariable Integer branchId) {
        return volunteerService.findByBranchId(branchId);
    }

    @PostMapping
    public Volunteer create(@RequestBody Volunteer volunteer) {
        return volunteerService.save(volunteer);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Volunteer> update(@PathVariable Integer id, @RequestBody Volunteer volunteer) {
        if (volunteerService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        volunteer.setVolunteerId(id);
        return ResponseEntity.ok(volunteerService.save(volunteer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (volunteerService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        volunteerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
