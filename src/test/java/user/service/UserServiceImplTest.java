package user.service;

import exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import user.domain.User;
import user.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void publicRegistrationAlwaysCreatesAnOfficerAndHashesPassword() {
        User user = new User();
        user.setUsername("officer1");
        user.setEmail("officer@example.com");
        user.setPasswordHash("SecurePass123");
        when(userRepository.existsByUsername("officer1")).thenReturn(false);
        when(userRepository.existsByEmail("officer@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123")).thenReturn("bcrypt-hash");
        when(userRepository.save(user)).thenReturn(user);

        User saved = userService.registerInsuranceOfficerAccount(user);

        assertEquals("Insurance Officer", saved.getRole());
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
}
