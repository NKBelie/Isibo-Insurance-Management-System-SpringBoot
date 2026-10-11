package user.controller;

import exception.DuplicateResourceException;
import exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import user.dto.CustomerRegistrationForm;
import user.service.UserService;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    @GetMapping("/register")
    public String registration(Model model) {
        if (!model.containsAttribute("registration")) {
            model.addAttribute("registration", new CustomerRegistrationForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("registration") CustomerRegistrationForm registration,
                           @RequestParam String passwordConfirmation,
                           RedirectAttributes redirectAttributes) {
        try {
            if (registration.getPassword() == null || !registration.getPassword().equals(passwordConfirmation)) {
                throw new ValidationException("Password confirmation does not match.");
            }
            userService.registerCustomerAccount(registration);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your account has been created. Sign in to submit an insurance application.");
            return "redirect:/login";
        } catch (ValidationException | DuplicateResourceException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            registration.setPassword(null);
            redirectAttributes.addFlashAttribute("registration", registration);
            return "redirect:/register";
        } catch (Exception ex) {
            log.error("Unexpected error while creating a customer account.", ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "We could not create your account right now. Please try again.");
            registration.setPassword(null);
            redirectAttributes.addFlashAttribute("registration", registration);
            return "redirect:/register";
        }
    }

}
