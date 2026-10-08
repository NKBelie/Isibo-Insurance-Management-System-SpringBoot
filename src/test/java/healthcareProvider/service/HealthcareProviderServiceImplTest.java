package healthcareProvider.service;

import exception.DuplicateResourceException;
import exception.ValidationException;
import healthcareProvider.domain.HealthcareProvider;
import healthcareProvider.repository.HealthcareProviderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthcareProviderServiceImplTest {

    @Mock
    private HealthcareProviderRepository providerRepository;

    @InjectMocks
    private HealthcareProviderServiceImpl providerService;

    @Test
    void createShouldNormalizeProviderFields() {
        HealthcareProvider provider = validProvider();
        provider.setName("  Kigali Hospital  ");

        when(providerRepository.existsByProviderId("PROV-1")).thenReturn(false);
        when(providerRepository.save(provider)).thenReturn(provider);

        HealthcareProvider saved = providerService.create(provider);

        assertEquals("Kigali Hospital", saved.getName());
        verify(providerRepository).save(provider);
    }

    @Test
    void createShouldRejectDuplicateProviderId() {
        HealthcareProvider provider = validProvider();
        when(providerRepository.existsByProviderId("PROV-1")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> providerService.create(provider));
    }

    @Test
    void createShouldRejectInvalidContactNumber() {
        HealthcareProvider provider = validProvider();
        provider.setContactNumber("not-a-phone");

        assertThrows(ValidationException.class, () -> providerService.create(provider));
    }

    private HealthcareProvider validProvider() {
        HealthcareProvider provider = new HealthcareProvider();
        provider.setProviderId("PROV-1");
        provider.setName("Kigali Hospital");
        provider.setAddress("Kigali");
        provider.setContactNumber("+250788123456");
        provider.setSpecialty("General");
        return provider;
    }
}
