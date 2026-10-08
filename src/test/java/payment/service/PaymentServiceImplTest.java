package payment.service;

import exception.ValidationException;
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
    private MemberRepository memberRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void createShouldNormalizePaymentMethodAndResolveMember() {
        Payment payment = validPayment();
        payment.setPaymentMethod("mobile money");
        when(paymentRepository.existsByPaymentId("PAY-1")).thenReturn(false);
        when(memberRepository.findById("NAT-1")).thenReturn(Optional.of(payment.getMember()));
        when(paymentRepository.save(payment)).thenReturn(payment);

        Payment result = paymentService.create(payment);

        assertEquals("Mobile Money", result.getPaymentMethod());
    }

    @Test
    void createShouldRejectFuturePaymentDate() {
        Payment payment = validPayment();
        payment.setPaymentDate(LocalDate.now().plusDays(1));

        assertThrows(ValidationException.class, () -> paymentService.create(payment));
    }

    @Test
    void createShouldRejectUnsupportedPaymentMethod() {
        Payment payment = validPayment();
        payment.setPaymentMethod("Cryptocurrency");

        assertThrows(ValidationException.class, () -> paymentService.create(payment));
    }

    private Payment validPayment() {
        Member member = new Member();
        member.setNationalId("NAT-1");

        Payment payment = new Payment();
        payment.setPaymentId("PAY-1");
        payment.setAmount(25000);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentMethod("Cash");
        payment.setMember(member);
        return payment;
    }
}
