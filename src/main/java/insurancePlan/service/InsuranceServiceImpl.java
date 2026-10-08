package insurancePlan.service;

import exception.BusinessRuleException;
import exception.DuplicateResourceException;
import exception.ResourceNotFoundException;
import exception.ValidationException;
import insurancePlan.domain.InsurancePlan;
import insurancePlan.repository.InsurancePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class InsuranceServiceImpl implements InsuranceService {

    private final InsurancePlanRepository insurancePlanRepository;

    @Override
    @Transactional
    public InsurancePlan registerInsurance(InsurancePlan insurancePlan) {
        validatePlanForCreate(insurancePlan);

        if (insurancePlanRepository.existsByInsurancePlanId(insurancePlan.getInsurancePlanId())) {
            throw new DuplicateResourceException("Insurance Plan ID already exists.");
        }

        if (insurancePlanRepository.existsByInsurancePlanNameIgnoreCase(insurancePlan.getInsurancePlanName())) {
            throw new DuplicateResourceException("Insurance Plan name already exists.");
        }

        validatePlanBusinessRules(insurancePlan);
        insurancePlan.setStatus(insurancePlan.getStatus().trim().toUpperCase());
        return insurancePlanRepository.save(insurancePlan);
    }

    @Override
    public InsurancePlan findInsuranceById(UUID insurancePlanId) {
        if (insurancePlanId == null) {
            throw new ValidationException("Insurance Plan ID is required.");
        }

        return insurancePlanRepository.findById(insurancePlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Plan not found."));
    }

    @Override
    public List<InsurancePlan> findAllInsurancePlans() {
        return insurancePlanRepository.findAll();
    }

    @Override
    @Transactional
    public InsurancePlan updateInsurance(UUID insurancePlanId, InsurancePlan insurancePlan) {
        if (insurancePlanId == null) {
            throw new ValidationException("Insurance Plan ID is required.");
        }

        InsurancePlan existing = findInsuranceById(insurancePlanId);

        if (insurancePlan.getInsurancePlanId() != null && !insurancePlan.getInsurancePlanId().isBlank()
                && !insurancePlan.getInsurancePlanId().equals(existing.getInsurancePlanId())) {
            throw new BusinessRuleException("Insurance Plan business identifier cannot be changed.");
        }

        if (insurancePlan.getInsurancePlanName() != null && !insurancePlan.getInsurancePlanName().isBlank()) {
            String normalizedName = insurancePlan.getInsurancePlanName().trim();
            if (!normalizedName.equalsIgnoreCase(existing.getInsurancePlanName())
                    && insurancePlanRepository.existsByInsurancePlanNameIgnoreCase(normalizedName)) {
                throw new DuplicateResourceException("Insurance Plan name already exists.");
            }
            existing.setInsurancePlanName(normalizedName);
        }

        if (insurancePlan.getDescription() != null) {
            existing.setDescription(insurancePlan.getDescription());
        }

        if (insurancePlan.getMonthlyPremium() > 0) {
            existing.setMonthlyPremium(insurancePlan.getMonthlyPremium());
        }

        if (insurancePlan.getCoverageLimit() > 0) {
            existing.setCoverageLimit(insurancePlan.getCoverageLimit());
        }

        if (insurancePlan.getDurationMonths() > 0) {
            existing.setDurationMonths(insurancePlan.getDurationMonths());
        }

        if (insurancePlan.getStatus() != null && !insurancePlan.getStatus().isBlank()) {
            existing.setStatus(insurancePlan.getStatus().trim().toUpperCase());
        }

        validatePlanBusinessRules(existing);
        return insurancePlanRepository.save(existing);
    }

    @Override
    @Transactional
    public InsurancePlan deleteInsurance(UUID insurancePlanId) {
        InsurancePlan existing = findInsuranceById(insurancePlanId);

        if (!existing.getMembers().isEmpty() || !existing.getCoverages().isEmpty()) {
            throw new BusinessRuleException(
                    "Cannot delete insurance plan because it has associated members or coverage."
            );
        }

        insurancePlanRepository.delete(existing);
        return existing;
    }

    private void validatePlanForCreate(InsurancePlan insurancePlan) {
        if (insurancePlan == null) {
            throw new ValidationException("Insurance Plan data is required.");
        }
        if (insurancePlan.getInsurancePlanId() == null || insurancePlan.getInsurancePlanId().isBlank()) {
            throw new ValidationException("Insurance Plan ID is required.");
        }
        if (insurancePlan.getInsurancePlanName() == null || insurancePlan.getInsurancePlanName().isBlank()) {
            throw new ValidationException("Insurance Plan name is required.");
        }
    }

    private void validatePlanBusinessRules(InsurancePlan insurancePlan) {
        if (insurancePlan.getMonthlyPremium() <= 0) {
            throw new ValidationException("Monthly premium must be greater than zero.");
        }

        if (insurancePlan.getCoverageLimit() <= 0) {
            throw new ValidationException("Coverage limit must be greater than zero.");
        }

        if (insurancePlan.getDurationMonths() <= 0) {
            throw new ValidationException("Duration must be greater than zero.");
        }

        if (insurancePlan.getStatus() == null || insurancePlan.getStatus().isBlank()) {
            throw new ValidationException("Status is required.");
        }

        String normalizedStatus = insurancePlan.getStatus().trim().toUpperCase();
        if (!normalizedStatus.equals("ACTIVE") && !normalizedStatus.equals("INACTIVE")) {
            throw new ValidationException("Status must be ACTIVE or INACTIVE.");
        }

        insurancePlan.setStatus(normalizedStatus);
    }
}