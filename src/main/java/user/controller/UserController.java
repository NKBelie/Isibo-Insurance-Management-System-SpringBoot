package user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import user.domain.SystemRole;
import user.domain.User;
import user.service.UserService;
import user.service.UserSessionManager;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserSessionManager userSessionManager;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(user));
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.findAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable UUID id) {
        return userService.findById(id);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable UUID id, @RequestBody User user, Authentication authentication) {
        User existing = userService.findById(id);
        boolean roleChanged = user.getRole() != null && !user.getRole().isBlank()
                && SystemRole.from(user.getRole()) != SystemRole.from(existing.getRole());
        if (roleChanged && existing.getUsername().equals(authentication.getName())) {
            throw new AccessDeniedException("Administrators cannot change their own role.");
        }
        User updated = userService.updateUser(id, user);
        if (roleChanged) {
            userSessionManager.expireSessions(existing.getUsername());
        }
        return updated;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
