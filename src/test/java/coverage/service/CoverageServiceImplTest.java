package coverage.service;

import coverage.domain.Coverage;
import coverage.repository.CoverageRepository;
import exception.BusinessRuleException;
import insurancePlan.domain.InsurancePlan;
import insurancePlan.repository.InsurancePlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoverageServiceImplTest {

    @Mock
    private CoverageRepository coverageRepository;

    @Mock
    private InsurancePlanRepository insurancePlanRepository;

    @InjectMocks
    private CoverageServiceImpl coverageService;

    @Test
    void updateCoverageShouldRejectAmountThatExceedsPlanLimitOnSamePlan() {
        InsurancePlan plan = new InsurancePlan();
        plan.setId(UUID.randomUUID());
        plan.setInsurancePlanId("PLN-100");
        plan.setInsurancePlanName("Gold");
        plan.setStatus("ACTIVE");
        plan.setCoverageLimit(500);

        Coverage existing = new Coverage();
        existing.setId(UUID.randomUUID());
        existing.setCoverageId("COV-001");
        existing.setDescription("Hospitalization");
        existing.setCoverageAmount(300);
        existing.setInsurancePlan(plan);

        Coverage updated = new Coverage();
        updated.setCoverageAmount(700);

        when(coverageRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(coverageRepository.findByInsurancePlan_Id(plan.getId())).thenReturn(List.of(existing));

        assertThrows(BusinessRuleException.class,
                () -> coverageService.updateCoverage(existing.getId(), updated));
    }
}
