package insuranceOfficer.service;

import exception.BusinessRuleException;
import exception.DuplicateResourceException;
import exception.ResourceNotFoundException;
import exception.ValidationException;
import claim.repository.ClaimRepository;
import insuranceOfficer.domain.InsuranceOfficer;
import insuranceOfficer.repository.InsuranceOfficerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InsuranceOfficerServiceImpl implements InsuranceOfficerService {

    private final InsuranceOfficerRepository officerRepository;
    private final ClaimRepository claimRepository;

    @Override
    @Transactional
    public InsuranceOfficer create(InsuranceOfficer officer) {
        validate(officer, true);
        officer.setOfficerId(officer.getOfficerId().trim());
        if (officerRepository.existsByOfficerId(officer.getOfficerId())) {
            throw new DuplicateResourceException("Insurance Officer ID already exists.");
        }
        normalize(officer);
        return officerRepository.save(officer);
    }

    @Override
    @Transactional(readOnly = true)
    public InsuranceOfficer findById(UUID id) {
        if (id == null) {
            throw new ValidationException("Insurance Officer ID is required.");
        }
        return officerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Officer not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceOfficer> findAll() {
        return officerRepository.findAll();
    }

    @Override
    @Transactional
    public InsuranceOfficer update(UUID id, InsuranceOfficer officer) {
        if (officer == null) {
            throw new ValidationException("Insurance Officer data is required.");
        }
        InsuranceOfficer existing = findById(id);
        if (officer.getOfficerId() != null && !officer.getOfficerId().isBlank()
                && !officer.getOfficerId().trim().equals(existing.getOfficerId())) {
            throw new ValidationException("Insurance Officer ID cannot be changed.");
        }
        if (officer.getFullName() != null && !officer.getFullName().isBlank()) {
            existing.setFullName(officer.getFullName());
        }
        if (officer.getRole() != null && !officer.getRole().isBlank()) {
            existing.setRole(officer.getRole());
        }
        if (officer.getContact() != null && !officer.getContact().isBlank()) {
            existing.setContact(officer.getContact());
        }
        validate(existing, false);
        normalize(existing);
        return officerRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        InsuranceOfficer officer = findById(id);
        if (claimRepository.existsByInsuranceOfficer_Id(id)) {
            throw new BusinessRuleException("Cannot delete an Insurance Officer referenced by claim decisions.");
        }
        officerRepository.delete(officer);
    }

    private void validate(InsuranceOfficer officer, boolean requireId) {
        if (officer == null) {
            throw new ValidationException("Insurance Officer data is required.");
        }
        if (requireId && (officer.getOfficerId() == null || officer.getOfficerId().isBlank())) {
            throw new ValidationException("Insurance Officer ID is required.");
        }
        if (officer.getFullName() == null || officer.getFullName().isBlank()) {
            throw new ValidationException("Insurance Officer name is required.");
        }
        if (officer.getRole() == null || officer.getRole().isBlank()) {
            throw new ValidationException("Insurance Officer role is required.");
        }
        if (officer.getContact() == null || officer.getContact().isBlank()) {
            throw new ValidationException("Insurance Officer contact is required.");
        }
    }

    private void normalize(InsuranceOfficer officer) {
        officer.setFullName(officer.getFullName().trim());
        officer.setRole(officer.getRole().trim());
        officer.setContact(officer.getContact().trim());
    }
}
