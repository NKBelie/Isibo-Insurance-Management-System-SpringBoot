package rw.ac.auca.isiboinsurancespringboot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import insurancePlan.domain.InsurancePlan;
import insurancePlan.repository.InsurancePlanRepository;
import coverage.domain.Coverage;
import coverage.repository.CoverageRepository;
import member.domain.Member;
import member.repository.MemberRepository;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import user.domain.User;
import user.repository.UserRepository;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
class IsiboInsuranceSpringBootApplicationTests {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InsurancePlanRepository insurancePlanRepository;

    @Autowired
    private CoverageRepository coverageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void databaseSchemaContainsAllEightDomainTables() {
        List<String> tableNames = jdbcTemplate.queryForList(
                "SELECT LOWER(TABLE_NAME) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'",
                String.class);

        assertTrue(tableNames.containsAll(List.of(
                "members", "insurance_plans", "coverages", "healthcare_providers",
                "insurance_officers", "claims", "payments", "users")));
    }

    @Test
    void registrationPageIsPublic() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create your ISIBO account")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("insurancePlanId"))));
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Healthcare coverage, managed with confidence.")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void customerRegistrationRequiresCsrfAndPersistsHashedCustomerAccount() throws Exception {
        String username = "public-" + java.util.UUID.randomUUID();
        String email = username + "@example.com";

        mockMvc.perform(post("/register")
                        .param("username", username)
                        .param("email", email)
                        .param("password", "SecurePass123")
                        .param("passwordConfirmation", "SecurePass123")
                        .param("role", "ADMINISTRATOR"))
                .andExpect(status().isForbidden());
        assertTrue(userRepository.findByUsername(username).isEmpty());

