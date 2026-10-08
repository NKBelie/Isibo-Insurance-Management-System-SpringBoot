package claim.repository;

import claim.domain.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    boolean existsByClaimId(String claimId);
    boolean existsByHealthcareProvider_Id(UUID providerId);
    boolean existsByInsuranceOfficer_Id(UUID officerId);
    boolean existsByMember_NationalId(String nationalId);
    List<Claim> findByMember_NationalId(String nationalId);
}
