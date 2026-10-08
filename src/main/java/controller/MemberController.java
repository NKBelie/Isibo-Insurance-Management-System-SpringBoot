package controller;

import lombok.RequiredArgsConstructor;
import member.domain.Member;
import member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping({"/api/members", "/member"})
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<Member> createMember(@RequestBody Member member) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberService.registerMember(member));
    }

    @GetMapping
    public List<Member> getAllMembers() {
        return memberService.findAllMembers();
    }

    @GetMapping("/{nationalId}")
    public Member getMemberById(@PathVariable String nationalId) {
        return memberService.findMemberById(nationalId);
    }

    @PutMapping("/{nationalId}")
    public Member updateMember(@PathVariable String nationalId, @RequestBody Member member) {
        return memberService.updateMember(nationalId, member);
    }

    @DeleteMapping("/{nationalId}")
    public ResponseEntity<Void> deleteMember(@PathVariable String nationalId) {
        memberService.deleteMember(nationalId);
        return ResponseEntity.ok().build();
    }
}