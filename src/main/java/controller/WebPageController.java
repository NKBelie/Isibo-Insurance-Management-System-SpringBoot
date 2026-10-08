package controller;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import claim.domain.Claim;
import claim.service.ClaimService;
import exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import coverage.domain.Coverage;
import coverage.service.CoverageService;
import healthcareProvider.domain.HealthcareProvider;
import healthcareProvider.service.HealthcareProviderService;
import insuranceOfficer.domain.InsuranceOfficer;
import insuranceOfficer.service.InsuranceOfficerService;
import insurancePlan.domain.InsurancePlan;
import insurancePlan.service.InsuranceService;
import member.domain.Member;
import member.service.MemberService;
import java.time.LocalDate;
import payment.domain.Payment;
import payment.service.PaymentService;
import user.domain.User;
import user.service.UserService;

@Controller
public class WebPageController {

    private final MemberService memberService;
    private final InsuranceService insuranceService;
    private final CoverageService coverageService;
    private final HealthcareProviderService providerService;
    private final ClaimService claimService;
    private final InsuranceOfficerService officerService;
    private final PaymentService paymentService;
    private final UserService userService;

    public WebPageController(MemberService memberService,
                            InsuranceService insuranceService,
                            CoverageService coverageService,
                            HealthcareProviderService providerService,
                            ClaimService claimService,
                            InsuranceOfficerService officerService,
                            PaymentService paymentService,
                            UserService userService) {
        this.memberService = memberService;
        this.insuranceService = insuranceService;
        this.coverageService = coverageService;
        this.providerService = providerService;
        this.claimService = claimService;
        this.officerService = officerService;
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @ModelAttribute("requestURI")
    public String requestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @GetMapping({"/", "/index", "/home"})
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Member> members = memberService.findAllMembers();
        List<InsurancePlan> plans = insuranceService.findAllInsurancePlans();
        List<Coverage> coverages = coverageService.findAllCoverage();
        List<Claim> claims = claimService.findAll();
        List<Payment> payments = paymentService.findAll();
        List<HealthcareProvider> providers = providerService.findAll();
        List<InsuranceOfficer> officers = officerService.findAll();

        Map<UUID, Double> planCoverageUsed = coverages.stream()
                .filter(c -> c.getInsurancePlan() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getInsurancePlan().getId(),
                        Collectors.summingDouble(Coverage::getCoverageAmount)
                ));

