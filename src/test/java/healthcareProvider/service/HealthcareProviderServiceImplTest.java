package healthcareProvider.service;

import exception.ValidationException;
import base.BusinessIdGenerator;
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
    @Mock
    private BusinessIdGenerator businessIdGenerator;

    @InjectMocks
    private HealthcareProviderServiceImpl providerService;

    @Test
    void createShouldNormalizeProviderFields() {
        HealthcareProvider provider = validProvider();
        provider.setName("  Kigali Hospital  ");

        when(businessIdGenerator.nextProviderId()).thenReturn("HCP-001");
        when(providerRepository.save(provider)).thenReturn(provider);

        HealthcareProvider saved = providerService.create(provider);

        assertEquals("Kigali Hospital", saved.getName());
        assertEquals("HCP-001", saved.getProviderId());
        verify(providerRepository).save(provider);
    }

    @Test
    void createShouldAssignProviderIdWithoutUsingSubmittedValue() {
        HealthcareProvider provider = validProvider();
        when(businessIdGenerator.nextProviderId()).thenReturn("HCP-002");
        when(providerRepository.save(provider)).thenReturn(provider);

        assertEquals("HCP-002", providerService.create(provider).getProviderId());
    }

    @Test
    void createShouldRejectInvalidContactNumber() {
        HealthcareProvider provider = validProvider();
        provider.setContactNumber("not-a-phone");
        when(businessIdGenerator.nextProviderId()).thenReturn("HCP-003");

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
