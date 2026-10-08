package payment.service;

import payment.domain.Payment;

import java.util.List;
import java.util.UUID;

public interface PaymentService {
    Payment create(Payment payment);
    Payment findById(UUID id);
    List<Payment> findAll();
    Payment update(UUID id, Payment payment);
    void delete(UUID id);
}
