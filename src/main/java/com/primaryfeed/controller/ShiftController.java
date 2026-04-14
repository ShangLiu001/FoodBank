package com.primaryfeed.controller;

import com.primaryfeed.entity.VolunteerShift;
import com.primaryfeed.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final VolunteerService volunteerService;

    @GetMapping
    public List<VolunteerShift> getAll() {
        return volunteerService.findAllShifts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<VolunteerShift> getById(@PathVariable Integer id) {
        return volunteerService.findShiftById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/volunteer/{volunteerId}")
    public List<VolunteerShift> getByVolunteer(@PathVariable Integer volunteerId) {
        return volunteerService.findShiftsByVolunteerId(volunteerId);
    }

    @GetMapping("/branch/{branchId}")
    public List<VolunteerShift> getByBranch(@PathVariable Integer branchId) {
        return volunteerService.findShiftsByBranchId(branchId);
    }

    @GetMapping("/date/{date}")
    public List<VolunteerShift> getByDate(@PathVariable LocalDate date) {
        return volunteerService.findShiftsByDate(date);
    }

    @PostMapping
    public VolunteerShift create(@RequestBody VolunteerShift shift) {
        return volunteerService.saveShift(shift);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VolunteerShift> update(@PathVariable Integer id, @RequestBody VolunteerShift shift) {
        if (volunteerService.findShiftById(id).isEmpty()) return ResponseEntity.notFound().build();
        shift.setShiftId(id);
        return ResponseEntity.ok(volunteerService.saveShift(shift));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (volunteerService.findShiftById(id).isEmpty()) return ResponseEntity.notFound().build();
        volunteerService.deleteShift(id);
        return ResponseEntity.noContent().build();
    }
}
