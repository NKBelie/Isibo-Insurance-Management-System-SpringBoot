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

    @GetMapping("/searchAll")
    @ResponseStatus(HttpStatus.OK)

    public List<Member> searchAllPatient(){
        return memberService.findAllMembers();
    }


}
