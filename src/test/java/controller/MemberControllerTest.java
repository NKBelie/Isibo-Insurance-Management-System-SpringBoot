package controller;

import member.domain.Member;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MemberControllerTest {

    @Test
    void memberFieldsShouldBeAssignedCorrectly() {
        Member member = new Member();
        member.setNationalId("1199887766554433");
        member.setMemberId("MEM-001");
        member.setFirstName("Belie");
        member.setLastName("Ndayisaba");
        member.setEmail("belie@example.com");
        member.setPhone("0788000000");
        member.setDateOfBirth(LocalDate.of(2004, 5, 20));
        member.setGender("FEMALE");
        member.setAddress("Kigali, Rwanda");
        member.setStatus("ACTIVE");

        assertNotNull(member);
        assertEquals("1199887766554433", member.getNationalId());
        assertEquals("MEM-001", member.getMemberId());
        assertEquals("belie@example.com", member.getEmail());
        assertEquals("ACTIVE", member.getStatus());
    }
}
