package insurancePlan.service;

import insurancePlan.domain.InsurancePlan;
import insurancePlan.repository.InsurancePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class InsuranceServiceImpl implements InsuranceService {

    private final InsurancePlanRepository insurancePlanRepository;

    @Override
    public InsurancePlan registerInsurance(InsurancePlan theInsurance) {

        if (theInsurance.getInsurancePlanId() == null ||
                theInsurance.getInsurancePlanId().isBlank()) {
            throw new RuntimeException("Insurance Plan ID is required");
        }

        if (theInsurance.getInsurancePlanName() == null ||
                theInsurance.getInsurancePlanName().isBlank()) {
            throw new RuntimeException("Insurance Plan name is required");
        }

        if (insurancePlanRepository.existsByInsurancePlanId(
                theInsurance.getInsurancePlanId())) {
            throw new RuntimeException("Insurance Plan ID already exists");
        }

        if (theInsurance.getMonthlyPremium() <= 0) {
            throw new RuntimeException(
                    "Monthly premium must be greater than zero"
            );
        }

        if (theInsurance.getCoverageLimit() <= 0) {
            throw new RuntimeException(
                    "Coverage limit must be greater than zero"
            );
        }

        if (theInsurance.getDurationMonths() <= 0) {
            throw new RuntimeException(
                    "Duration must be greater than zero"
            );
        }

        if (!theInsurance.getStatus().equalsIgnoreCase("ACTIVE")
                && !theInsurance.getStatus().equalsIgnoreCase("INACTIVE")) {

            throw new RuntimeException(
                    "Status must be ACTIVE or INACTIVE"
            );
        }

        return insurancePlanRepository.save(theInsurance);
    }

    @Override
    public InsurancePlan updateInsurance(InsurancePlan theInsurance) {

        if (theInsurance.getId() == null) {
            throw new RuntimeException("Insurance Plan ID is required");
        }

        InsurancePlan existing =
                insurancePlanRepository.findById(theInsurance.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Insurance Plan not found"
                                ));

        existing.setInsurancePlanName(
                theInsurance.getInsurancePlanName()
        );

        existing.setDescription(
                theInsurance.getDescription()
        );

        existing.setMonthlyPremium(
                theInsurance.getMonthlyPremium()
        );

        existing.setCoverageLimit(
                theInsurance.getCoverageLimit()
        );

        existing.setDurationMonths(
                theInsurance.getDurationMonths()
        );

        existing.setStatus(
                theInsurance.getStatus()
        );

        return insurancePlanRepository.save(existing);
    }

    @Override
    public InsurancePlan deleteInsurance(InsurancePlan theInsurance) {

        if (theInsurance.getId() == null) {
            throw new RuntimeException("Insurance Plan ID is required");
        }

        InsurancePlan existing =
                insurancePlanRepository.findById(theInsurance.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Insurance Plan not found"
                                ));

        insurancePlanRepository.delete(existing);

        return existing;
    }

    @Override
    public InsurancePlan findInsuranceById(
            InsurancePlan theInsurance) {

        if (theInsurance.getId() == null) {
            throw new RuntimeException("Insurance Plan ID is required");
        }

        return insurancePlanRepository.findById(
                theInsurance.getId()
        ).orElseThrow(() ->
                new RuntimeException(
                        "Insurance Plan not found"
                ));
    }

    @Override
    public List<InsurancePlan> findAllInsurancePlans() {
        return insurancePlanRepository.findAll();
    }
}