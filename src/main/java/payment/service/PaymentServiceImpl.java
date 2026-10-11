package payment.service;

import exception.DuplicateResourceException;
import exception.ResourceNotFoundException;
import exception.ValidationException;
import base.BusinessIdGenerator;
import lombok.RequiredArgsConstructor;
import member.domain.Member;
import member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment.domain.Payment;
import payment.repository.PaymentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Set<String> PAYMENT_METHODS =
            Set.of("BANK TRANSFER", "CASH", "CARD", "MOBILE MONEY");

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final BusinessIdGenerator businessIdGenerator;

    @Override
    @Transactional
    public Payment create(Payment payment) {
        if (payment == null) {
            throw new ValidationException("Payment data is required.");
        }
        payment.setPaymentId(businessIdGenerator.nextPaymentId());
        validate(payment);
        payment.setPaymentMethod(normalizeMethod(payment.getPaymentMethod()));
        payment.setMember(findMember(payment.getMember()));
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment findById(UUID id) {
        if (id == null) {
            throw new ValidationException("Payment ID is required.");
        }
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByMemberNationalId(String nationalId) {
        if (nationalId == null || nationalId.isBlank()) {
            throw new ValidationException("Member national ID is required.");
        }
        return paymentRepository.findByMember_NationalId(nationalId);
    }

    @Override
    @Transactional
    public Payment update(UUID id, Payment payment) {
        if (payment == null) {
            throw new ValidationException("Payment data is required.");
        }
        Payment existing = findById(id);
        if (payment.getPaymentId() != null && !payment.getPaymentId().isBlank()
                && !payment.getPaymentId().trim().equals(existing.getPaymentId())) {
            throw new ValidationException("Payment ID cannot be changed.");
        }
        if (payment.getAmount() < 0) {
            throw new ValidationException("Payment amount must be greater than zero.");
        }
        if (payment.getAmount() > 0) {
            existing.setAmount(payment.getAmount());
        }
        if (payment.getPaymentDate() != null) {
            validateDate(payment.getPaymentDate());
            existing.setPaymentDate(payment.getPaymentDate());
        }
        if (payment.getPaymentMethod() != null && !payment.getPaymentMethod().isBlank()) {
            existing.setPaymentMethod(normalizeMethod(payment.getPaymentMethod()));
        }
        if (payment.getMember() != null && payment.getMember().getNationalId() != null
                && !payment.getMember().getNationalId().isBlank()) {
            existing.setMember(findMember(payment.getMember()));
        }
        return paymentRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        paymentRepository.delete(findById(id));
    }

    private void validate(Payment payment) {
        if (payment == null) {
            throw new ValidationException("Payment data is required.");
        }
        if (payment.getAmount() <= 0) {
            throw new ValidationException("Payment amount must be greater than zero.");
        }
        if (payment.getPaymentDate() == null) {
            throw new ValidationException("Payment date is required.");
        }
        validateDate(payment.getPaymentDate());
        normalizeMethod(payment.getPaymentMethod());
        if (payment.getMember() == null || payment.getMember().getNationalId() == null
                || payment.getMember().getNationalId().isBlank()) {
            throw new ValidationException("Member is required.");
        }
    }

    private void validateDate(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("Payment date cannot be in the future.");
        }
    }

    private String normalizeMethod(String method) {
        if (method == null || method.isBlank()) {
            throw new ValidationException("Payment method is required.");
        }
        String normalized = method.trim().toUpperCase();
        if (!PAYMENT_METHODS.contains(normalized)) {
            throw new ValidationException("Payment method must be Bank Transfer, Cash, Card, or Mobile Money.");
        }
        return switch (normalized) {
            case "BANK TRANSFER" -> "Bank Transfer";
            case "MOBILE MONEY" -> "Mobile Money";
            default -> normalized.charAt(0) + normalized.substring(1).toLowerCase();
        };
    }

    private Member findMember(Member reference) {
        if (reference == null || reference.getNationalId() == null || reference.getNationalId().isBlank()) {
            throw new ValidationException("Member is required.");
        }
        return memberRepository.findById(reference.getNationalId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found."));
    }
}
