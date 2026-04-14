package com.primaryfeed.service;

import com.primaryfeed.entity.Donation;
import com.primaryfeed.entity.DonationItem;
import com.primaryfeed.entity.Donor;
import com.primaryfeed.repository.DonationItemRepository;
import com.primaryfeed.repository.DonationRepository;
import com.primaryfeed.repository.DonorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationRepository donationRepository;
    private final DonationItemRepository donationItemRepository;
    private final DonorRepository donorRepository;

    // Donations
    public List<Donation> findAll() { return donationRepository.findAll(); }
    public Optional<Donation> findById(Integer id) { return donationRepository.findById(id); }
    public List<Donation> findByBranchId(Integer branchId) { return donationRepository.findByBranch_BranchId(branchId); }
    public List<Donation> findByDonorId(Integer donorId) { return donationRepository.findByDonor_DonorId(donorId); }
    public List<Donation> findByUserId(Integer userId) { return donationRepository.findByUser_UserId(userId); }
    public Donation save(Donation donation) { return donationRepository.save(donation); }
    public void delete(Integer id) { donationRepository.deleteById(id); }

    // Donation items
    public List<DonationItem> findItemsByDonationId(Integer donationId) { return donationItemRepository.findByDonation_DonationId(donationId); }
    public Optional<DonationItem> findItemById(Integer id) { return donationItemRepository.findById(id); }
    public DonationItem saveItem(DonationItem item) { return donationItemRepository.save(item); }
    public void deleteItem(Integer id) { donationItemRepository.deleteById(id); }

    // Donors
    public List<Donor> findAllDonors() { return donorRepository.findAll(); }
    public Optional<Donor> findDonorById(Integer id) { return donorRepository.findById(id); }
    public Optional<Donor> findDonorByEmail(String email) { return donorRepository.findByEmail(email); }
    public Donor saveDonor(Donor donor) { return donorRepository.save(donor); }
    public void deleteDonor(Integer id) { donorRepository.deleteById(id); }
}
