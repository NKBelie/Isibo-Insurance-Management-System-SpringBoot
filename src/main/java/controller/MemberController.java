package controller;

import lombok.RequiredArgsConstructor;
import member.domain.Member;
import member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    // CREATE
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Member registerMember(
            @RequestBody Member member) {

        return memberService.registerMember(member);
    }

    // READ ALL
    @GetMapping("/searchAll")
    @ResponseStatus(HttpStatus.OK)
    public List<Member> searchAllMembers() {

        return memberService.findAllMembers();
    }

    // READ ONE
    @GetMapping("/{nationalId}")
    @ResponseStatus(HttpStatus.OK)
    public Member searchMemberById(
            @PathVariable String nationalId) {

        Member member = new Member();
        member.setNationalId(nationalId);

        return memberService.findMemberById(member);
    }

    // UPDATE
    @PutMapping("/{nationalId}")
    @ResponseStatus(HttpStatus.OK)
    public Member updateMember(
            @PathVariable String nationalId,
            @RequestBody Member member) {

        member.setNationalId(nationalId);

        return memberService.updateMember(member);
    }

    // DELETE
    @DeleteMapping("/{nationalId}")
    @ResponseStatus(HttpStatus.OK)
    public Member deleteMember(
            @PathVariable String nationalId) {

        Member member = new Member();
        member.setNationalId(nationalId);

        return memberService.deleteMember(member);
    }
}