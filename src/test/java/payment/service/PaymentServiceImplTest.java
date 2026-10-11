package payment.service;

import exception.ValidationException;
import base.BusinessIdGenerator;
import member.domain.Member;
import member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import payment.domain.Payment;
import payment.repository.PaymentRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BusinessIdGenerator businessIdGenerator;
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void createShouldNormalizePaymentMethodAndResolveMember() {
        Payment payment = validPayment();
        payment.setPaymentMethod("mobile money");
        when(businessIdGenerator.nextPaymentId()).thenReturn("PAY-001");
        when(memberRepository.findById("1199887766554433")).thenReturn(Optional.of(payment.getMember()));
        when(paymentRepository.save(payment)).thenReturn(payment);

        Payment result = paymentService.create(payment);

        assertEquals("Mobile Money", result.getPaymentMethod());
    }

    @Test
    void createShouldRejectFuturePaymentDate() {
        Payment payment = validPayment();
        payment.setPaymentDate(LocalDate.now().plusDays(1));
        when(businessIdGenerator.nextPaymentId()).thenReturn("PAY-001");

        assertThrows(ValidationException.class, () -> paymentService.create(payment));
    }

    @Test
    void createShouldRejectUnsupportedPaymentMethod() {
        Payment payment = validPayment();
        payment.setPaymentMethod("Cryptocurrency");
        when(businessIdGenerator.nextPaymentId()).thenReturn("PAY-001");

        assertThrows(ValidationException.class, () -> paymentService.create(payment));
    }

    private Payment validPayment() {
        Member member = new Member();
        member.setNationalId("1199887766554433");

        Payment payment = new Payment();
        payment.setPaymentId("PAY-001");
        payment.setAmount(25000);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentMethod("Cash");
        payment.setMember(member);
        return payment;
    }
}
