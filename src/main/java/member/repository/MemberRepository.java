package member.repository;

import member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, String> {

    Optional<Member> findByMemberId(String memberId);

    Optional<Member> findByEmailIgnoreCase(String email);

    Optional<Member> findByPhone(String phone);

    List<Member> findByInsurancePlan_Id(UUID insurancePlanId);

    boolean existsByMemberId(String memberId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);
}