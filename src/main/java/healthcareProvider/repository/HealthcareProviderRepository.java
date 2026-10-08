package healthcareProvider.repository;

import healthcareProvider.domain.HealthcareProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HealthcareProviderRepository extends JpaRepository<HealthcareProvider, UUID> {
    boolean existsByProviderId(String providerId);
}