        List<Map<String, Object>> coverageSummary = plans.stream()
                .sorted(Comparator.comparing(InsurancePlan::getInsurancePlanName))
                .map(plan -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("plan", plan);
                    item.put("used", planCoverageUsed.getOrDefault(plan.getId(), 0.0));
                    item.put("remaining", Math.max(plan.getCoverageLimit() - planCoverageUsed.getOrDefault(plan.getId(), 0.0), 0.0));
                    return item;
                })
                .toList();

        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("totalMembers", members.size());
        model.addAttribute("activeMembers", members.stream().filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus())).count());
        model.addAttribute("totalPlans", plans.size());
        model.addAttribute("activePlans", plans.stream().filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus())).count());
        model.addAttribute("totalCoverageRecords", coverages.size());
        model.addAttribute("totalClaims", claims.size());
        model.addAttribute("pendingClaims", claims.stream().filter(c -> "PENDING".equalsIgnoreCase(c.getStatus())).count());
        model.addAttribute("approvedClaims", claims.stream().filter(c -> "APPROVED".equalsIgnoreCase(c.getStatus())).count());
        model.addAttribute("totalPayments", payments.size());
        model.addAttribute("totalPaymentAmount", payments.stream().mapToDouble(Payment::getAmount).sum());
        model.addAttribute("totalProviders", providers.size());
        model.addAttribute("totalOfficers", officers.size());
        model.addAttribute("recentClaims", claims.stream().sorted(Comparator.comparing(Claim::getClaimDate).reversed()).limit(5).toList());
        model.addAttribute("recentPayments", payments.stream().sorted(Comparator.comparing(Payment::getPaymentDate).reversed()).limit(5).toList());
        model.addAttribute("recentMembers", members.stream().sorted(Comparator.comparing(Member::getMemberId).reversed()).limit(5).toList());
        model.addAttribute("recentPlans", plans.stream().sorted(Comparator.comparing(InsurancePlan::getInsurancePlanName)).limit(5).toList());
        model.addAttribute("coverageSummary", coverageSummary);
        model.addAttribute("members", members);
        model.addAttribute("plans", plans);
        model.addAttribute("coverages", coverages);
        return "dashboard";
    }

    @GetMapping("/members")
    public String listMembers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Model model) {

        List<Member> members = memberService.findAllMembers();
        List<Member> filtered = filterMembers(members, search, status);

        model.addAttribute("pageTitle", "Members");
        model.addAttribute("members", filtered);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("statuses", List.of("ACTIVE", "INACTIVE", "EXPIRED"));
        return "member/list";
    }

    @GetMapping("/members/new")
    public String newMemberForm(Model model) {
        List<InsurancePlan> activePlans = insuranceService.findAllInsurancePlans().stream()
                .filter(plan -> "ACTIVE".equalsIgnoreCase(plan.getStatus()))
                .toList();

        model.addAttribute("pageTitle", "Register Member");
        model.addAttribute("member", new Member());
        model.addAttribute("insurancePlans", activePlans);
        return "member/form";
    }

    @PostMapping("/members")
    public String createMember(@ModelAttribute("member") Member member,
                               RedirectAttributes redirectAttributes) {
        try {
            memberService.registerMember(member);
            redirectAttributes.addFlashAttribute("successMessage", "Member registered successfully.");
            return "redirect:/members";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/members/new";
        }
    }

    @GetMapping("/members/{nationalId}")
    public String memberDetails(@PathVariable String nationalId, Model model) {
        Member member = memberService.findMemberById(nationalId);
        model.addAttribute("pageTitle", "Member Details");
        model.addAttribute("member", member);
        return "member/details";
    }

    @GetMapping("/members/{nationalId}/edit")
    public String editMemberForm(@PathVariable String nationalId, Model model) {
        Member member = memberService.findMemberById(nationalId);
        List<InsurancePlan> activePlans = insuranceService.findAllInsurancePlans().stream()
                .filter(plan -> "ACTIVE".equalsIgnoreCase(plan.getStatus()))
                .toList();

        model.addAttribute("pageTitle", "Edit Member");
        model.addAttribute("member", member);
        model.addAttribute("insurancePlans", activePlans);
        return "member/form";
    }

    @PostMapping("/members/{nationalId}/update")
    public String updateMember(@PathVariable String nationalId,
                              @ModelAttribute("member") Member member,
                              RedirectAttributes redirectAttributes) {
        try {
            memberService.updateMember(nationalId, member);
            redirectAttributes.addFlashAttribute("successMessage", "Member updated successfully.");
            return "redirect:/members/" + nationalId;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/members/" + nationalId + "/edit";
        }
    }

    @PostMapping("/members/{nationalId}/delete")
    public String deleteMember(@PathVariable String nationalId,
                              RedirectAttributes redirectAttributes) {
        try {
            memberService.deleteMember(nationalId);
            redirectAttributes.addFlashAttribute("successMessage", "Member deleted successfully.");
            return "redirect:/members";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/members";
        }
    }

    @GetMapping("/insurance-plans")
    public String listInsurancePlans(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Model model) {

        List<InsurancePlan> plans = insuranceService.findAllInsurancePlans();
        List<InsurancePlan> filtered = filterPlans(plans, search, status);

        model.addAttribute("pageTitle", "Insurance Plans");
        model.addAttribute("insurancePlans", filtered);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("statuses", List.of("ACTIVE", "INACTIVE"));
        return "insurancePlan/list";
    }

    @GetMapping("/insurance-plans/new")
    public String newInsurancePlanForm(Model model) {
        model.addAttribute("pageTitle", "Create Insurance Plan");
        model.addAttribute("insurancePlan", new InsurancePlan());
        return "insurancePlan/form";
    }

    @PostMapping("/insurance-plans")
    public String createInsurancePlan(@ModelAttribute("insurancePlan") InsurancePlan insurancePlan,
                                     RedirectAttributes redirectAttributes) {
        try {
            insuranceService.registerInsurance(insurancePlan);
            redirectAttributes.addFlashAttribute("successMessage", "Insurance plan created successfully.");
            return "redirect:/insurance-plans";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/insurance-plans/new";
        }
    }

    @GetMapping("/insurance-plans/{id}")
    public String insurancePlanDetails(@PathVariable UUID id, Model model) {
        InsurancePlan insurancePlan = insuranceService.findInsuranceById(id);
        List<Member> members = insurancePlan.getMembers();
        List<Coverage> coverages = insurancePlan.getCoverages();

        model.addAttribute("pageTitle", "Insurance Plan Details");
        model.addAttribute("insurancePlan", insurancePlan);
        model.addAttribute("members", members);
        model.addAttribute("coverages", coverages);
        return "insurancePlan/details";
    }

    @GetMapping("/insurance-plans/{id}/edit")
    public String editInsurancePlanForm(@PathVariable UUID id, Model model) {
        InsurancePlan insurancePlan = insuranceService.findInsuranceById(id);
        model.addAttribute("pageTitle", "Edit Insurance Plan");
        model.addAttribute("insurancePlan", insurancePlan);
        return "insurancePlan/form";
    }

    @PostMapping("/insurance-plans/{id}/update")
    public String updateInsurancePlan(@PathVariable UUID id,
                                     @ModelAttribute("insurancePlan") InsurancePlan insurancePlan,
                                     RedirectAttributes redirectAttributes) {
        try {
            insuranceService.updateInsurance(id, insurancePlan);
            redirectAttributes.addFlashAttribute("successMessage", "Insurance plan updated successfully.");
            return "redirect:/insurance-plans/" + id;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/insurance-plans/" + id + "/edit";
        }
    }

    @PostMapping("/insurance-plans/{id}/delete")
    public String deleteInsurancePlan(@PathVariable UUID id,
                                     RedirectAttributes redirectAttributes) {
        try {
            insuranceService.deleteInsurance(id);
            redirectAttributes.addFlashAttribute("successMessage", "Insurance plan deleted successfully.");
            return "redirect:/insurance-plans";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/insurance-plans";
        }
    }

    @GetMapping("/coverages")
    public String listCoverages(
            @RequestParam(required = false) String search,
            Model model) {

        List<Coverage> coverages = coverageService.findAllCoverage();
        List<Coverage> filtered = filterCoverages(coverages, search);

        model.addAttribute("pageTitle", "Coverage");
        model.addAttribute("coverages", filtered);
        model.addAttribute("search", search);
        return "coverage/list";
    }

    @GetMapping("/coverages/new")
    public String newCoverageForm(Model model) {
        List<InsurancePlan> activePlans = insuranceService.findAllInsurancePlans().stream()
                .filter(plan -> "ACTIVE".equalsIgnoreCase(plan.getStatus()))
                .toList();

        Map<UUID, Double> existingCoverageByPlan = new LinkedHashMap<>();
        for (InsurancePlan plan : activePlans) {
            double used = coverageService.findAllCoverage().stream()
                    .filter(c -> c.getInsurancePlan() != null && c.getInsurancePlan().getId().equals(plan.getId()))
                    .mapToDouble(Coverage::getCoverageAmount)
                    .sum();
            existingCoverageByPlan.put(plan.getId(), used);
        }

        model.addAttribute("pageTitle", "Add Coverage");
        model.addAttribute("coverage", new Coverage());
        model.addAttribute("insurancePlans", activePlans);
        model.addAttribute("existingCoverageByPlan", existingCoverageByPlan);
        return "coverage/form";
    }

    @PostMapping("/coverages")
    public String createCoverage(@ModelAttribute("coverage") Coverage coverage,
                                RedirectAttributes redirectAttributes) {
        try {
            coverageService.registerCoverage(coverage);
            redirectAttributes.addFlashAttribute("successMessage", "Coverage added successfully.");
            return "redirect:/coverages";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/coverages/new";
        }
    }

    @GetMapping("/coverages/{id}")
    public String coverageDetails(@PathVariable UUID id, Model model) {
        Coverage coverage = coverageService.findCoverageById(id);
        model.addAttribute("pageTitle", "Coverage Details");
        model.addAttribute("coverage", coverage);
        return "coverage/details";
    }

    @GetMapping("/coverages/{id}/edit")
    public String editCoverageForm(@PathVariable UUID id, Model model) {
        Coverage coverage = coverageService.findCoverageById(id);
        List<InsurancePlan> activePlans = insuranceService.findAllInsurancePlans().stream()
                .filter(plan -> "ACTIVE".equalsIgnoreCase(plan.getStatus()))
                .toList();

        model.addAttribute("pageTitle", "Edit Coverage");
        model.addAttribute("coverage", coverage);
        model.addAttribute("insurancePlans", activePlans);
        return "coverage/form";
    }

    @PostMapping("/coverages/{id}/update")
    public String updateCoverage(@PathVariable UUID id,
                                @ModelAttribute("coverage") Coverage coverage,
                                RedirectAttributes redirectAttributes) {
        try {
            coverageService.updateCoverage(id, coverage);
            redirectAttributes.addFlashAttribute("successMessage", "Coverage updated successfully.");
            return "redirect:/coverages/" + id;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/coverages/" + id + "/edit";
        }
    }

    @PostMapping("/coverages/{id}/delete")
    public String deleteCoverage(@PathVariable UUID id,
                                RedirectAttributes redirectAttributes) {
        try {
            coverageService.deleteCoverage(id);
            redirectAttributes.addFlashAttribute("successMessage", "Coverage deleted successfully.");
            return "redirect:/coverages";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/coverages";
        }
    }

    @GetMapping("/providers")
    public String listProviders(@RequestParam(required = false) String search,
                                @RequestParam(required = false) String specialty,
                                Model model) {
        List<HealthcareProvider> providers = providerService.findAll();
        String keyword = search == null ? "" : search.trim().toLowerCase();
        String selectedSpecialty = specialty == null ? "" : specialty.trim();
        List<HealthcareProvider> filtered = providers.stream()
                .filter(provider -> selectedSpecialty.isEmpty()
                        || selectedSpecialty.equalsIgnoreCase(provider.getSpecialty()))
                .filter(provider -> keyword.isEmpty()
                        || provider.getProviderId().toLowerCase().contains(keyword)
                        || provider.getName().toLowerCase().contains(keyword)
                        || provider.getSpecialty().toLowerCase().contains(keyword))
                .toList();

        model.addAttribute("pageTitle", "Healthcare Providers");
        model.addAttribute("providers", filtered);
        model.addAttribute("search", search);
        model.addAttribute("specialty", specialty);
        model.addAttribute("specialties", providers.stream()
                .map(HealthcareProvider::getSpecialty)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList());
        return "provider/list";
    }

    @GetMapping("/providers/new")
    public String newProviderForm(Model model) {
        model.addAttribute("pageTitle", "Register Healthcare Provider");
        model.addAttribute("provider", new HealthcareProvider());
        return "provider/form";
    }

    @PostMapping("/providers")
    public String createProvider(@ModelAttribute("provider") HealthcareProvider provider,
                                 RedirectAttributes redirectAttributes) {
        try {
            providerService.create(provider);
            redirectAttributes.addFlashAttribute("successMessage", "Healthcare provider registered successfully.");
            return "redirect:/providers";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/providers/new";
        }
    }

    @GetMapping("/providers/{id}")
    public String providerDetails(@PathVariable UUID id, Model model) {
        model.addAttribute("pageTitle", "Healthcare Provider Details");
        model.addAttribute("provider", providerService.findById(id));
        return "provider/details";
    }

    @GetMapping("/providers/{id}/edit")
    public String editProviderForm(@PathVariable UUID id, Model model) {
        model.addAttribute("pageTitle", "Edit Healthcare Provider");
        model.addAttribute("provider", providerService.findById(id));
        return "provider/form";
    }

    @PostMapping("/providers/{id}/update")
    public String updateProvider(@PathVariable UUID id,
                                 @ModelAttribute("provider") HealthcareProvider provider,
                                 RedirectAttributes redirectAttributes) {
        try {
            providerService.update(id, provider);
            redirectAttributes.addFlashAttribute("successMessage", "Healthcare provider updated successfully.");
            return "redirect:/providers/" + id;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/providers/" + id + "/edit";
        }
    }

    @PostMapping("/providers/{id}/delete")
    public String deleteProvider(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            providerService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Healthcare provider deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/providers";
    }

    @GetMapping("/claims")
    public String listClaims(@RequestParam(required = false) String search,
                             @RequestParam(required = false) String status,
                             Model model) {
        String keyword = search == null ? "" : search.trim().toLowerCase();
        String selectedStatus = status == null ? "" : status.trim();
        List<Claim> filtered = claimService.findAll().stream()
                .filter(claim -> selectedStatus.isEmpty() || selectedStatus.equalsIgnoreCase(claim.getStatus()))
                .filter(claim -> keyword.isEmpty()
                        || claim.getClaimId().toLowerCase().contains(keyword)
                        || claim.getMember().getMemberId().toLowerCase().contains(keyword)
                        || (claim.getMember().getFirstName() + " " + claim.getMember().getLastName())
                                .toLowerCase().contains(keyword)
                        || claim.getHealthcareProvider().getName().toLowerCase().contains(keyword))
                .toList();
        model.addAttribute("pageTitle", "Claims");
        model.addAttribute("claims", filtered);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("claimStatuses", List.of("PENDING", "APPROVED", "REJECTED"));
        return "claim/list";
    }

    @GetMapping("/claims/new")
    public String newClaimForm(Model model) {
        model.addAttribute("pageTitle", "Submit Claim");
        model.addAttribute("claim", new Claim());
        model.addAttribute("members", memberService.findAllMembers());
        model.addAttribute("providers", providerService.findAll());
        model.addAttribute("today", LocalDate.now());
        return "claim/form";
    }

    @PostMapping("/claims")
    public String createClaim(@ModelAttribute("claim") Claim claim, RedirectAttributes redirectAttributes) {
        try {
            claimService.create(claim);
            redirectAttributes.addFlashAttribute("successMessage", "Claim submitted successfully.");
            return "redirect:/claims";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/claims/new";
        }
    }

    @GetMapping("/claims/{id}")
    public String claimDetails(@PathVariable UUID id, Model model) {
        model.addAttribute("pageTitle", "Claim Details");
        model.addAttribute("claim", claimService.findById(id));
        return "claim/details";
    }

    @GetMapping("/claims/{id}/edit")
    public String editClaimForm(@PathVariable UUID id, Model model) {
        model.addAttribute("pageTitle", "Edit Claim");
        model.addAttribute("claim", claimService.findById(id));
        model.addAttribute("members", memberService.findAllMembers());
        model.addAttribute("providers", providerService.findAll());
        model.addAttribute("today", LocalDate.now());
        return "claim/form";
    }

    @PostMapping("/claims/{id}/update")
    public String updateClaim(@PathVariable UUID id,
                              @ModelAttribute("claim") Claim claim,
                              RedirectAttributes redirectAttributes) {
        try {
            claimService.update(id, claim);
            redirectAttributes.addFlashAttribute("successMessage", "Claim updated successfully.");
            return "redirect:/claims/" + id;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/claims/" + id + "/edit";
        }
    }

    @GetMapping("/claims/{id}/review")
    public String reviewClaimForm(@PathVariable UUID id, Model model) {
        model.addAttribute("pageTitle", "Review Claim");
        model.addAttribute("claim", claimService.findById(id));
        model.addAttribute("officers", officerService.findAll());
        return "claim/review";
    }

    @PostMapping("/claims/{id}/decision")
    public String reviewClaim(@PathVariable UUID id,
                              @RequestParam String status,
                              @RequestParam UUID officerId,
                              RedirectAttributes redirectAttributes) {
        try {
            claimService.review(id, status, officerId);
            redirectAttributes.addFlashAttribute("successMessage", "Claim decision saved.");
            return "redirect:/claims/" + id;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/claims/" + id + "/review";
        }
    }

    @PostMapping("/claims/{id}/delete")
    public String deleteClaim(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            claimService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Pending claim deleted.");
            return "redirect:/claims";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/claims/" + id;
        }
    }

    @GetMapping("/officers")
    public String listOfficers(@RequestParam(required = false) String search, Model model) {
        String keyword = search == null ? "" : search.trim().toLowerCase();
        List<InsuranceOfficer> officers = officerService.findAll().stream()
                .filter(officer -> keyword.isEmpty()
                        || officer.getOfficerId().toLowerCase().contains(keyword)
                        || officer.getFullName().toLowerCase().contains(keyword)
                        || officer.getRole().toLowerCase().contains(keyword))
                .toList();
        model.addAttribute("pageTitle", "Insurance Officers");
        model.addAttribute("officers", officers);
        model.addAttribute("search", search);
        return "officer/list";
    }

    @GetMapping("/officers/new")
    public String newOfficerForm(Model model) {
        model.addAttribute("pageTitle", "Register Insurance Officer");
        model.addAttribute("officer", new InsuranceOfficer());
        return "officer/form";
    }

    @PostMapping("/officers")
    public String createOfficer(@ModelAttribute("officer") InsuranceOfficer officer,
                                RedirectAttributes redirectAttributes) {
        try {
            officerService.create(officer);
            redirectAttributes.addFlashAttribute("successMessage", "Insurance Officer registered successfully.");
            return "redirect:/officers";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/officers/new";
        }
    }

    @GetMapping("/officers/{id}")
    public String officerDetails(@PathVariable UUID id, Model model) {
        model.addAttribute("pageTitle", "Insurance Officer Details");
        model.addAttribute("officer", officerService.findById(id));
        return "officer/details";
    }

    @GetMapping("/officers/{id}/edit")
    public String editOfficerForm(@PathVariable UUID id, Model model) {
        model.addAttribute("pageTitle", "Edit Insurance Officer");
        model.addAttribute("officer", officerService.findById(id));
        return "officer/form";
    }

    @PostMapping("/officers/{id}/update")
    public String updateOfficer(@PathVariable UUID id,
                                @ModelAttribute("officer") InsuranceOfficer officer,
                                RedirectAttributes redirectAttributes) {
        try {
            officerService.update(id, officer);
            redirectAttributes.addFlashAttribute("successMessage", "Insurance Officer updated successfully.");
            return "redirect:/officers/" + id;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/officers/" + id + "/edit";
        }
    }

    @PostMapping("/officers/{id}/delete")
    public String deleteOfficer(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            officerService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Insurance Officer deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/officers";
    }

    @GetMapping("/payments")
    public String listPayments(@RequestParam(required = false) String search, Model model) {
        String keyword = search == null ? "" : search.trim().toLowerCase();
        List<Payment> payments = paymentService.findAll().stream()
                .filter(payment -> keyword.isEmpty()
                        || payment.getPaymentId().toLowerCase().contains(keyword)
                        || payment.getPaymentMethod().toLowerCase().contains(keyword)
                        || payment.getMember().getMemberId().toLowerCase().contains(keyword)
                        || (payment.getMember().getFirstName() + " " + payment.getMember().getLastName())
                                .toLowerCase().contains(keyword))
                .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                .toList();
        model.addAttribute("pageTitle", "Payments");
        model.addAttribute("payments", payments);
        model.addAttribute("search", search);
        model.addAttribute("totalPaymentAmount", payments.stream().mapToDouble(Payment::getAmount).sum());
        return "payment/list";
    }

    @GetMapping("/payments/new")
    public String newPaymentForm(Model model) {
        model.addAttribute("pageTitle", "Record Payment");
        model.addAttribute("payment", new Payment());
        model.addAttribute("members", memberService.findAllMembers());
        model.addAttribute("today", LocalDate.now());
        return "payment/form";
    }

    @PostMapping("/payments")
    public String createPayment(@ModelAttribute("payment") Payment payment, RedirectAttributes redirectAttributes) {
        try {
            paymentService.create(payment);
            redirectAttributes.addFlashAttribute("successMessage", "Payment recorded successfully.");
            return "redirect:/payments";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/payments/new";
        }
    }

    @GetMapping("/payments/{id}")
    public String paymentDetails(@PathVariable UUID id, Model model) {
        model.addAttribute("pageTitle", "Payment Details");
        model.addAttribute("payment", paymentService.findById(id));
        return "payment/details";
    }

    @GetMapping("/payments/{id}/edit")
    public String editPaymentForm(@PathVariable UUID id, Model model) {
        model.addAttribute("pageTitle", "Edit Payment");
        model.addAttribute("payment", paymentService.findById(id));
        model.addAttribute("members", memberService.findAllMembers());
        model.addAttribute("today", LocalDate.now());
        return "payment/form";
    }

    @PostMapping("/payments/{id}/update")
    public String updatePayment(@PathVariable UUID id,
                                @ModelAttribute("payment") Payment payment,
                                RedirectAttributes redirectAttributes) {
        try {
            paymentService.update(id, payment);
            redirectAttributes.addFlashAttribute("successMessage", "Payment updated successfully.");
            return "redirect:/payments/" + id;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/payments/" + id + "/edit";
        }
    }

    @PostMapping("/payments/{id}/delete")
    public String deletePayment(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            paymentService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Payment deleted successfully.");
            return "redirect:/payments";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/payments/" + id;
        }
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("pageTitle", "System Users");
        model.addAttribute("users", userService.findAllUsers());
        return "user/list";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("pageTitle", "Create System User");
        model.addAttribute("user", new User());
        return "user/form";
    }

    @PostMapping("/users")
    public String createUser(@ModelAttribute("user") User user,
                             @RequestParam String passwordConfirmation,
                             RedirectAttributes redirectAttributes) {
        try {
            if (user.getPasswordHash() == null || !user.getPasswordHash().equals(passwordConfirmation)) {
                throw new ValidationException("Password confirmation does not match.");
            }
            userService.registerUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully.");
            return "redirect:/users";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            user.setPasswordHash(null);
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/users/new";
        }
    }

    @GetMapping("/users/{id}")
    public String userDetails(@PathVariable UUID id, Model model) {
        model.addAttribute("pageTitle", "User Details");
        model.addAttribute("user", userService.findById(id));
        return "user/details";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable UUID id, Model model) {
        model.addAttribute("pageTitle", "Edit System User");
        model.addAttribute("user", userService.findById(id));
        return "user/form";
    }

    @PostMapping("/users/{id}/update")
    public String updateUser(@PathVariable UUID id,
                             @ModelAttribute("user") User user,
                             @RequestParam(required = false) String passwordConfirmation,
                             RedirectAttributes redirectAttributes) {
        try {
            if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()
                    && !user.getPasswordHash().equals(passwordConfirmation)) {
                throw new ValidationException("Password confirmation does not match.");
            }
            userService.updateUser(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully.");
            return "redirect:/users/" + id;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            user.setPasswordHash(null);
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/users/" + id + "/edit";
        }
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/users";
    }

    private List<Member> filterMembers(List<Member> members, String search, String status) {
        String keyword = search == null ? "" : search.trim().toLowerCase();
        String selectedStatus = status == null ? "" : status.trim();

        return members.stream()
                .filter(member -> (selectedStatus.isEmpty() || selectedStatus.equalsIgnoreCase(member.getStatus())))
                .filter(member -> {
                    if (keyword.isEmpty()) {
                        return true;
                    }

                    String memberId = member.getMemberId() == null ? "" : member.getMemberId();
                    String nationalId = member.getNationalId() == null ? "" : member.getNationalId();
                    String fullName = ((member.getFirstName() == null ? "" : member.getFirstName()) + " " +
                            (member.getLastName() == null ? "" : member.getLastName())).trim();
                    String email = member.getEmail() == null ? "" : member.getEmail();

                    return memberId.toLowerCase().contains(keyword)
                            || nationalId.toLowerCase().contains(keyword)
                            || fullName.toLowerCase().contains(keyword)
                            || email.toLowerCase().contains(keyword);
                })
                .collect(Collectors.toList());
    }

    private List<InsurancePlan> filterPlans(List<InsurancePlan> plans, String search, String status) {
        String keyword = search == null ? "" : search.trim().toLowerCase();
        String selectedStatus = status == null ? "" : status.trim();

        return plans.stream()
                .filter(plan -> (selectedStatus.isEmpty() || selectedStatus.equalsIgnoreCase(plan.getStatus())))
                .filter(plan -> {
                    if (keyword.isEmpty()) {
                        return true;
                    }

                    String planId = plan.getInsurancePlanId() == null ? "" : plan.getInsurancePlanId();
                    String planName = plan.getInsurancePlanName() == null ? "" : plan.getInsurancePlanName();
                    return planId.toLowerCase().contains(keyword)
                            || planName.toLowerCase().contains(keyword);
                })
                .collect(Collectors.toList());
    }

    private List<Coverage> filterCoverages(List<Coverage> coverages, String search) {
        String keyword = search == null ? "" : search.trim().toLowerCase();

        return coverages.stream()
                .filter(coverage -> {
                    if (keyword.isEmpty()) {
                        return true;
                    }

                    String coverageId = coverage.getCoverageId() == null ? "" : coverage.getCoverageId();
                    String description = coverage.getDescription() == null ? "" : coverage.getDescription();
                    return coverageId.toLowerCase().contains(keyword)
                            || description.toLowerCase().contains(keyword);
                })
                .collect(Collectors.toList());
    }
}
