package insuranceOfficer.repository;

import insuranceOfficer.domain.InsuranceOfficer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InsuranceOfficerRepository extends JpaRepository<InsuranceOfficer, UUID> {
    boolean existsByOfficerId(String officerId);
}
