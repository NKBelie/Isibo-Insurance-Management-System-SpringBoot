package user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import user.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByMember_NationalId(String nationalId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET role = 'ADMINISTRATOR' "
            + "WHERE UPPER(REPLACE(REPLACE(TRIM(role), ' ', '_'), '-', '_')) IN ('ADMIN', 'ADMINISTRATOR')",
            nativeQuery = true)
    int normalizeAdministratorRoles();

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET role = 'INSURANCE_OFFICER' "
            + "WHERE UPPER(REPLACE(REPLACE(TRIM(role), ' ', '_'), '-', '_')) "
            + "IN ('OFFICER', 'INSURANCE_OFFICER')",
            nativeQuery = true)
    int normalizeInsuranceOfficerRoles();

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET role = 'CUSTOMER' "
            + "WHERE UPPER(TRIM(role)) = 'CUSTOMER'",
            nativeQuery = true)
    int normalizeCustomerRoles();
}
