package user.service;

import exception.DuplicateResourceException;
import exception.ResourceNotFoundException;
import exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import user.domain.User;
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

    @Override
    public User registerUser(User user) {
        validateNewUser(user);

        String normalizedRole = user.getRole().trim();
        if (!("Administrator".equalsIgnoreCase(normalizedRole) || "Insurance Officer".equalsIgnoreCase(normalizedRole))) {
            throw new ValidationException("Role must be Administrator or Insurance Officer.");
        }

        return saveNewUser(user, normalizedRole);
    }

    @Override
    public User registerInsuranceOfficerAccount(User user) {
        validateNewUser(user);
        return saveNewUser(user, "Insurance Officer");
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
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("Insurance Officer");
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
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists.");
        }

        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        return userRepository.save(user);
    }

    @Override
    public User findById(UUID id) {
        if (id == null) {
            throw new ValidationException("User ID is required.");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    @Override
    public User findByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username is required.");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User updateUser(UUID id, User user) {
        User existing = findById(id);

        if (user == null) {
            throw new ValidationException("User data is required.");
        }

        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            if (!user.getUsername().equals(existing.getUsername()) && userRepository.existsByUsername(user.getUsername())) {
                throw new DuplicateResourceException("Username already exists.");
            }
            existing.setUsername(user.getUsername());
        }

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            if (!user.getEmail().equalsIgnoreCase(existing.getEmail()) && userRepository.existsByEmail(user.getEmail())) {
                throw new DuplicateResourceException("Email already exists.");
            }
            existing.setEmail(user.getEmail());
        }

        if (user.getRole() != null && !user.getRole().isBlank()) {
            String normalizedRole = user.getRole().trim();
            if (!("Administrator".equalsIgnoreCase(normalizedRole) || "Insurance Officer".equalsIgnoreCase(normalizedRole))) {
                throw new ValidationException("Role must be Administrator or Insurance Officer.");
            }
            existing.setRole(normalizedRole);
        }

        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            existing.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }

        return userRepository.save(existing);
    }

    @Override
    public void deleteUser(UUID id) {
        User existing = findById(id);
        userRepository.delete(existing);
    }
}
