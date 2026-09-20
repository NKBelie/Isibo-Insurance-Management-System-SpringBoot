package member.service;

import insurancePlan.domain.InsurancePlan;
import insurancePlan.repository.InsurancePlanRepository;
import lombok.RequiredArgsConstructor;
import member.domain.Member;
import member.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final InsurancePlanRepository insurancePlanRepository;

    @Override
    public Member registerMember(Member theMember) {

        validateMemberData(theMember);

        if (memberRepository.existsById(
                theMember.getNationalId())) {

            throw new RuntimeException(
                    "Member with this National ID already exists"
            );
        }

        if (memberRepository.existsByMemberId(
                theMember.getMemberId())) {

            throw new RuntimeException(
                    "Member ID already exists"
            );
        }

        if (memberRepository.existsByEmail(
                theMember.getEmail())) {

            throw new RuntimeException(
                    "Email already exists"
            );
        }

        if (memberRepository.existsByPhone(
                theMember.getPhone())) {

            throw new RuntimeException(
                    "Phone number already exists"
            );
        }

        if (theMember.getInsurancePlan() == null ||
                theMember.getInsurancePlan().getId() == null) {

            throw new RuntimeException(
                    "Insurance Plan is required"
            );
        }

        InsurancePlan plan =
                insurancePlanRepository.findById(
                        theMember.getInsurancePlan().getId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Insurance Plan not found"
                        ));

        if (!plan.getStatus().equalsIgnoreCase("ACTIVE")) {

            throw new RuntimeException(
                    "Cannot register member under an inactive plan"
            );
        }

        theMember.setInsurancePlan(plan);

        return memberRepository.save(theMember);
    }

    @Override
    public Member updateMember(Member theMember) {

        if (theMember.getNationalId() == null ||
                theMember.getNationalId().isBlank()) {

            throw new RuntimeException(
                    "National ID is required"
            );
        }

        Member existing =
                memberRepository.findById(
                        theMember.getNationalId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Member not found"
                        ));

        validateMemberData(theMember);

        if (!existing.getEmail().equalsIgnoreCase(
                theMember.getEmail())) {

            if (memberRepository.existsByEmail(
                    theMember.getEmail())) {

                throw new RuntimeException(
                        "Email already exists"
                );
            }
        }

        if (!existing.getPhone().equals(
                theMember.getPhone())) {

            if (memberRepository.existsByPhone(
                    theMember.getPhone())) {

                throw new RuntimeException(
                        "Phone number already exists"
                );
            }
        }

        existing.setFirstName(
                theMember.getFirstName()
        );

        existing.setLastName(
                theMember.getLastName()
        );

        existing.setEmail(
                theMember.getEmail()
        );

        existing.setPhone(
                theMember.getPhone()
        );

        existing.setDateOfBirth(
                theMember.getDateOfBirth()
        );

        existing.setGender(
                theMember.getGender()
        );

        existing.setAddress(
                theMember.getAddress()
        );

        existing.setStatus(
                theMember.getStatus()
        );

        return memberRepository.save(existing);
    }

    @Override
    public Member deleteMember(Member theMember) {

        if (theMember.getNationalId() == null ||
                theMember.getNationalId().isBlank()) {

            throw new RuntimeException(
                    "National ID is required"
            );
        }

        Member existing =
                memberRepository.findById(
                        theMember.getNationalId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Member not found"
                        ));

        memberRepository.delete(existing);

        return existing;
    }

    @Override
    public Member findMemberById(Member theMember) {

        if (theMember.getNationalId() == null ||
                theMember.getNationalId().isBlank()) {

            throw new RuntimeException(
                    "National ID is required"
            );
        }

        return memberRepository.findById(
                theMember.getNationalId()
        ).orElseThrow(() ->
                new RuntimeException(
                        "Member not found"
                ));
    }

    @Override
    public List<Member> findAllMembers() {

        return memberRepository.findAll();
    }

    private void validateMemberData(Member member) {

        if (member.getNationalId() == null ||
                member.getNationalId().isBlank()) {

            throw new RuntimeException(
                    "National ID is required"
            );
        }

        if (member.getMemberId() == null ||
                member.getMemberId().isBlank()) {

            throw new RuntimeException(
                    "Member ID is required"
            );
        }

        if (member.getFirstName() == null ||
                member.getFirstName().isBlank()) {

            throw new RuntimeException(
                    "First name is required"
            );
        }

        if (member.getLastName() == null ||
                member.getLastName().isBlank()) {

            throw new RuntimeException(
                    "Last name is required"
            );
        }

        if (member.getEmail() == null ||
                member.getEmail().isBlank()) {

            throw new RuntimeException(
                    "Email is required"
            );
        }

        if (!member.getEmail().contains("@")) {

            throw new RuntimeException(
                    "Invalid email address"
            );
        }

        if (member.getPhone() == null ||
                member.getPhone().isBlank()) {

            throw new RuntimeException(
                    "Phone number is required"
            );
        }

        if (member.getDateOfBirth() == null) {

            throw new RuntimeException(
                    "Date of birth is required"
            );
        }

        if (member.getDateOfBirth().isAfter(
                LocalDate.now())) {

            throw new RuntimeException(
                    "Date of birth cannot be in the future"
            );
        }

        if (member.getGender() == null ||
                member.getGender().isBlank()) {

            throw new RuntimeException(
                    "Gender is required"
            );
        }

        if (member.getAddress() == null ||
                member.getAddress().isBlank()) {

            throw new RuntimeException(
                    "Address is required"
            );
        }

        if (member.getStatus() == null ||
                (!member.getStatus().equalsIgnoreCase("ACTIVE")
                        && !member.getStatus().equalsIgnoreCase("INACTIVE"))) {

            throw new RuntimeException(
                    "Status must be ACTIVE or INACTIVE"
            );
        }
    }
}