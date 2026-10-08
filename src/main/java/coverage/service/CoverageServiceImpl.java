package coverage.service;

import coverage.domain.Coverage;
import coverage.repository.CoverageRepository;
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
public class CoverageServiceImpl implements CoverageService {

    private final CoverageRepository coverageRepository;
    private final InsurancePlanRepository insurancePlanRepository;

    @Override
    @Transactional
    public Coverage registerCoverage(Coverage coverage) {
        validateCoverageForCreate(coverage);

        if (coverageRepository.existsByCoverageId(coverage.getCoverageId())) {
            throw new DuplicateResourceException("Coverage ID already exists.");
        }

        if (coverageRepository.existsByDescriptionIgnoreCase(coverage.getDescription())) {
            throw new DuplicateResourceException("Coverage description already exists.");
        }

        InsurancePlan plan = findActiveInsurancePlan(coverage.getInsurancePlan().getId());
        double existingCoverage = coverageRepository.findByInsurancePlan_Id(plan.getId())
                .stream()
                .mapToDouble(Coverage::getCoverageAmount)
                .sum();

        double newTotal = existingCoverage + coverage.getCoverageAmount();
        if (newTotal > plan.getCoverageLimit()) {
            throw new BusinessRuleException("Total coverage exceeds the insurance plan limit.");
        }

        coverage.setInsurancePlan(plan);
        return coverageRepository.save(coverage);
    }

    @Override
    public Coverage findCoverageById(UUID coverageId) {
        if (coverageId == null) {
            throw new ValidationException("Coverage ID is required.");
        }

        return coverageRepository.findById(coverageId)
                .orElseThrow(() -> new ResourceNotFoundException("Coverage not found."));
    }

    @Override
    public List<Coverage> findAllCoverage() {
        return coverageRepository.findAll();
    }

    @Override
    @Transactional
    public Coverage updateCoverage(UUID coverageId, Coverage coverage) {
        if (coverageId == null) {
            throw new ValidationException("Coverage ID is required.");
        }

        if (coverage == null) {
            throw new ValidationException("Coverage data is required.");
        }

        Coverage existing = findCoverageById(coverageId);

        if (coverage.getCoverageId() != null && !coverage.getCoverageId().isBlank()
                && !coverage.getCoverageId().equals(existing.getCoverageId())) {
            throw new BusinessRuleException("Coverage ID cannot be changed.");
        }

        if (coverage.getDescription() != null && !coverage.getDescription().isBlank()) {
            String description = coverage.getDescription().trim();
            if (!description.equalsIgnoreCase(existing.getDescription())
                    && coverageRepository.existsByDescriptionIgnoreCase(description)) {
                throw new DuplicateResourceException("Coverage description already exists.");
            }
            existing.setDescription(description);
        }

        InsurancePlan targetPlan = existing.getInsurancePlan();
        if (coverage.getInsurancePlan() != null && coverage.getInsurancePlan().getId() != null) {
            targetPlan = findActiveInsurancePlan(coverage.getInsurancePlan().getId());
        }

        double updatedAmount = coverage.getCoverageAmount() > 0
                ? coverage.getCoverageAmount()
                : existing.getCoverageAmount();

        double otherCoverage = coverageRepository.findByInsurancePlan_Id(targetPlan.getId())
                .stream()
                .filter(c -> !c.getId().equals(existing.getId()))
                .mapToDouble(Coverage::getCoverageAmount)
                .sum();

        double totalAfterChange = otherCoverage + updatedAmount;
        if (totalAfterChange > targetPlan.getCoverageLimit()) {
            throw new BusinessRuleException("Total coverage exceeds the insurance plan limit.");
        }

        if (coverage.getCoverageAmount() > 0) {
            existing.setCoverageAmount(coverage.getCoverageAmount());
        }

        if (coverage.getInsurancePlan() != null && coverage.getInsurancePlan().getId() != null
                && !targetPlan.getId().equals(existing.getInsurancePlan().getId())) {
            existing.setInsurancePlan(targetPlan);
        }

        validateCoverageState(existing);
        return coverageRepository.save(existing);
    }

    @Override
    @Transactional
    public Coverage deleteCoverage(UUID coverageId) {
        Coverage existing = findCoverageById(coverageId);
        coverageRepository.delete(existing);
        return existing;
    }

    private void validateCoverageForCreate(Coverage coverage) {
        if (coverage == null) {
            throw new ValidationException("Coverage data is required.");
        }

        validateCoverageState(coverage);

        if (coverage.getInsurancePlan() == null || coverage.getInsurancePlan().getId() == null) {
            throw new ValidationException("Insurance Plan is required.");
        }
    }

    private void validateCoverageState(Coverage coverage) {
        if (coverage.getCoverageId() == null || coverage.getCoverageId().isBlank()) {
            throw new ValidationException("Coverage ID is required.");
        }

        if (coverage.getDescription() == null || coverage.getDescription().isBlank()) {
            throw new ValidationException("Coverage description is required.");
        }

        if (coverage.getCoverageAmount() <= 0) {
            throw new ValidationException("Coverage amount must be greater than zero.");
        }
    }

    private InsurancePlan findActiveInsurancePlan(UUID insurancePlanId) {
        if (insurancePlanId == null) {
            throw new ValidationException("Insurance Plan is required.");
        }

        InsurancePlan plan = insurancePlanRepository.findById(insurancePlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Plan not found."));

        if (!"ACTIVE".equalsIgnoreCase(plan.getStatus())) {
            throw new BusinessRuleException("Insurance Plan must be ACTIVE.");
        }

        return plan;
    }
}