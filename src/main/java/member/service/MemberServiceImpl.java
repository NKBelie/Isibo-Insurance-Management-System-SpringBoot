package member.service;

import exception.BusinessRuleException;
import exception.DuplicateResourceException;
import exception.ResourceNotFoundException;
import exception.ValidationException;
import claim.repository.ClaimRepository;
import base.BusinessIdGenerator;
import insurancePlan.domain.InsurancePlan;
import insurancePlan.repository.InsurancePlanRepository;
import lombok.RequiredArgsConstructor;
import member.domain.Member;
import member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment.repository.PaymentRepository;
import user.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class MemberServiceImpl implements MemberService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern NATIONAL_ID_PATTERN = Pattern.compile("^[0-9]{16}$");

    private final BusinessIdGenerator businessIdGenerator;
    private final MemberRepository memberRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final ClaimRepository claimRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Member registerMember(Member member) {
        if (member == null) {
            throw new ValidationException("Member data is required.");
        }
        member.setMemberId(businessIdGenerator.nextMemberId());
        validateMemberForCreate(member);

        if (memberRepository.existsById(member.getNationalId())) {
            throw new DuplicateResourceException("National ID already exists.");
        }

        if (memberRepository.existsByEmailIgnoreCase(member.getEmail())) {
            throw new DuplicateResourceException("Email already exists.");
        }

        if (memberRepository.existsByPhone(member.getPhone())) {
            throw new DuplicateResourceException("Phone number already exists.");
        }

        InsurancePlan plan = findActiveInsurancePlan(member.getInsurancePlan().getId());
        member.setInsurancePlan(plan);

        return memberRepository.save(member);
    }

    @Override
    public Member findMemberById(String nationalId) {
        if (nationalId == null || nationalId.isBlank()) {
            throw new ValidationException("National ID is required.");
        }

        return memberRepository.findById(nationalId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found."));
    }

    @Override
    public List<Member> findAllMembers() {
        return memberRepository.findAll();
    }

    @Override
    @Transactional
    public Member updateMember(String nationalId, Member member) {
        if (nationalId == null || nationalId.isBlank()) {
            throw new ValidationException("National ID is required.");
        }

        Member existing = findMemberById(nationalId);

        if (member.getMemberId() != null && !member.getMemberId().isBlank()
                && !member.getMemberId().equals(existing.getMemberId())) {
            throw new BusinessRuleException("Member ID cannot be changed.");
        }

        if (member.getEmail() != null && !member.getEmail().isBlank()) {
            if (!member.getEmail().equalsIgnoreCase(existing.getEmail())
                    && memberRepository.existsByEmailIgnoreCase(member.getEmail())) {
                throw new DuplicateResourceException("Email already exists.");
            }
        }

        if (member.getPhone() != null && !member.getPhone().isBlank()) {
            if (!member.getPhone().equals(existing.getPhone())
                    && memberRepository.existsByPhone(member.getPhone())) {
                throw new DuplicateResourceException("Phone number already exists.");
            }
        }

        validateMemberForUpdate(member, existing);

        if (member.getFirstName() != null && !member.getFirstName().isBlank()) {
            existing.setFirstName(member.getFirstName());
        }
        if (member.getLastName() != null && !member.getLastName().isBlank()) {
            existing.setLastName(member.getLastName());
        }
        if (member.getEmail() != null && !member.getEmail().isBlank()) {
            existing.setEmail(member.getEmail());
        }
        if (member.getPhone() != null && !member.getPhone().isBlank()) {
            existing.setPhone(member.getPhone());
        }
        if (member.getDateOfBirth() != null) {
            existing.setDateOfBirth(member.getDateOfBirth());
        }
        if (member.getGender() != null && !member.getGender().isBlank()) {
            existing.setGender(member.getGender());
        }
        if (member.getAddress() != null && !member.getAddress().isBlank()) {
            existing.setAddress(member.getAddress());
        }
        if (member.getStatus() != null && !member.getStatus().isBlank()) {
            existing.setStatus(member.getStatus());
        }
        if (member.getInsurancePlan() != null && member.getInsurancePlan().getId() != null) {
            existing.setInsurancePlan(findActiveInsurancePlan(member.getInsurancePlan().getId()));
        }

        validateMemberState(existing);
        return memberRepository.save(existing);
    }

    @Override
    @Transactional
    public Member deleteMember(String nationalId) {
        Member existing = findMemberById(nationalId);
        if (userRepository.existsByMember_NationalId(nationalId)) {
            throw new BusinessRuleException("Cannot delete a member linked to a customer account. Reject the application instead.");
        }
        if (claimRepository.existsByMember_NationalId(nationalId)
                || paymentRepository.existsByMember_NationalId(nationalId)) {
            throw new BusinessRuleException("Cannot delete a member with associated claims or payments.");
        }
        memberRepository.delete(existing);
        return existing;
    }

    private void validateMemberForCreate(Member member) {
        if (member == null) {
            throw new ValidationException("Member data is required.");
        }

        validateMemberState(member);

        if (member.getInsurancePlan() == null || member.getInsurancePlan().getId() == null) {
            throw new ValidationException("Insurance Plan is required.");
        }
    }

    private void validateMemberForUpdate(Member member, Member existing) {
        if (member == null) {
            throw new ValidationException("Member data is required.");
        }

        if (member.getNationalId() != null && !member.getNationalId().isBlank()
                && !member.getNationalId().equals(existing.getNationalId())) {
            throw new BusinessRuleException("National ID cannot be changed.");
        }

        if (member.getInsurancePlan() != null && member.getInsurancePlan().getId() != null) {
            findActiveInsurancePlan(member.getInsurancePlan().getId());
        }
    }

    private void validateMemberState(Member member) {
        if (member.getNationalId() == null || member.getNationalId().isBlank()) {
            throw new ValidationException("National ID is required.");
        }

        if (!NATIONAL_ID_PATTERN.matcher(member.getNationalId()).matches()) {
            throw new ValidationException("National ID must contain exactly 16 digits.");
        }

        if (member.getFirstName() == null || member.getFirstName().isBlank()) {
            throw new ValidationException("First name is required.");
        }

        if (member.getLastName() == null || member.getLastName().isBlank()) {
            throw new ValidationException("Last name is required.");
        }

        if (member.getEmail() == null || member.getEmail().isBlank()) {
            throw new ValidationException("Email is required.");
        }

        if (!EMAIL_PATTERN.matcher(member.getEmail()).matches()) {
            throw new ValidationException("Email must be valid.");
        }

        if (member.getPhone() == null || member.getPhone().isBlank()) {
            throw new ValidationException("Phone is required.");
        }

        if (member.getDateOfBirth() == null) {
            throw new ValidationException("Date of birth is required.");
        }

        if (member.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new ValidationException("Date of birth cannot be in the future.");
        }

        if (member.getGender() == null || member.getGender().isBlank()) {
            throw new ValidationException("Gender is required.");
        }

        if (member.getAddress() == null || member.getAddress().isBlank()) {
            throw new ValidationException("Address is required.");
        }

        if (member.getStatus() == null || member.getStatus().isBlank()) {
            throw new ValidationException("Status is required.");
        }

        String normalizedStatus = member.getStatus().trim().toUpperCase();
        if (!normalizedStatus.equals("ACTIVE")
                && !normalizedStatus.equals("PENDING")
                && !normalizedStatus.equals("INACTIVE")
                && !normalizedStatus.equals("REJECTED")
                && !normalizedStatus.equals("EXPIRED")) {
            throw new ValidationException("Member status must be PENDING, ACTIVE, INACTIVE, REJECTED, or EXPIRED.");
        }
        member.setStatus(normalizedStatus);
    }

    private InsurancePlan findActiveInsurancePlan(java.util.UUID insurancePlanId) {
        if (insurancePlanId == null) {
            throw new ValidationException("Insurance Plan is required.");
        }

        InsurancePlan plan = insurancePlanRepository.findById(insurancePlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Plan not found."));

        if (!"ACTIVE".equalsIgnoreCase(plan.getStatus())) {
            throw new BusinessRuleException("New members can only be assigned to ACTIVE Insurance Plans.");
        }

        return plan;
    }
}