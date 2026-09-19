package insurancePlan.repository;

import insurancePlan.domain.InsurancePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InsurancePlanRepository
        extends JpaRepository<InsurancePlan, UUID> {

    Optional<InsurancePlan> findByInsurancePlanId(String insurancePlanId);

    boolean existsByInsurancePlanId(String insurancePlanId);

    boolean existsByInsurancePlanNameIgnoreCase(String insurancePlanName);
}