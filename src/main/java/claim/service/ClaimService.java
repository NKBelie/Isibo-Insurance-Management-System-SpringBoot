package claim.service;

import claim.domain.Claim;

import java.util.List;
import java.util.UUID;

public interface ClaimService {
    Claim create(Claim claim);
    Claim findById(UUID id);
    List<Claim> findAll();
    List<Claim> findByMemberNationalId(String nationalId);
    Claim update(UUID id, Claim claim);
    Claim review(UUID id, String status, UUID officerId);
    void delete(UUID id);
}
