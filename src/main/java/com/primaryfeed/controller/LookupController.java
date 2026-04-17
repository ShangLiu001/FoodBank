package com.primaryfeed.controller;

import com.primaryfeed.dto.LookupOption;
import com.primaryfeed.service.DonationService;
import com.primaryfeed.service.DistributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lookups")
@RequiredArgsConstructor
@Tag(name = "Lookups", description = "Lightweight ID + name lists for populating selector dropdowns. Returns minimal payload to keep UI responsive.")
@SecurityRequirement(name = "bearerAuth")
public class LookupController {

    private final DonationService donationService;
    private final DistributionService distributionService;

    @Operation(
        summary = "Donor lookup list",
        description = "Returns donorId → donorName pairs. Used by Reports Q11 and any other donor selector."
    )
    @GetMapping("/donors")
    public List<LookupOption> donors() {
        return donationService.findAllDonors().stream()
                .map(d -> new LookupOption(d.getDonorId(), d.getDonorName()))
                .toList();
    }

    @Operation(
        summary = "Beneficiary lookup list",
        description = "Returns beneficiaryId → beneficiaryFullName pairs. Used by Reports Q9 and any other beneficiary selector."
    )
    @GetMapping("/beneficiaries")
    public List<LookupOption> beneficiaries() {
        return distributionService.findAllBeneficiaries().stream()
                .map(b -> new LookupOption(b.getBeneficiaryId(), b.getBeneficiaryFullName()))
                .toList();
    }
}
