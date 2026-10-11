package user.service;

import exception.ValidationException;
import member.domain.Member;
import member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import user.domain.User;
import user.dto.CustomerRegistrationForm;
import user.dto.InsuranceApplicationForm;
import user.repository.UserRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MemberService memberService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void officerAccountCreationAssignsCanonicalRoleAndHashesPassword() {
        User user = new User();
        user.setUsername("officer1");
        user.setEmail("officer@example.com");
        user.setPasswordHash("SecurePass123");
        when(userRepository.existsByUsername("officer1")).thenReturn(false);
        when(userRepository.existsByEmail("officer@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123")).thenReturn("bcrypt-hash");
        when(userRepository.save(user)).thenReturn(user);

        User saved = userService.registerInsuranceOfficerAccount(user);

        assertEquals("INSURANCE_OFFICER", saved.getRole());
        assertEquals("bcrypt-hash", saved.getPasswordHash());
        assertNotEquals("SecurePass123", saved.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    void publicRegistrationRejectsShortPassword() {
        User user = new User();
        user.setUsername("officer1");
        user.setEmail("officer@example.com");
        user.setPasswordHash("short");

        assertThrows(ValidationException.class, () -> userService.registerInsuranceOfficerAccount(user));
    }

    @Test
    void customerRegistrationCreatesOnlyAccountAndHashesPassword() {
        CustomerRegistrationForm registration = new CustomerRegistrationForm();
        registration.setUsername("customer1");
        registration.setEmail(" customer@example.com ");
        registration.setPassword("SecurePass123");

        when(userRepository.existsByUsername("customer1")).thenReturn(false);
        when(userRepository.existsByEmail("customer@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123")).thenReturn("bcrypt-hash");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.registerCustomerAccount(registration);

        assertEquals("CUSTOMER", saved.getRole());
        assertEquals("bcrypt-hash", saved.getPasswordHash());
        assertNull(saved.getMember());
        assertEquals("customer@example.com", saved.getEmail());
        verify(userRepository).save(saved);
    }

    @Test
    void customerCanSubmitInsuranceApplicationAfterSigningIn() {
        User customer = new User();
        customer.setUsername("customer1");
        customer.setEmail("customer@example.com");
        customer.setRole("CUSTOMER");

        InsuranceApplicationForm application = new InsuranceApplicationForm();
        application.setNationalId("1199887766554433");
        application.setFirstName("Customer");
        application.setLastName("One");
        application.setPhone("0788000000");
        application.setDateOfBirth(LocalDate.of(1990, 1, 1));
        application.setGender("OTHER");
        application.setAddress("Kigali");
        application.setInsurancePlanId(java.util.UUID.randomUUID());

        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(customer));
        when(memberService.registerMember(org.mockito.ArgumentMatchers.any(Member.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(customer)).thenReturn(customer);

        Member saved = userService.submitCustomerApplication("customer1", application);

        assertEquals("PENDING", saved.getStatus());
        assertEquals("customer@example.com", saved.getEmail());
        assertEquals(saved, customer.getMember());
        verify(memberService).registerMember(org.mockito.ArgumentMatchers.any(Member.class));
        verify(userRepository).save(customer);
    }
}
