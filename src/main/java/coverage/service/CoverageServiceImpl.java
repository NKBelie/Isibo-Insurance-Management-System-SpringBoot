package coverage.service;

import coverage.domain.Coverage;
import coverage.repository.CoverageRepository;
import insurancePlan.domain.InsurancePlan;
import insurancePlan.repository.InsurancePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CoverageServiceImpl implements CoverageService {

    private final CoverageRepository coverageRepository;
    private final InsurancePlanRepository insurancePlanRepository;

    @Override
    public Coverage registerCoverage(Coverage theCoverage) {

        if (theCoverage.getCoverageId() == null ||
                theCoverage.getCoverageId().isBlank()) {

            throw new RuntimeException(
                    "Coverage ID is required"
            );
        }

        if (theCoverage.getDescription() == null ||
                theCoverage.getDescription().isBlank()) {

            throw new RuntimeException(
                    "Coverage description is required"
            );
        }

        if (theCoverage.getCoverageAmount() <= 0) {
            throw new RuntimeException(
                    "Coverage amount must be greater than zero"
            );
        }

        if (theCoverage.getInsurancePlan() == null ||
                theCoverage.getInsurancePlan().getId() == null) {

            throw new RuntimeException(
                    "Insurance Plan is required"
            );
        }

        InsurancePlan plan =
                insurancePlanRepository.findById(
                        theCoverage.getInsurancePlan().getId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Insurance Plan not found"
                        ));

        if (!plan.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw new RuntimeException(
                    "Cannot add coverage to an inactive plan"
            );
        }

        double existingCoverage =
                coverageRepository
                        .findByInsurancePlanId(plan.getId())
                        .stream()
                        .mapToDouble(Coverage::getCoverageAmount)
                        .sum();

        double newTotal =
                existingCoverage +
                        theCoverage.getCoverageAmount();

        if (newTotal > plan.getCoverageLimit()) {
            throw new RuntimeException(
                    "Total coverage exceeds the insurance plan limit"
            );
        }

        theCoverage.setInsurancePlan(plan);

        return coverageRepository.save(theCoverage);
    }

    @Override
    public Coverage updateCoverage(Coverage theCoverage) {

        if (theCoverage.getId() == null) {
            throw new RuntimeException(
                    "Coverage ID is required"
            );
        }

        Coverage existing =
                coverageRepository.findById(theCoverage.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Coverage not found"
                                ));

        existing.setDescription(
                theCoverage.getDescription()
        );

        existing.setCoverageAmount(
                theCoverage.getCoverageAmount()
        );

        return coverageRepository.save(existing);
    }

    @Override
    public Coverage deleteCoverage(Coverage theCoverage) {

        if (theCoverage.getId() == null) {
            throw new RuntimeException(
                    "Coverage ID is required"
            );
        }

        Coverage existing =
                coverageRepository.findById(theCoverage.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Coverage not found"
                                ));

        coverageRepository.delete(existing);

        return existing;
    }

    @Override
    public Coverage findAllCoverageById(Coverage theCoverage) {

        if (theCoverage.getId() == null) {
            throw new RuntimeException(
                    "Coverage ID is required"
            );
        }

        return coverageRepository.findById(
                theCoverage.getId()
        ).orElseThrow(() ->
                new RuntimeException(
                        "Coverage not found"
                ));
    }

    @Override
    public List<Coverage> findAllCoverage() {
        return coverageRepository.findAll();
    }
}