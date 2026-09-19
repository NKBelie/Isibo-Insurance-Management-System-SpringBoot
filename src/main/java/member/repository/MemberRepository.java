package member.repository;

import member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository
        extends JpaRepository<Member, String> {

    Optional<Member> findByMemberId(String memberId);

    Optional<Member> findByEmail(String email);

    boolean existsByMemberId(String memberId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}