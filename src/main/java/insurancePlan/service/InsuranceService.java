package insurancePlan.service;

import insurancePlan.domain.InsurancePlan;

import java.util.List;
import java.util.UUID;

public interface InsuranceService {
    InsurancePlan registerInsurance(InsurancePlan insurancePlan);
    InsurancePlan updateInsurance(UUID insurancePlanId, InsurancePlan insurancePlan);
    InsurancePlan deleteInsurance(UUID insurancePlanId);
    InsurancePlan findInsuranceById(UUID insurancePlanId);
    List<InsurancePlan> findAllInsurancePlans();
}
