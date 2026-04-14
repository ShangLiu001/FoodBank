package com.primaryfeed.service;

import com.primaryfeed.entity.Staff;
import com.primaryfeed.entity.User;
import com.primaryfeed.entity.Volunteer;
import com.primaryfeed.repository.StaffRepository;
import com.primaryfeed.repository.UserRepository;
import com.primaryfeed.repository.VolunteerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final VolunteerRepository volunteerRepository;

    // Users
    public List<User> findAll() { return userRepository.findAll(); }
    public Optional<User> findById(Integer id) { return userRepository.findById(id); }
    public Optional<User> findByEmail(String email) { return userRepository.findByEmail(email); }
    public List<User> findByRole(Byte role) { return userRepository.findByRole(role); }
    public List<User> findByBranchId(Integer branchId) { return userRepository.findByBranch_BranchId(branchId); }
    public boolean existsByEmail(String email) { return userRepository.existsByEmail(email); }
    public User save(User user) { return userRepository.save(user); }
    public void delete(Integer id) { userRepository.deleteById(id); }

    // Staff subtype
    public List<Staff> findAllStaff() { return staffRepository.findAll(); }
    public Optional<Staff> findStaffById(Integer staffId) { return staffRepository.findById(staffId); }
    public Optional<Staff> findStaffByUserId(Integer userId) { return staffRepository.findByUser_UserId(userId); }
    public Staff saveStaff(Staff staff) { return staffRepository.save(staff); }
    public void deleteStaff(Integer staffId) { staffRepository.deleteById(staffId); }

    // Volunteer subtype (profile record — shifts live in VolunteerService)
    public List<Volunteer> findAllVolunteers() { return volunteerRepository.findAll(); }
    public Optional<Volunteer> findVolunteerById(Integer volunteerId) { return volunteerRepository.findById(volunteerId); }
    public Optional<Volunteer> findVolunteerByUserId(Integer userId) { return volunteerRepository.findByUser_UserId(userId); }
    public Volunteer saveVolunteer(Volunteer volunteer) { return volunteerRepository.save(volunteer); }
    public void deleteVolunteer(Integer volunteerId) { volunteerRepository.deleteById(volunteerId); }
}
