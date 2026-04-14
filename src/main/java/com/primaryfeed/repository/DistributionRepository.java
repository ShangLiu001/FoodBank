package com.primaryfeed.repository;
import com.primaryfeed.entity.Distribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DistributionRepository extends JpaRepository<Distribution, Integer> {
    List<Distribution> findByBranch_BranchId(Integer branchId);
    List<Distribution> findByBeneficiary_BeneficiaryId(Integer beneficiaryId);
    List<Distribution> findByUser_UserId(Integer userId);
}