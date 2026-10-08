package insuranceOfficer.service;

import insuranceOfficer.domain.InsuranceOfficer;

import java.util.List;
import java.util.UUID;

public interface InsuranceOfficerService {
    InsuranceOfficer create(InsuranceOfficer officer);
    InsuranceOfficer findById(UUID id);
    List<InsuranceOfficer> findAll();
    InsuranceOfficer update(UUID id, InsuranceOfficer officer);
    void delete(UUID id);
}