        mockMvc.perform(post("/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("username", username)
                        .param("email", email)
                        .param("password", "SecurePass123")
                        .param("passwordConfirmation", "SecurePass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        User registered = userRepository.findByUsername(username).orElseThrow();
        assertEquals("CUSTOMER", registered.getRole());
        assertTrue(!"SecurePass123".equals(registered.getPasswordHash()));
        assertTrue(registered.getMember() == null);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRATOR")
    void administratorCanOpenDashboardAndUserManagement() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("System Users")));
        mockMvc.perform(get("/users")).andExpect(status().isOk());
        mockMvc.perform(get("/users/new")).andExpect(status().isOk());
        mockMvc.perform(get("/members")).andExpect(status().isOk());
        mockMvc.perform(get("/insurance-plans")).andExpect(status().isOk());
        mockMvc.perform(get("/coverages")).andExpect(status().isOk());
        mockMvc.perform(get("/claims")).andExpect(status().isOk());
        mockMvc.perform(get("/payments")).andExpect(status().isOk());
        mockMvc.perform(get("/providers")).andExpect(status().isOk());
        mockMvc.perform(get("/officers")).andExpect(status().isOk());
        mockMvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Members by status")))
                .andExpect(content().string(containsString("Latest recorded payments")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRATOR")
    void administratorCanCreateAccountsWithEachCanonicalRole() throws Exception {
        for (String role : List.of("ADMINISTRATOR", "INSURANCE_OFFICER", "CUSTOMER")) {
            String username = role.toLowerCase() + "-" + java.util.UUID.randomUUID();
            mockMvc.perform(post("/users")
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .param("username", username)
                            .param("email", username + "@example.com")
                            .param("passwordHash", "SecurePass123")
                            .param("passwordConfirmation", "SecurePass123")
                            .param("role", role))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/users"));

            User created = userRepository.findByUsername(username).orElseThrow();
            assertEquals(role, created.getRole());
            assertTrue(created.getPasswordHash().startsWith("$2"));
        }
    }

    @Test
    @WithMockUser(username = "role-admin", roles = "ADMINISTRATOR")
    void administratorCanChangeAnotherUsersRoleButCannotPromoteOrDemoteThemselves() throws Exception {
        User administrator = new User();
        administrator.setUsername("role-admin");
        administrator.setEmail("role-admin@example.com");
        administrator.setPasswordHash("not-used-in-mock-auth");
        administrator.setRole("ADMINISTRATOR");
        userRepository.save(administrator);

        User customer = new User();
        customer.setUsername("role-target-" + java.util.UUID.randomUUID());
        customer.setEmail("role-target-" + java.util.UUID.randomUUID() + "@example.com");
        customer.setPasswordHash("not-used-in-mock-auth");
        customer.setRole("CUSTOMER");
        userRepository.save(customer);

        mockMvc.perform(put("/api/users/" + customer.getId())
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"INSURANCE_OFFICER\"}"))
                .andExpect(status().isOk());
        assertEquals("INSURANCE_OFFICER",
                userRepository.findById(customer.getId()).orElseThrow().getRole());

        mockMvc.perform(put("/api/users/" + administrator.getId())
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isForbidden());
        assertEquals("ADMINISTRATOR",
                userRepository.findById(administrator.getId()).orElseThrow().getRole());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRATOR")
    void insurancePlanCreatePageAndValidationKeepSubmittedValues() throws Exception {
        mockMvc.perform(get("/insurance-plans/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create Insurance Plan")))
                .andExpect(content().string(not(containsString("name=\"insurancePlanId\""))))
                .andExpect(content().string(containsString("name=\"_csrf\"")));

        mockMvc.perform(post("/insurance-plans")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("insurancePlanName", "Submitted Plan Name")
                        .param("description", "Submitted description")
                        .param("monthlyPremium", "0")
                        .param("coverageLimit", "500000")
                        .param("durationMonths", "12")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Monthly premium must be greater than zero.")))
                .andExpect(content().string(containsString("Submitted Plan Name")))
                .andExpect(content().string(containsString("Submitted description")));
        assertTrue(insurancePlanRepository.findAll().stream()
                .noneMatch(plan -> "Submitted Plan Name".equals(plan.getInsurancePlanName())));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRATOR")
    void insurancePlanWorkflowSupportsCreateSearchEditDetailsAndDelete() throws Exception {
        mockMvc.perform(post("/insurance-plans")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("insurancePlanId", "PLN-999999")
                        .param("insurancePlanName", "Workflow Health Plan")
                        .param("description", "Initial plan description")
                        .param("monthlyPremium", "15000")
                        .param("coverageLimit", "750000")
                        .param("durationMonths", "12")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insurance-plans"));

        InsurancePlan created = insurancePlanRepository.findAll().stream()
                .filter(plan -> "Workflow Health Plan".equals(plan.getInsurancePlanName()))
                .findFirst().orElseThrow();
        assertTrue(created.getInsurancePlanId().matches("PLN-[0-9]{3,}"));
        mockMvc.perform(get("/insurance-plans")
                        .param("search", created.getInsurancePlanId())
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(created.getInsurancePlanId())))
                .andExpect(content().string(containsString("Workflow Health Plan")));

        mockMvc.perform(get("/insurance-plans/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Members on this plan")))
                .andExpect(content().string(containsString("Coverage included")))
                .andExpect(content().string(containsString("No members enrolled yet.")))
                .andExpect(content().string(containsString("Edit plan")));

        mockMvc.perform(post("/insurance-plans/{id}/update", created.getId())
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("insurancePlanName", "Updated Workflow Plan")
                        .param("description", "Updated plan description")
                        .param("monthlyPremium", "17500")
                        .param("coverageLimit", "800000")
                        .param("durationMonths", "18")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insurance-plans/" + created.getId()));

        InsurancePlan updated = insurancePlanRepository.findById(created.getId()).orElseThrow();
        assertTrue(updated.getInsurancePlanName().equals("Updated Workflow Plan"));
        assertTrue(updated.getMonthlyPremium() == 17500);

        mockMvc.perform(post("/insurance-plans/{id}/delete", created.getId())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insurance-plans"));
        assertTrue(insurancePlanRepository.findById(created.getId()).isEmpty());
    }

    @Test
    @WithMockUser(username = "application-page-customer", roles = "CUSTOMER")
    void customerApplicationPageShowsActivePlansAndAccountInformation() throws Exception {
        User customer = new User();
        customer.setUsername("application-page-customer");
        customer.setEmail("application-page-customer@example.com");
        customer.setPasswordHash("test-password-hash");
        customer.setRole("CUSTOMER");
        userRepository.save(customer);

        InsurancePlan plan = new InsurancePlan();
        plan.setInsurancePlanId("PLAN-APPLICATION-PAGE");
        plan.setInsurancePlanName("Application Page Health Plan");
        plan.setDescription("Active test plan for the customer application screen.");
        plan.setMonthlyPremium(15000);
        plan.setCoverageLimit(750000);
        plan.setDurationMonths(12);
        plan.setStatus("ACTIVE");
        insurancePlanRepository.save(plan);

        mockMvc.perform(get("/insurance-application"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Apply for insurance")))
                .andExpect(content().string(containsString("Applicant details")))
                .andExpect(content().string(containsString("Application Page Health Plan")))
                .andExpect(content().string(containsString("15,000 RWF/month")))
                .andExpect(content().string(containsString("application-page-customer@example.com")))
                .andExpect(content().string(containsString("What happens next")))
                .andExpect(content().string(not(containsString("No active plans are available"))));
    }

    @Test
    @WithMockUser(username = "officer", roles = "INSURANCE_OFFICER")
    void officerGetsRoleSpecificDashboardAndOnlyAuthorizedManagementActions() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/dashboard"));
        mockMvc.perform(get("/officer/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Insurance Officer workspace")));
        mockMvc.perform(get("/admin/dashboard")).andExpect(status().isForbidden());
        mockMvc.perform(get("/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/reports")).andExpect(status().isForbidden());
        mockMvc.perform(get("/access-denied"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Access denied")));
        mockMvc.perform(get("/users/new")).andExpect(status().isForbidden());
        mockMvc.perform(get("/members/new")).andExpect(status().isOk());
        mockMvc.perform(get("/claims/new")).andExpect(status().isForbidden());
        mockMvc.perform(get("/payments/new")).andExpect(status().isForbidden());
        mockMvc.perform(get("/members"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Register Member")));
        mockMvc.perform(get("/insurance-plans"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Create Insurance Plan"))));
        mockMvc.perform(get("/coverages"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Add Coverage"))));
        mockMvc.perform(get("/api/members")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "member-officer", roles = "INSURANCE_OFFICER")
    void officerCanCreateAndEditMembersButCannotDeleteThem() throws Exception {
        InsurancePlan plan = new InsurancePlan();
        plan.setInsurancePlanId("OFFICER-PLAN-" + java.util.UUID.randomUUID());
        plan.setInsurancePlanName("Officer Member Plan " + java.util.UUID.randomUUID());
        plan.setDescription("Plan for officer member management test");
        plan.setMonthlyPremium(15000);
        plan.setCoverageLimit(750000);
        plan.setDurationMonths(12);
        plan.setStatus("ACTIVE");
        insurancePlanRepository.save(plan);

        String nationalId = String.format("%016d",
                Math.floorMod(System.nanoTime(), 10_000_000_000_000_000L));
        String email = "officer-member-" + java.util.UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/members")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("nationalId", nationalId)
                        .param("firstName", "Officer")
                        .param("lastName", "Managed Member")
                        .param("email", email)
                        .param("phone", "078" + nationalId.substring(3, 10))
                        .param("dateOfBirth", "1990-01-01")
                        .param("gender", "OTHER")
                        .param("address", "Kigali")
                        .param("status", "ACTIVE")
                        .param("insurancePlan.id", plan.getId().toString()))
                .andExpect(status().is3xxRedirection());

        assertTrue(memberRepository.existsById(nationalId));
        mockMvc.perform(get("/members/{nationalId}", nationalId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/members/" + nationalId + "/edit")))
                .andExpect(content().string(not(containsString("Delete Member?"))));
        mockMvc.perform(get("/members/{nationalId}/edit", nationalId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit Member")));
        mockMvc.perform(post("/members/{nationalId}/update", nationalId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("nationalId", nationalId)
                        .param("firstName", "Officer")
                        .param("lastName", "Updated Member")
                        .param("email", email)
                        .param("phone", "078" + nationalId.substring(3, 10))
                        .param("dateOfBirth", "1990-01-01")
                        .param("gender", "OTHER")
                        .param("address", "Updated address")
                        .param("status", "ACTIVE")
                        .param("insurancePlan.id", plan.getId().toString()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/members/{nationalId}/delete", nationalId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
        assertTrue(memberRepository.existsById(nationalId));

        String apiNationalId = String.format("%016d",
                Math.floorMod(System.nanoTime() + 1, 10_000_000_000_000_000L));
        String apiEmail = "officer-api-member-" + java.util.UUID.randomUUID() + "@example.com";
        String memberJson = """
                {
                  "nationalId": "%s",
                  "firstName": "Officer",
                  "lastName": "API Member",
                  "email": "%s",
                  "phone": "079%s",
                  "dateOfBirth": "1991-01-01",
                  "gender": "OTHER",
                  "address": "Kigali",
                  "status": "ACTIVE",
                  "insurancePlan": {
                    "id": "%s",
                    "insurancePlanId": "%s",
                    "insurancePlanName": "%s",
                    "description": "Officer member management test",
                    "monthlyPremium": 15000,
                    "coverageLimit": 750000,
                    "durationMonths": 12,
                    "status": "ACTIVE"
                  }
                }
                """.formatted(apiNationalId, apiEmail, apiNationalId.substring(3, 10), plan.getId(),
                plan.getInsurancePlanId(), plan.getInsurancePlanName());

        mockMvc.perform(post("/api/members")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/members/{nationalId}", apiNationalId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"Updated API address\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/members/{nationalId}", apiNationalId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void customerDashboardOnlyShowsTheirOwnApplication() throws Exception {
        InsurancePlan plan = new InsurancePlan();
        plan.setInsurancePlanId("PLAN-CUSTOMER-TEST");
        plan.setInsurancePlanName("Customer Test Plan");
        plan.setDescription("Test plan");
        plan.setMonthlyPremium(10000);
        plan.setCoverageLimit(500000);
        plan.setDurationMonths(12);
        plan.setStatus("ACTIVE");
        insurancePlanRepository.save(plan);
        Coverage coverage = new Coverage();
        coverage.setCoverageId("COV-CUSTOMER-TEST");
        coverage.setDescription("Customer dashboard test coverage");
        coverage.setCoverageAmount(250000);
        coverage.setInsurancePlan(plan);
        coverageRepository.save(coverage);

        Member member = new Member();
        member.setNationalId("CUSTOMER-TEST-NID");
        member.setMemberId("MEM-CUSTOMER-TEST");
        member.setFirstName("Private");
        member.setLastName("Customer");
        member.setEmail("customer-dashboard@example.com");
        member.setPhone("0788111222");
        member.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));
        member.setGender("OTHER");
        member.setAddress("Kigali");
        member.setStatus("PENDING");
        member.setInsurancePlan(plan);
        memberRepository.save(member);

        Member otherMember = new Member();
        otherMember.setNationalId("OTHER-TEST-NID");
        otherMember.setMemberId("MEM-OTHER-TEST");
        otherMember.setFirstName("Other");
        otherMember.setLastName("Person");
        otherMember.setEmail("other-dashboard@example.com");
        otherMember.setPhone("0788333444");
        otherMember.setDateOfBirth(java.time.LocalDate.of(1991, 1, 1));
        otherMember.setGender("OTHER");
        otherMember.setAddress("Kigali");
        otherMember.setStatus("ACTIVE");
        otherMember.setInsurancePlan(plan);
        memberRepository.save(otherMember);

        User customer = new User();
        customer.setUsername("customer");
        customer.setEmail("customer-dashboard@example.com");
        customer.setPasswordHash("not-used-in-mock-auth");
        customer.setRole("CUSTOMER");
        customer.setMember(member);
        userRepository.save(customer);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/dashboard"));
        mockMvc.perform(get("/customer/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Private Customer")))
                .andExpect(content().string(containsString("Customer Test Plan")))
                .andExpect(content().string(containsString("Customer dashboard test coverage")))
                .andExpect(content().string(containsString("MEM-CUSTOMER-TEST")))
                .andExpect(content().string(not(containsString("Other Person"))))
                .andExpect(content().string(not(containsString("System Users"))));

        mockMvc.perform(get("/admin/dashboard")).andExpect(status().isForbidden());
        mockMvc.perform(get("/officer/dashboard")).andExpect(status().isForbidden());
        mockMvc.perform(get("/members")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/members")).andExpect(status().isForbidden());
        mockMvc.perform(get("/claims")).andExpect(status().isForbidden());
        mockMvc.perform(get("/payments")).andExpect(status().isForbidden());
        mockMvc.perform(get("/reports")).andExpect(status().isForbidden());
    }
}
