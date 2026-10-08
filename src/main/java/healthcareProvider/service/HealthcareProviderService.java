package healthcareProvider.service;

import healthcareProvider.domain.HealthcareProvider;

import java.util.List;
import java.util.UUID;

public interface HealthcareProviderService {
    HealthcareProvider create(HealthcareProvider provider);
    HealthcareProvider findById(UUID id);
    List<HealthcareProvider> findAll();
    HealthcareProvider update(UUID id, HealthcareProvider provider);
    void delete(UUID id);
}
