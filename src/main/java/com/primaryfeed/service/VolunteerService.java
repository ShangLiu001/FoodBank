package com.primaryfeed.service;

import com.primaryfeed.entity.User;
import com.primaryfeed.entity.Volunteer;
import com.primaryfeed.entity.VolunteerShift;
import com.primaryfeed.repository.UserRepository;
import com.primaryfeed.repository.VolunteerRepository;
import com.primaryfeed.repository.VolunteerShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolunteerService {

    private final VolunteerRepository volunteerRepository;
    private final VolunteerShiftRepository volunteerShiftRepository;
    private final UserRepository userRepository;

    // Volunteers
    public List<Volunteer> findAll() { return volunteerRepository.findAll(); }
    public Optional<Volunteer> findById(Integer id) { return volunteerRepository.findById(id); }
    public Optional<Volunteer> findByUserId(Integer userId) { return volunteerRepository.findByUser_UserId(userId); }

    public List<Volunteer> findByBranchId(Integer branchId) {
        List<User> branchUsers = userRepository.findByBranch_BranchId(branchId);
        return branchUsers.stream()
                .filter(u -> u.getRole() != null && u.getRole() == 1)
                .map(u -> volunteerRepository.findByUser_UserId(u.getUserId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Volunteer save(Volunteer volunteer) { return volunteerRepository.save(volunteer); }
    public void delete(Integer id) { volunteerRepository.deleteById(id); }

    // Shifts
    public List<VolunteerShift> findAllShifts() { return volunteerShiftRepository.findAll(); }
    public Optional<VolunteerShift> findShiftById(Integer id) { return volunteerShiftRepository.findById(id); }
    public List<VolunteerShift> findShiftsByVolunteerId(Integer volunteerId) { return volunteerShiftRepository.findByVolunteer_VolunteerId(volunteerId); }
    public List<VolunteerShift> findShiftsByBranchId(Integer branchId) { return volunteerShiftRepository.findByBranch_BranchId(branchId); }
    public List<VolunteerShift> findShiftsByDate(LocalDate date) { return volunteerShiftRepository.findByShiftDate(date); }
    public VolunteerShift saveShift(VolunteerShift shift) { return volunteerShiftRepository.save(shift); }
    public void deleteShift(Integer id) { volunteerShiftRepository.deleteById(id); }
}
