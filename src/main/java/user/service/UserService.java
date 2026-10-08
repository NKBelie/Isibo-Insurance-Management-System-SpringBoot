package user.service;

import user.domain.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
    User registerUser(User user);
    User registerInsuranceOfficerAccount(User user);
    User findById(UUID id);
    User findByUsername(String username);
    List<User> findAllUsers();
    User updateUser(UUID id, User user);
    void deleteUser(UUID id);
}
