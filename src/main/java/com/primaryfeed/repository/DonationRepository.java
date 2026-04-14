package com.primaryfeed.repository;
import com.primaryfeed.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Integer> {
    List<Donation> findByBranch_BranchId(Integer branchId);
    List<Donation> findByDonor_DonorId(Integer donorId);
    List<Donation> findByUser_UserId(Integer userId);
}