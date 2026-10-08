package payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import payment.domain.Payment;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    boolean existsByPaymentId(String paymentId);
    boolean existsByMember_NationalId(String nationalId);
    List<Payment> findByMember_NationalId(String nationalId);
}
