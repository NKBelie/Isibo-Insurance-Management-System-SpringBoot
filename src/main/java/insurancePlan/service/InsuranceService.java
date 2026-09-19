package insurancePlan.service;

import insurancePlan.domain.InsurancePlan;

import java.util.List;

public interface InsuranceService {
    InsurancePlan registerInsurance(InsurancePlan theInsurance);
    InsurancePlan updateInsurance(InsurancePlan theInsurance);
    InsurancePlan deleteInsurance(InsurancePlan theInsurance);
    InsurancePlan findInsuranceById(InsurancePlan theInsurance);
    List<InsurancePlan> findAllInsurancePlans();
}
