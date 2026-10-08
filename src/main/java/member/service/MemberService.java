package member.service;

import member.domain.Member;

import java.util.List;

public interface MemberService {
    Member registerMember(Member member);
    Member updateMember(String nationalId, Member member);
    Member deleteMember(String nationalId);
    Member findMemberById(String nationalId);
    List<Member> findAllMembers();
}
