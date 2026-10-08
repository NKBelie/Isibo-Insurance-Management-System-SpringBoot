package claim.service;

import claim.domain.Claim;
import claim.repository.ClaimRepository;
import exception.BusinessRuleException;
import exception.DuplicateResourceException;
import exception.ResourceNotFoundException;
import exception.ValidationException;
import healthcareProvider.domain.HealthcareProvider;
import healthcareProvider.repository.HealthcareProviderRepository;
import insuranceOfficer.domain.InsuranceOfficer;
import insuranceOfficer.repository.InsuranceOfficerRepository;
import lombok.RequiredArgsConstructor;
import member.domain.Member;
import member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private final ClaimRepository claimRepository;
    private final MemberRepository memberRepository;
    private final HealthcareProviderRepository providerRepository;
    private final InsuranceOfficerRepository officerRepository;

    @Override
    @Transactional
    public Claim create(Claim claim) {
        validateClaim(claim, true);
        claim.setClaimId(claim.getClaimId().trim());
        if (claimRepository.existsByClaimId(claim.getClaimId())) {
            throw new DuplicateResourceException("Claim ID already exists.");
        }

        Member member = findMember(claim.getMember());
        HealthcareProvider provider = findProvider(claim.getHealthcareProvider());
        validateAmountAgainstPlan(claim.getAmountClaimed(), member);

        claim.setClaimId(claim.getClaimId().trim());
        claim.setDescription(claim.getDescription().trim());
        claim.setStatus(PENDING);
        claim.setDecisionDate(null);
        claim.setInsuranceOfficer(null);
        claim.setMember(member);
        claim.setHealthcareProvider(provider);
        return claimRepository.save(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public Claim findById(UUID id) {
        if (id == null) {
            throw new ValidationException("Claim ID is required.");
        }
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Claim> findAll() {
        return claimRepository.findAll();
    }

    @Override
    @Transactional
    public Claim update(UUID id, Claim claim) {
        if (claim == null) {
            throw new ValidationException("Claim data is required.");
        }
        Claim existing = findById(id);
        ensurePending(existing);
        if (claim.getClaimId() != null && !claim.getClaimId().isBlank()
                && !claim.getClaimId().trim().equals(existing.getClaimId())) {
            throw new BusinessRuleException("Claim ID cannot be changed.");
        }
        if (claim.getClaimDate() != null) {
            validateClaimDate(claim.getClaimDate());
            existing.setClaimDate(claim.getClaimDate());
        }
        if (claim.getDescription() != null && !claim.getDescription().isBlank()) {
            existing.setDescription(claim.getDescription().trim());
        }

        Member member = claim.getMember() == null ? existing.getMember() : findMember(claim.getMember());
        HealthcareProvider provider = claim.getHealthcareProvider() == null
                ? existing.getHealthcareProvider()
                : findProvider(claim.getHealthcareProvider());
        if (claim.getAmountClaimed() < 0) {
            throw new ValidationException("Claim amount must be greater than zero.");
        }
        if (claim.getAmountClaimed() > 0) {
            existing.setAmountClaimed(claim.getAmountClaimed());
        }
        validateAmountAgainstPlan(existing.getAmountClaimed(), member);
        existing.setMember(member);
        existing.setHealthcareProvider(provider);
        return claimRepository.save(existing);
    }

    @Override
    @Transactional
    public Claim review(UUID id, String status, UUID officerId) {
        Claim existing = findById(id);
        ensurePending(existing);
        if (status == null || status.isBlank()) {
            throw new ValidationException("Claim decision is required.");
        }
        String decision = status.trim().toUpperCase();
        if (!APPROVED.equals(decision) && !REJECTED.equals(decision)) {
            throw new ValidationException("Claim decision must be APPROVED or REJECTED.");
        }
        if (officerId == null) {
            throw new ValidationException("Insurance Officer is required to review a claim.");
        }
        InsuranceOfficer officer = officerRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Officer not found."));

        existing.setStatus(decision);
        existing.setDecisionDate(LocalDate.now());
        existing.setInsuranceOfficer(officer);
        return claimRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Claim existing = findById(id);
        ensurePending(existing);
        claimRepository.delete(existing);
    }

    private void validateClaim(Claim claim, boolean requireClaimId) {
        if (claim == null) {
            throw new ValidationException("Claim data is required.");
        }
        if (requireClaimId && (claim.getClaimId() == null || claim.getClaimId().isBlank())) {
            throw new ValidationException("Claim ID is required.");
        }
        if (claim.getClaimDate() == null) {
            throw new ValidationException("Claim date is required.");
        }
        validateClaimDate(claim.getClaimDate());
        if (claim.getDescription() == null || claim.getDescription().isBlank()) {
            throw new ValidationException("Claim description is required.");
        }
        if (claim.getAmountClaimed() <= 0) {
            throw new ValidationException("Claim amount must be greater than zero.");
        }
        if (claim.getMember() == null || claim.getMember().getNationalId() == null
                || claim.getMember().getNationalId().isBlank()) {
            throw new ValidationException("Member is required.");
        }
        if (claim.getHealthcareProvider() == null || claim.getHealthcareProvider().getId() == null) {
            throw new ValidationException("Healthcare Provider is required.");
        }
    }

    private void validateClaimDate(LocalDate claimDate) {
        if (claimDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Claim date cannot be in the future.");
        }
    }

    private Member findMember(Member reference) {
        if (reference == null || reference.getNationalId() == null || reference.getNationalId().isBlank()) {
            throw new ValidationException("Member is required.");
        }
        return memberRepository.findById(reference.getNationalId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found."));
    }

    private HealthcareProvider findProvider(HealthcareProvider reference) {
        if (reference == null || reference.getId() == null) {
            throw new ValidationException("Healthcare Provider is required.");
        }
        return providerRepository.findById(reference.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Healthcare Provider not found."));
    }

    private void validateAmountAgainstPlan(double amount, Member member) {
        if (member.getInsurancePlan() == null) {
            throw new BusinessRuleException("Member does not have an Insurance Plan.");
        }
        if (amount > member.getInsurancePlan().getCoverageLimit()) {
            throw new BusinessRuleException("Claim amount exceeds the member's Insurance Plan coverage limit.");
        }
    }

    private void ensurePending(Claim claim) {
        if (!PENDING.equalsIgnoreCase(claim.getStatus())) {
            throw new BusinessRuleException("Only pending claims can be changed or reviewed.");
        }
    }
}
