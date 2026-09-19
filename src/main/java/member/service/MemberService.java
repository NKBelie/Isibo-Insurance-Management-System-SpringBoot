package member.service;

import member.domain.Member;

import java.util.List;

public interface MemberService {
    Member registerMember(Member theMember);
    Member updateMember(Member theMember);
    Member deleteMember(Member theMember);
    Member findMemberById(Member theMember);
    List<Member> findAllMembers();
}
