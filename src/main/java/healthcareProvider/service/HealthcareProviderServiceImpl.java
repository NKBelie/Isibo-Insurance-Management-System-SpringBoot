package healthcareProvider.service;

import claim.repository.ClaimRepository;
import exception.BusinessRuleException;
import exception.DuplicateResourceException;
import exception.ResourceNotFoundException;
import exception.ValidationException;
import healthcareProvider.domain.HealthcareProvider;
import healthcareProvider.repository.HealthcareProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class HealthcareProviderServiceImpl implements HealthcareProviderService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9][0-9\\s()-]{6,19}$");

    private final HealthcareProviderRepository providerRepository;
    private final ClaimRepository claimRepository;

    @Override
    @Transactional
    public HealthcareProvider create(HealthcareProvider provider) {
        validateRequiredFields(provider, true);
        provider.setProviderId(provider.getProviderId().trim());
        if (providerRepository.existsByProviderId(provider.getProviderId())) {
            throw new DuplicateResourceException("Provider ID already exists.");
        }
        normalizeFields(provider);
        return providerRepository.save(provider);
    }

    @Override
    @Transactional(readOnly = true)
    public HealthcareProvider findById(UUID id) {
        if (id == null) {
            throw new ValidationException("Provider ID is required.");
        }
        return providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Healthcare provider not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthcareProvider> findAll() {
        return providerRepository.findAll();
    }

    @Override
    @Transactional
    public HealthcareProvider update(UUID id, HealthcareProvider provider) {
        if (provider == null) {
            throw new ValidationException("Provider data is required.");
        }
        HealthcareProvider existing = findById(id);

        if (provider.getProviderId() != null && !provider.getProviderId().isBlank()
                && !provider.getProviderId().trim().equals(existing.getProviderId())) {
            throw new ValidationException("Provider ID cannot be changed.");
        }
        if (provider.getName() != null && !provider.getName().isBlank()) {
            existing.setName(provider.getName().trim());
        }
        if (provider.getAddress() != null && !provider.getAddress().isBlank()) {
            existing.setAddress(provider.getAddress().trim());
        }
        if (provider.getContactNumber() != null && !provider.getContactNumber().isBlank()) {
            existing.setContactNumber(provider.getContactNumber().trim());
        }
        if (provider.getSpecialty() != null && !provider.getSpecialty().isBlank()) {
            existing.setSpecialty(provider.getSpecialty().trim());
        }

        validateRequiredFields(existing, false);
        normalizeFields(existing);
        return providerRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        HealthcareProvider provider = findById(id);
        if (claimRepository.existsByHealthcareProvider_Id(id)) {
            throw new BusinessRuleException("Cannot delete a healthcare provider referenced by claims.");
        }
        providerRepository.delete(provider);
    }

    private void validateRequiredFields(HealthcareProvider provider, boolean requireProviderId) {
        if (provider == null) {
            throw new ValidationException("Provider data is required.");
        }
        if (requireProviderId && (provider.getProviderId() == null || provider.getProviderId().isBlank())) {
            throw new ValidationException("Provider ID is required.");
        }
        if (provider.getName() == null || provider.getName().isBlank()) {
            throw new ValidationException("Provider name is required.");
        }
        if (provider.getAddress() == null || provider.getAddress().isBlank()) {
            throw new ValidationException("Provider address is required.");
        }
        if (provider.getContactNumber() == null || !PHONE_PATTERN.matcher(provider.getContactNumber().trim()).matches()) {
            throw new ValidationException("A valid provider contact number is required.");
        }
        if (provider.getSpecialty() == null || provider.getSpecialty().isBlank()) {
            throw new ValidationException("Provider specialty is required.");
        }
    }

    private void normalizeFields(HealthcareProvider provider) {
        provider.setName(provider.getName().trim());
        provider.setAddress(provider.getAddress().trim());
        provider.setContactNumber(provider.getContactNumber().trim());
        provider.setSpecialty(provider.getSpecialty().trim());
    }
}
