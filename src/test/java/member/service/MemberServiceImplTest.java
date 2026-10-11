package member.service;

import base.BusinessIdGenerator;
import claim.repository.ClaimRepository;
import exception.ValidationException;
import insurancePlan.domain.InsurancePlan;
import insurancePlan.repository.InsurancePlanRepository;
import member.domain.Member;
import member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import payment.repository.PaymentRepository;
import user.repository.UserRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private BusinessIdGenerator businessIdGenerator;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private InsurancePlanRepository insurancePlanRepository;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MemberServiceImpl memberService;

    @Test
    void registerMemberShouldRejectNationalIdThatIsNotSixteenDigits() {
        Member member = validMember();
        member.setNationalId("123456789012345");
        when(businessIdGenerator.nextMemberId()).thenReturn("MEM-001");

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> memberService.registerMember(member)
        );

        assertEquals("National ID must contain exactly 16 digits.", exception.getMessage());
    }

    @Test
    void registerMemberShouldGenerateMemberIdAndAcceptSixteenDigitNationalId() {
        Member member = validMember();
        InsurancePlan plan = new InsurancePlan();
        plan.setId(member.getInsurancePlan().getId());
        plan.setStatus("ACTIVE");
        when(businessIdGenerator.nextMemberId()).thenReturn("MEM-001");
        when(memberRepository.existsById(member.getNationalId())).thenReturn(false);
        when(memberRepository.existsByEmailIgnoreCase(member.getEmail())).thenReturn(false);
        when(memberRepository.existsByPhone(member.getPhone())).thenReturn(false);
        when(insurancePlanRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
        when(memberRepository.save(member)).thenReturn(member);

        Member registered = memberService.registerMember(member);

        assertEquals("MEM-001", registered.getMemberId());
        assertEquals("1234567890123456", registered.getNationalId());
    }

    private Member validMember() {
        InsurancePlan plan = new InsurancePlan();
        plan.setId(UUID.randomUUID());

        Member member = new Member();
        member.setNationalId("1234567890123456");
        member.setFirstName("Aline");
        member.setLastName("Example");
        member.setEmail("aline@example.com");
        member.setPhone("+250788123456");
        member.setDateOfBirth(LocalDate.of(1995, 1, 1));
        member.setGender("FEMALE");
        member.setAddress("Kigali");
        member.setStatus("ACTIVE");
        member.setInsurancePlan(plan);
        return member;
    }
}
