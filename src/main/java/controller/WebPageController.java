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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

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
import user.domain.SystemRole;
import user.dto.InsuranceApplicationForm;
import user.service.UserSessionManager;
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
    private final UserSessionManager userSessionManager;

    public WebPageController(MemberService memberService,
                            InsuranceService insuranceService,
                            CoverageService coverageService,
                            HealthcareProviderService providerService,
                            ClaimService claimService,
                            InsuranceOfficerService officerService,
                            PaymentService paymentService,
                            UserService userService,
                            UserSessionManager userSessionManager) {
        this.memberService = memberService;
        this.insuranceService = insuranceService;
        this.coverageService = coverageService;
        this.providerService = providerService;
        this.claimService = claimService;
        this.officerService = officerService;
        this.paymentService = paymentService;
        this.userService = userService;
        this.userSessionManager = userSessionManager;
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
    public String dashboardHome(Authentication authentication) {
        String target = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .findFirst()
                .map(authority -> switch (authority) {
                    case "ROLE_ADMINISTRATOR" -> "/admin/dashboard";
                    case "ROLE_INSURANCE_OFFICER" -> "/officer/dashboard";
                    case "ROLE_CUSTOMER" -> "/customer/dashboard";
                    default -> throw new IllegalStateException("Authenticated user has an unsupported role.");
                })
                .orElseThrow(() -> new IllegalStateException("Authenticated user has no role."));
        return "redirect:" + target;
    }

    @GetMapping({"/admin/dashboard", "/officer/dashboard", "/customer/dashboard"})
    public String dashboard(Model model, Authentication authentication) {
        boolean isCustomer = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CUSTOMER".equals(authority.getAuthority()));
        if (isCustomer) {
            User customer = userService.findByUsername(authentication.getName());
            model.addAttribute("pageTitle", customer.getMember() == null ? "Apply for Insurance" : "My Insurance");
            model.addAttribute("customerDashboard", true);
            model.addAttribute("customerName", customer.getMember() == null
                    ? customer.getUsername() : customer.getMember().getFirstName());
            model.addAttribute("customerMember", customer.getMember());
            if (customer.getMember() != null) {
                String nationalId = customer.getMember().getNationalId();
                model.addAttribute("customerClaims", claimService.findByMemberNationalId(nationalId).stream()
                        .sorted(Comparator.comparing(Claim::getClaimDate).reversed())
                        .limit(5)
                        .toList());
                model.addAttribute("customerPayments", paymentService.findByMemberNationalId(nationalId).stream()
                        .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                        .limit(5)
                        .toList());
                if (customer.getMember().getInsurancePlan() != null) {
                    model.addAttribute("customerCoverages",
                            coverageService.findByInsurancePlanId(customer.getMember().getInsurancePlan().getId()));
                } else {
                    model.addAttribute("customerCoverages", List.of());
                }
            }
            return "dashboard";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMINISTRATOR".equals(authority.getAuthority()));
        List<Member> members = memberService.findAllMembers();
        List<InsurancePlan> plans = insuranceService.findAllInsurancePlans();
        List<Coverage> coverages = coverageService.findAllCoverage();
        List<Claim> claims = claimService.findAll();
        List<Payment> payments = paymentService.findAll();
        List<HealthcareProvider> providers = isAdmin ? providerService.findAll() : List.of();
        List<InsuranceOfficer> officers = isAdmin ? officerService.findAll() : List.of();

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
        model.addAttribute("dashboardPath", isAdmin ? "/admin/dashboard" : "/officer/dashboard");
        model.addAttribute("customerDashboard", false);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("dashboardRole", isAdmin ? "Administrator workspace" : "Insurance Officer workspace");
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
        model.addAttribute("pendingApplications", members.stream()
                .filter(member -> "PENDING".equalsIgnoreCase(member.getStatus())).count());
        if (isAdmin) {
            model.addAttribute("totalUsers", userService.findAllUsers().size());
        }
        model.addAttribute("recentClaims", claims.stream().sorted(Comparator.comparing(Claim::getClaimDate).reversed()).limit(5).toList());
        model.addAttribute("recentPayments", payments.stream().sorted(Comparator.comparing(Payment::getPaymentDate).reversed()).limit(5).toList());
        model.addAttribute("recentMembers", members.stream()
                .sorted(Comparator.comparing(
                        Member::getRegistrationDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList());
        model.addAttribute("recentPlans", plans.stream().sorted(Comparator.comparing(InsurancePlan::getInsurancePlanName)).limit(5).toList());
        model.addAttribute("coverageSummary", coverageSummary);
        model.addAttribute("members", members);
        model.addAttribute("plans", plans);
        model.addAttribute("coverages", coverages);
        return "dashboard";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        List<Member> members = memberService.findAllMembers();
        List<InsurancePlan> plans = insuranceService.findAllInsurancePlans();
        List<Claim> claims = claimService.findAll();
        List<Payment> payments = paymentService.findAll();

        Map<String, Long> memberStatuses = members.stream()
                .collect(Collectors.groupingBy(
                        member -> member.getStatus().toUpperCase(),
                        java.util.TreeMap::new,
                        Collectors.counting()));
        Map<String, Long> claimStatuses = claims.stream()
                .collect(Collectors.groupingBy(
                        claim -> claim.getStatus().toUpperCase(),
                        java.util.TreeMap::new,
                        Collectors.counting()));
        Map<UUID, Long> membersByPlan = members.stream()
                .filter(member -> member.getInsurancePlan() != null)
                .collect(Collectors.groupingBy(member -> member.getInsurancePlan().getId(), Collectors.counting()));

        model.addAttribute("pageTitle", "Reports");
        model.addAttribute("reportMemberStatuses", memberStatuses);
        model.addAttribute("reportClaimStatuses", claimStatuses);
        model.addAttribute("reportPlans", plans.stream()
                .sorted(Comparator.comparing(InsurancePlan::getInsurancePlanName))
                .map(plan -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("plan", plan);
                    item.put("memberCount", membersByPlan.getOrDefault(plan.getId(), 0L));
                    return item;
                })
                .toList());
        model.addAttribute("reportMemberCount", members.size());
        model.addAttribute("reportClaimCount", claims.size());
        model.addAttribute("reportPaymentCount", payments.size());
        model.addAttribute("reportPaymentTotal", payments.stream().mapToDouble(Payment::getAmount).sum());
        model.addAttribute("reportLatestPayments", payments.stream()
                .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                .limit(10)
                .toList());
        return "reports";
    }

    @GetMapping("/insurance-application")
    public String insuranceApplication(Model model, Authentication authentication) {
        User customer = userService.findByUsername(authentication.getName());
        if (customer.getMember() != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("pageTitle", "Insurance Application");
        model.addAttribute("application", new InsuranceApplicationForm());
        model.addAttribute("accountEmail", customer.getEmail());
        model.addAttribute("insurancePlans", insuranceService.findAllInsurancePlans().stream()
                .filter(plan -> "ACTIVE".equalsIgnoreCase(plan.getStatus()))
                .toList());
        return "customer/application";
    }

    @PostMapping("/insurance-application")
    public String submitInsuranceApplication(
            @ModelAttribute("application") InsuranceApplicationForm application,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            userService.submitCustomerApplication(authentication.getName(), application);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your insurance application was submitted and is pending review.");
            return "redirect:/dashboard";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("application", application);
            return "redirect:/insurance-application";
        }
    }

    @GetMapping("/members")
    public String listMembers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        List<Member> members = memberService.findAllMembers();
        List<Member> filtered = filterMembers(members, search, status).stream()
                .sorted(Comparator.comparing(
                        Member::getRegistrationDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        model.addAttribute("pageTitle", "Members");
        model.addAttribute("members", paginateResults(filtered, page, "/members",
                paginationFilters("search", search, "status", status), model));
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("statuses", List.of("PENDING", "ACTIVE", "INACTIVE", "REJECTED", "EXPIRED"));
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
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        List<InsurancePlan> plans = insuranceService.findAllInsurancePlans();
        List<InsurancePlan> filtered = filterPlans(plans, search, status).stream()
                .sorted(Comparator.comparing(InsurancePlan::getInsurancePlanName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        model.addAttribute("pageTitle", "Insurance Plans");
        model.addAttribute("insurancePlans", paginateResults(filtered, page, "/insurance-plans",
                paginationFilters("search", search, "status", status), model));
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("statuses", List.of("ACTIVE", "INACTIVE"));
        model.addAttribute("activePlanCount", plans.stream()
                .filter(plan -> "ACTIVE".equalsIgnoreCase(plan.getStatus())).count());
        model.addAttribute("inactivePlanCount", plans.stream()
                .filter(plan -> "INACTIVE".equalsIgnoreCase(plan.getStatus())).count());
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
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        try {
            insuranceService.registerInsurance(insurancePlan);
            redirectAttributes.addFlashAttribute("successMessage", "Insurance plan created successfully.");
            return "redirect:/insurance-plans";
        } catch (RuntimeException ex) {
            modelForPlanForm(model, insurancePlan, "Create Insurance Plan", ex.getMessage());
            return "insurancePlan/form";
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
        model.addAttribute("memberCount", members.size());
        model.addAttribute("coverageCount", coverages.size());
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
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        try {
            if (insurancePlan.getMonthlyPremium() <= 0) {
                throw new ValidationException("Monthly premium must be greater than zero.");
            }
            if (insurancePlan.getCoverageLimit() <= 0) {
                throw new ValidationException("Coverage limit must be greater than zero.");
            }
            if (insurancePlan.getDurationMonths() <= 0) {
                throw new ValidationException("Duration must be greater than zero.");
            }
            insuranceService.updateInsurance(id, insurancePlan);
            redirectAttributes.addFlashAttribute("successMessage", "Insurance plan updated successfully.");
            return "redirect:/insurance-plans/" + id;
        } catch (RuntimeException ex) {
            insurancePlan.setId(id);
            modelForPlanForm(model, insurancePlan, "Edit Insurance Plan", ex.getMessage());
            return "insurancePlan/form";
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
            return "redirect:/insurance-plans/" + id;
        }
    }

    private void modelForPlanForm(Model model, InsurancePlan insurancePlan, String pageTitle, String errorMessage) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("insurancePlan", insurancePlan);
        model.addAttribute("errorMessage", errorMessage);
    }

    @GetMapping("/coverages")
    public String listCoverages(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        List<Coverage> coverages = coverageService.findAllCoverage();
        List<Coverage> filtered = filterCoverages(coverages, search).stream()
                .sorted(Comparator.comparing(Coverage::getCoverageId, String.CASE_INSENSITIVE_ORDER))
                .toList();

        model.addAttribute("pageTitle", "Coverage");
        model.addAttribute("coverages", paginateResults(filtered, page, "/coverages",
                paginationFilters("search", search), model));
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
                                @RequestParam(defaultValue = "0") int page,
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
                .sorted(Comparator.comparing(HealthcareProvider::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        model.addAttribute("pageTitle", "Healthcare Providers");
        model.addAttribute("providers", paginateResults(filtered, page, "/providers",
                paginationFilters("search", search, "specialty", specialty), model));
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
                             @RequestParam(defaultValue = "0") int page,
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
                .sorted(Comparator.comparing(Claim::getClaimDate, Comparator.reverseOrder()))
                .toList();
        model.addAttribute("pageTitle", "Claims");
        model.addAttribute("claims", paginateResults(filtered, page, "/claims",
                paginationFilters("search", search, "status", status), model));
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
    public String listOfficers(@RequestParam(required = false) String search,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        String keyword = search == null ? "" : search.trim().toLowerCase();
        List<InsuranceOfficer> officers = officerService.findAll().stream()
                .filter(officer -> keyword.isEmpty()
                        || officer.getOfficerId().toLowerCase().contains(keyword)
                        || officer.getFullName().toLowerCase().contains(keyword)
                        || officer.getRole().toLowerCase().contains(keyword))
                .sorted(Comparator.comparing(InsuranceOfficer::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        model.addAttribute("pageTitle", "Insurance Officers");
        model.addAttribute("officers", paginateResults(officers, page, "/officers",
                paginationFilters("search", search), model));
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
    public String listPayments(@RequestParam(required = false) String search,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
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
        model.addAttribute("payments", paginateResults(payments, page, "/payments",
                paginationFilters("search", search), model));
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
    public String listUsers(@RequestParam(required = false) String search,
                            @RequestParam(required = false) String role,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        String keyword = search == null ? "" : search.trim().toLowerCase();
        String selectedRole = role == null ? "" : role.trim();
        List<User> users = userService.findAllUsers().stream()
                .filter(user -> selectedRole.isEmpty()
                        || SystemRole.from(selectedRole) == SystemRole.from(user.getRole()))
                .filter(user -> keyword.isEmpty()
                        || user.getUsername().toLowerCase().contains(keyword)
                        || user.getEmail().toLowerCase().contains(keyword)
                        || user.getRole().toLowerCase().contains(keyword))
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .toList();
        model.addAttribute("pageTitle", "System Users");
        model.addAttribute("users", paginateResults(users, page, "/users",
                paginationFilters("search", search, "role", role), model));
        model.addAttribute("userRoleLabels", users.stream()
                .collect(Collectors.toMap(User::getId, user -> SystemRole.from(user.getRole()).label())));
        model.addAttribute("userCanDelete", users.stream()
                .collect(Collectors.toMap(User::getId,
                        user -> SystemRole.from(user.getRole()) != SystemRole.CUSTOMER)));
        model.addAttribute("search", search);
        model.addAttribute("role", role);
        model.addAttribute("userRoles", userRoleOptions());
        return "user/list";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("pageTitle", "Create System User");
        model.addAttribute("user", new User());
        model.addAttribute("userRoles", userRoleOptions());
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
        User user = userService.findById(id);
        model.addAttribute("user", user);
        model.addAttribute("roleLabel", SystemRole.from(user.getRole()).label());
        return "user/details";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable UUID id, Model model) {
        model.addAttribute("pageTitle", "Edit System User");
        User user = userService.findById(id);
        model.addAttribute("user", user);
        model.addAttribute("currentRole", SystemRole.from(user.getRole()).name());
        model.addAttribute("userRoles", userRoleOptions());
        return "user/form";
    }

    @PostMapping("/users/{id}/update")
    public String updateUser(@PathVariable UUID id,
                             @ModelAttribute("user") User user,
                             @RequestParam(required = false) String passwordConfirmation,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            User existing = userService.findById(id);
            if (existing.getUsername().equals(authentication.getName())) {
                throw new ValidationException("You cannot change your own account role or profile from user management.");
            }
            if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()
                    && !user.getPasswordHash().equals(passwordConfirmation)) {
                throw new ValidationException("Password confirmation does not match.");
            }
            User updated = userService.updateUser(id, user);
            if (SystemRole.from(existing.getRole()) != SystemRole.from(updated.getRole())) {
                userSessionManager.expireSessions(existing.getUsername());
            }
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully.");
            return "redirect:/users/" + id;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            user.setPasswordHash(null);
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/users/" + id + "/edit";
        }
    }

    private List<Map<String, String>> userRoleOptions() {
        return java.util.Arrays.stream(SystemRole.values())
                .map(role -> Map.of("value", role.name(), "label", role.label()))
                .toList();
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

    private <T> List<T> paginateResults(List<T> results,
                                       int requestedPage,
                                       String path,
                                       Map<String, String> filters,
                                       Model model) {
        int pageSize = 20;
        int totalPages = Math.max(1, (results.size() + pageSize - 1) / pageSize);
        int currentPage = Math.max(0, Math.min(requestedPage, totalPages - 1));
        int startIndex = currentPage * pageSize;
        int endIndex = Math.min(startIndex + pageSize, results.size());

        model.addAttribute("filteredCount", results.size());
        model.addAttribute("pageStart", results.isEmpty() ? 0 : startIndex + 1);
        model.addAttribute("pageEnd", endIndex);
        model.addAttribute("currentPage", currentPage + 1);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("hasPreviousPage", currentPage > 0);
        model.addAttribute("hasNextPage", currentPage + 1 < totalPages);
        if (currentPage > 0) {
            model.addAttribute("previousPageUrl", paginationUrl(path, currentPage - 1, filters));
        }
        if (currentPage + 1 < totalPages) {
            model.addAttribute("nextPageUrl", paginationUrl(path, currentPage + 1, filters));
        }
        return results.subList(startIndex, endIndex);
    }

    private String paginationUrl(String path, int page, Map<String, String> filters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path).queryParam("page", page);
        filters.forEach((name, value) -> {
            if (value != null && !value.isBlank()) {
                builder.queryParam(name, value);
            }
        });
        return builder.build().encode().toUriString();
    }

    private Map<String, String> paginationFilters(String... nameValues) {
        Map<String, String> filters = new LinkedHashMap<>();
        for (int index = 0; index + 1 < nameValues.length; index += 2) {
            filters.put(nameValues[index], nameValues[index + 1]);
        }
        return filters;
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
                    String description = plan.getDescription() == null ? "" : plan.getDescription();
                    return planId.toLowerCase().contains(keyword)
                            || planName.toLowerCase().contains(keyword)
                            || description.toLowerCase().contains(keyword);
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
