package com.primaryfeed.service;

import com.primaryfeed.entity.Beneficiary;
import com.primaryfeed.entity.Distribution;
import com.primaryfeed.entity.DistributionItem;
import com.primaryfeed.repository.BeneficiaryRepository;
import com.primaryfeed.repository.DistributionItemRepository;
import com.primaryfeed.repository.DistributionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DistributionService {

    private final DistributionRepository distributionRepository;
    private final DistributionItemRepository distributionItemRepository;
    private final BeneficiaryRepository beneficiaryRepository;

    // Distributions
    public List<Distribution> findAll() { return distributionRepository.findAll(); }
    public Optional<Distribution> findById(Integer id) { return distributionRepository.findById(id); }
    public List<Distribution> findByBranchId(Integer branchId) { return distributionRepository.findByBranch_BranchId(branchId); }
    public List<Distribution> findByBeneficiaryId(Integer beneficiaryId) { return distributionRepository.findByBeneficiary_BeneficiaryId(beneficiaryId); }
    public List<Distribution> findByUserId(Integer userId) { return distributionRepository.findByUser_UserId(userId); }
    public Distribution save(Distribution distribution) { return distributionRepository.save(distribution); }
    public void delete(Integer id) { distributionRepository.deleteById(id); }

    // Distribution items
    public List<DistributionItem> findItemsByDistributionId(Integer distributionId) { return distributionItemRepository.findByDistribution_DistributionId(distributionId); }
    public Optional<DistributionItem> findItemById(Integer id) { return distributionItemRepository.findById(id); }
    public DistributionItem saveItem(DistributionItem item) { return distributionItemRepository.save(item); }
    public void deleteItem(Integer id) { distributionItemRepository.deleteById(id); }

    // Beneficiaries
    public List<Beneficiary> findAllBeneficiaries() { return beneficiaryRepository.findAll(); }
    public Optional<Beneficiary> findBeneficiaryById(Integer id) { return beneficiaryRepository.findById(id); }
    public List<Beneficiary> findBeneficiariesByEligibility(Byte status) { return beneficiaryRepository.findByEligibilityStatus(status); }
    public Beneficiary saveBeneficiary(Beneficiary beneficiary) { return beneficiaryRepository.save(beneficiary); }
    public void deleteBeneficiary(Integer id) { beneficiaryRepository.deleteById(id); }
}
