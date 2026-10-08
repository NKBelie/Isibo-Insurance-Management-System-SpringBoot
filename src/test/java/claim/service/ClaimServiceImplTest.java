package claim.service;

import claim.domain.Claim;
import claim.repository.ClaimRepository;
import exception.BusinessRuleException;
import healthcareProvider.domain.HealthcareProvider;
import healthcareProvider.repository.HealthcareProviderRepository;
import insuranceOfficer.domain.InsuranceOfficer;
import insuranceOfficer.repository.InsuranceOfficerRepository;
import insurancePlan.domain.InsurancePlan;
import member.domain.Member;
import member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private HealthcareProviderRepository providerRepository;
    @Mock
    private InsuranceOfficerRepository officerRepository;

    @InjectMocks
    private ClaimServiceImpl claimService;

    @Test
    void createShouldStartClaimAsPendingWithoutDecisionInformation() {
        Claim claim = validClaim();
        when(claimRepository.existsByClaimId("CLM-1")).thenReturn(false);
        when(memberRepository.findById("NAT-1")).thenReturn(Optional.of(claim.getMember()));
        when(providerRepository.findById(claim.getHealthcareProvider().getId()))
                .thenReturn(Optional.of(claim.getHealthcareProvider()));
        when(claimRepository.save(claim)).thenReturn(claim);

        Claim saved = claimService.create(claim);

        assertEquals("PENDING", saved.getStatus());
        assertNull(saved.getDecisionDate());
        assertNull(saved.getInsuranceOfficer());
    }

    @Test
    void createShouldRejectClaimAbovePlanCoverageLimit() {
        Claim claim = validClaim();
        claim.setAmountClaimed(1001);
        when(claimRepository.existsByClaimId("CLM-1")).thenReturn(false);
        when(memberRepository.findById("NAT-1")).thenReturn(Optional.of(claim.getMember()));
        when(providerRepository.findById(claim.getHealthcareProvider().getId()))
                .thenReturn(Optional.of(claim.getHealthcareProvider()));

        assertThrows(BusinessRuleException.class, () -> claimService.create(claim));
    }

    @Test
    void reviewShouldSetDecisionAndDecisionDate() {
        UUID claimId = UUID.randomUUID();
        UUID officerId = UUID.randomUUID();
        Claim claim = validClaim();
        claim.setId(claimId);
        claim.setStatus("PENDING");
        InsuranceOfficer officer = new InsuranceOfficer();
        officer.setId(officerId);
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(officerRepository.findById(officerId)).thenReturn(Optional.of(officer));
        when(claimRepository.save(claim)).thenReturn(claim);

        Claim reviewed = claimService.review(claimId, "APPROVED", officerId);

        assertEquals("APPROVED", reviewed.getStatus());
        assertEquals(LocalDate.now(), reviewed.getDecisionDate());
        assertEquals(officer, reviewed.getInsuranceOfficer());
    }

    private Claim validClaim() {
        InsurancePlan plan = new InsurancePlan();
        plan.setCoverageLimit(1000);

        Member member = new Member();
        member.setNationalId("NAT-1");
        member.setInsurancePlan(plan);

        HealthcareProvider provider = new HealthcareProvider();
        provider.setId(UUID.randomUUID());

        Claim claim = new Claim();
        claim.setClaimId("CLM-1");
        claim.setClaimDate(LocalDate.now());
        claim.setDescription("Consultation");
        claim.setAmountClaimed(500);
        claim.setMember(member);
        claim.setHealthcareProvider(provider);
        return claim;
    }
}
