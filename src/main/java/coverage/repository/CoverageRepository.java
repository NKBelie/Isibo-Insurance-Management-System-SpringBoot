package coverage.repository;

import coverage.domain.Coverage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoverageRepository
        extends JpaRepository<Coverage, UUID> {

    Optional<Coverage> findByCoverageId(String coverageId);

    boolean existsByCoverageId(String coverageId);

    boolean existsByDescriptionIgnoreCase(String description);

    List<Coverage> findByInsurancePlanId(UUID insurancePlanId);
}