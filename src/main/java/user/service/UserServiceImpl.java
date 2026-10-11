package user.service;

import exception.DuplicateResourceException;
import exception.BusinessRuleException;
import exception.ResourceNotFoundException;
import exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import insurancePlan.domain.InsurancePlan;
import member.domain.Member;
import member.service.MemberService;
import user.domain.User;
import user.domain.SystemRole;
import user.dto.CustomerRegistrationForm;
import user.dto.InsuranceApplicationForm;
import user.repository.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberService memberService;

    @Override
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public User registerUser(User user) {
        validateNewUser(user);

        return saveNewUser(user, SystemRole.from(user.getRole()).name());
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public User registerInsuranceOfficerAccount(User user) {
        validateNewUser(user);
        return saveNewUser(user, SystemRole.INSURANCE_OFFICER.name());
    }

    @Override
    public User registerCustomerAccount(CustomerRegistrationForm registration) {
        if (registration == null) {
            throw new ValidationException("Registration data is required.");
        }

        User user = new User();
        user.setUsername(registration.getUsername() == null ? null : registration.getUsername().trim());
        user.setEmail(registration.getEmail() == null ? null : registration.getEmail().trim());
        user.setPasswordHash(registration.getPassword());
        validateNewUser(user);
        validateUniqueCredentials(user);
        return saveNewUser(user, SystemRole.CUSTOMER.name());
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER') and #username == authentication.name")
    @Transactional
    public Member submitCustomerApplication(String username, InsuranceApplicationForm application) {
        if (application == null) {
            throw new ValidationException("Insurance application data is required.");
        }
        User user = findByUsername(username);
        if (SystemRole.from(user.getRole()) != SystemRole.CUSTOMER) {
            throw new BusinessRuleException("Only customer accounts can submit insurance applications.");
        }
        if (user.getMember() != null) {
            throw new BusinessRuleException("An insurance application has already been submitted for this account.");
        }

        Member member = new Member();
        member.setNationalId(application.getNationalId());
        member.setFirstName(application.getFirstName());
        member.setLastName(application.getLastName());
        member.setEmail(user.getEmail());
        member.setPhone(application.getPhone());
        member.setDateOfBirth(application.getDateOfBirth());
        member.setGender(application.getGender());
        member.setAddress(application.getAddress());
        member.setStatus("PENDING");
        InsurancePlan selectedPlan = new InsurancePlan();
        selectedPlan.setId(application.getInsurancePlanId());
        member.setInsurancePlan(selectedPlan);

        user.setMember(memberService.registerMember(member));
        userRepository.save(user);
        return user.getMember();
    }

    private void validateNewUser(User user) {
        if (user == null) {
            throw new ValidationException("User data is required.");
        }

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new ValidationException("Username is required.");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ValidationException("Email is required.");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new ValidationException("Password is required.");
        }
        if (!EMAIL_PATTERN.matcher(user.getEmail().trim()).matches()) {
            throw new ValidationException("Email must be valid.");
        }
        if (user.getUsername().trim().length() < 3 || user.getUsername().trim().length() > 60) {
            throw new ValidationException("Username must be between 3 and 60 characters.");
        }
        if (user.getPasswordHash().length() < 8) {
            throw new ValidationException("Password must be at least 8 characters.");
        }
    }

    private User saveNewUser(User user, String role) {
        String username = user.getUsername().trim();
        String email = user.getEmail().trim();
        validateUniqueCredentials(user);

        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        return userRepository.save(user);
    }

    private void validateUniqueCredentials(User user) {
        String username = user.getUsername().trim();
        String email = user.getEmail().trim();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists.");
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public User findById(UUID id) {
        if (id == null) {
            throw new ValidationException("User ID is required.");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRATOR') or #username == authentication.name")
    public User findByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username is required.");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public User updateUser(UUID id, User user) {
        User existing = findById(id);

        if (user == null) {
            throw new ValidationException("User data is required.");
        }

        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            String username = user.getUsername().trim();
            if (!username.equals(existing.getUsername()) && userRepository.existsByUsername(username)) {
                throw new DuplicateResourceException("Username already exists.");
            }
            if (username.length() < 3 || username.length() > 60) {
                throw new ValidationException("Username must be between 3 and 60 characters.");
            }
            existing.setUsername(username);
        }

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            String email = user.getEmail().trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new ValidationException("Email must be valid.");
            }
            if (!email.equalsIgnoreCase(existing.getEmail()) && userRepository.existsByEmail(email)) {
                throw new DuplicateResourceException("Email already exists.");
            }
            existing.setEmail(email);
        }

        if (user.getRole() != null && !user.getRole().isBlank()) {
            existing.setRole(SystemRole.from(user.getRole()).name());
        }

        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            if (user.getPasswordHash().length() < 8) {
                throw new ValidationException("Password must be at least 8 characters.");
            }
            existing.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }

        return userRepository.save(existing);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void deleteUser(UUID id) {
        User existing = findById(id);
        if (SystemRole.from(existing.getRole()) == SystemRole.CUSTOMER) {
            throw new BusinessRuleException("Customer accounts are linked to insurance applications and cannot be deleted here.");
        }
        userRepository.delete(existing);
    }
}
