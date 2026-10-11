package user.service;

import user.domain.User;
import user.dto.CustomerRegistrationForm;
import user.dto.InsuranceApplicationForm;
import member.domain.Member;

import java.util.List;
import java.util.UUID;

public interface UserService {
    User registerUser(User user);
    User registerInsuranceOfficerAccount(User user);
    User registerCustomerAccount(CustomerRegistrationForm registration);
    Member submitCustomerApplication(String username, InsuranceApplicationForm application);
    User findById(UUID id);
    User findByUsername(String username);
    List<User> findAllUsers();
    User updateUser(UUID id, User user);
    void deleteUser(UUID id);
}
