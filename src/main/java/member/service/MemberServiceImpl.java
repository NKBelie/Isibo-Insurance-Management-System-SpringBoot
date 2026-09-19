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

        if (theMember.getNationalId() == null ||
                theMember.getNationalId().isBlank()) {

            throw new RuntimeException(
                    "National ID is required"
            );
        }

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

        if (theMember.getDateOfBirth() == null ||
                theMember.getDateOfBirth().isAfter(
                        LocalDate.now())) {

            throw new RuntimeException(
                    "Invalid date of birth"
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

        if (theMember.getNationalId() == null) {
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

        existing.setFirstName(theMember.getFirstName());
        existing.setLastName(theMember.getLastName());
        existing.setEmail(theMember.getEmail());
        existing.setPhone(theMember.getPhone());
        existing.setDateOfBirth(theMember.getDateOfBirth());
        existing.setGender(theMember.getGender());
        existing.setAddress(theMember.getAddress());
        existing.setStatus(theMember.getStatus());

        return memberRepository.save(existing);
    }

    @Override
    public Member deleteMember(Member theMember) {

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
}